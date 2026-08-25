package com.bt.bttune

import android.app.Application
import android.content.Context
import android.os.Build
import android.widget.Toast
import com.bt.bttune.ui.component.LocaleAwareApplication
import com.bt.bttune.utils.dataStore
import com.bt.bttune.utils.initializeCache
import android.widget.Toast.LENGTH_SHORT
import androidx.datastore.preferences.core.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.bt.bttune.innertube.YouTube
import com.bt.bttune.innertube.models.YouTubeLocale
import com.bt.bttune.kugou.KuGou
import com.bt.bttune.constants.AccountChannelHandleKey
import com.bt.bttune.constants.AccountEmailKey
import com.bt.bttune.constants.AccountNameKey
import com.bt.bttune.constants.ContentCountryKey
import com.bt.bttune.constants.ContentLanguageKey
import com.bt.bttune.constants.CountryCodeToName
import com.bt.bttune.constants.DataSyncIdKey
import com.bt.bttune.constants.InnerTubeCookieKey
import com.bt.bttune.constants.LanguageCodeToName
import com.bt.bttune.constants.MaxImageCacheSizeKey
import com.bt.bttune.constants.ProxyEnabledKey
import com.bt.bttune.constants.ProxyTypeKey
import com.bt.bttune.constants.ProxyUrlKey
import com.bt.bttune.constants.SYSTEM_DEFAULT
import com.bt.bttune.constants.UseLoginForBrowse
import com.bt.bttune.constants.VisitorDataKey
import com.bt.bttune.db.MusicDatabase
import com.bt.bttune.extensions.toEnum
import com.bt.bttune.extensions.toInetSocketAddress
import com.bt.bttune.ui.component.NamePreferenceManager
import com.bt.bttune.utils.BTTUNEStatsCloudSync
import com.bt.bttune.utils.dataStore
import com.bt.bttune.utils.get
import com.bt.bttune.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.Proxy
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class App : LocaleAwareApplication(), ImageLoaderFactory {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var namePreferenceManager: NamePreferenceManager

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this
        kotlinx.coroutines.runBlocking {
            runCatching { dataStore.initializeCache() }
        }
        Timber.plant(com.bt.bttune.utils.GlobalLogTree())

        try {
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    val sw = java.io.StringWriter()
                    val pw = java.io.PrintWriter(sw)
                    throwable.printStackTrace(pw)
                    val stack = sw.toString()

                    val intent = android.content.Intent(this@App, com.bt.bttune.ui.activities.DebugActivity::class.java).apply {
                        putExtra(com.bt.bttune.ui.activities.DebugActivity.EXTRA_STACK_TRACE, stack)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(intent)
                    try { Thread.sleep(100) } catch (_: InterruptedException) {}
                } catch (e: Exception) {
                    reportException(e)
                } finally {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(2)
                }
            }
        } catch (e: Exception) {
            reportException(e)
        }

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "") // replace zh-Hant-* to zh-*
        YouTube.locale = YouTubeLocale(
            gl = dataStore[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.country.takeIf { it in CountryCodeToName }
                ?: "US",
            hl = dataStore[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.language.takeIf { it in LanguageCodeToName }
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: "en"
        )
        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        if (dataStore[ProxyEnabledKey] == true) {
            try {
                YouTube.proxy = Proxy(
                    dataStore[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                    dataStore[ProxyUrlKey]!!.toInetSocketAddress()
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to parse proxy url.", LENGTH_SHORT).show()
                reportException(e)
            }
        }

        if (dataStore[UseLoginForBrowse] != false) {
            YouTube.useLoginForBrowse = true
        }

        GlobalScope.launch {
            BTTUNEStatsCloudSync.syncDaily(
                context = this@App,
                database = database,
                namePreferenceManager = namePreferenceManager,
            )?.onFailure(::reportException)
        }

        GlobalScope.launch(Dispatchers.IO) {
            runCatching {
                val email = namePreferenceManager.accountEmail.first().ifBlank {
                    dataStore[AccountEmailKey] ?: ""
                }
                val name = namePreferenceManager.userName.first().ifBlank { "BTTUNE User" }

                val automaticCloudBackupEnabled = getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
                    .getBoolean("enable_cloud_upload", true)

                if (automaticCloudBackupEnabled && email.isNotBlank()) {
                    Timber.i("App launch: Starting automatic cloud backup upload for $email")
                    val backupViewModel = com.bt.bttune.viewmodels.BackupRestoreViewModel(com.bt.bttune.db.InternalDatabase.newInstance(this@App))
                    val result = backupViewModel.backupToDrive(this@App, email, name)
                    if (result is com.bt.bttune.utils.DriveResult.Success) {
                        dataStore.edit { preferences ->
                            preferences[com.bt.bttune.constants.LastBackupTimestampKey] = System.currentTimeMillis()
                        }
                        Timber.i("App launch: Cloud backup upload completed successfully for $email")
                    } else {
                        Timber.e("App launch: Cloud backup upload failed for $email")
                    }

                    // Schedule periodic 24-hour backup worker
                    val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.bt.bttune.worker.DailyBackupWorker>(1, java.util.concurrent.TimeUnit.DAYS)
                        .setConstraints(
                            androidx.work.Constraints.Builder()
                                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                .build()
                        )
                        .build()

                    androidx.work.WorkManager.getInstance(this@App).enqueueUniquePeriodicWork(
                        "DailyBackupWorker",
                        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                    )
                }
            }.onFailure { e ->
                Timber.e(e, "App launch: Error during automatic cloud backup")
            }
        }

        GlobalScope.launch {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData
                        ?.takeIf { it != "null" } // Previously visitorData was sometimes saved as "null" due to a bug
                        ?: YouTube.visitorData().onFailure {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@App, "Failed to get visitorData.", LENGTH_SHORT)
                                    .show()
                            }
                            reportException(it)
                        }.getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        }
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[DataSyncIdKey] }
                .distinctUntilChanged()
                .collect { dataSyncId ->
                    YouTube.dataSyncId = dataSyncId?.let {
                        /*
                         * Workaround to avoid breaking older installations that have a dataSyncId
                         * that contains "||" in it.
                         * If the dataSyncId ends with "||" and contains only one id, then keep the
                         * id before the "||".
                         * If the dataSyncId contains "||" and is not at the end, then keep the
                         * second id.
                         * This is needed to keep using the same account as before.
                         */
                        it.takeIf { !it.contains("||") }
                            ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                            ?: it.substringAfter("||")
                    }
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    try {
                        YouTube.cookie = cookie
                    } catch (e: Exception) {
                        // we now allow user input now, here be the demons. This serves as a last ditch effort to avoid a crash loop
                        Timber.e("Could not parse cookie. Clearing existing cookie. %s", e.message)
                        forgetAccount(this@App)
                    }
                }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val cacheSize = dataStore[MaxImageCacheSizeKey]

        // will crash app if you set to 0 after cache starts being used
        if (cacheSize == 0) {
            return ImageLoader.Builder(this)
                .crossfade(true)
                .respectCacheHeaders(false)
                .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        }

        return ImageLoader.Builder(this)
            .crossfade(true)
            .respectCacheHeaders(false)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes((cacheSize ?: 512) * 1024 * 1024L)
                    .build()
            )
            .build()
    }

    companion object {
        lateinit var instance: App
            private set

        fun forgetAccount(context: Context) {
            runBlocking {
                context.dataStore.edit { settings ->
                    settings.remove(InnerTubeCookieKey)
                    settings.remove(VisitorDataKey)
                    settings.remove(DataSyncIdKey)
                    settings.remove(AccountNameKey)
                    settings.remove(AccountEmailKey)
                    settings.remove(AccountChannelHandleKey)
                }
            }
        }
    }
}
