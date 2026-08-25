package com.bt.bttune.ui.screens.settings


import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.bt.bttune.LocalPlayerAwareWindowInsets
import com.bt.bttune.R
import com.bt.bttune.constants.AudioNormalizationKey
import com.bt.bttune.constants.AudioQuality
import com.bt.bttune.constants.AudioQualityKey
import com.bt.bttune.constants.AutoLoadMoreKey
import com.bt.bttune.constants.AutoSkipNextOnErrorKey
import com.bt.bttune.constants.PermanentShuffleKey
import com.bt.bttune.constants.PersistentQueueKey
import com.bt.bttune.constants.SimilarContent
import com.bt.bttune.constants.SkipSilenceKey
import com.bt.bttune.constants.StopMusicOnTaskClearKey
import com.bt.bttune.ui.component.EnumListPreference
import com.bt.bttune.ui.component.IconButton
import com.bt.bttune.ui.component.PreferenceGroupTitle
import com.bt.bttune.ui.component.SettingsGeneralCategory
import com.bt.bttune.ui.component.SettingsPage
import com.bt.bttune.ui.component.SwitchPreference
import com.bt.bttune.ui.utils.backToMain
import com.bt.bttune.utils.rememberEnumPreference
import com.bt.bttune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (permanentShuffle, onPermanentShuffleChange) = rememberPreference(
        PermanentShuffleKey,
        defaultValue = false
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = true
    )
    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(
        AutoLoadMoreKey,
        defaultValue = true
    )
    val (similarContentEnabled, similarContentEnabledChange) = rememberPreference(
        key = SimilarContent,
        defaultValue = true
    )
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(
        AutoSkipNextOnErrorKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = false
    )

    SettingsPage(
        title = stringResource(R.string.player_and_audio),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.player),
            items = listOf(
                {EnumListPreference(
                    title = { Text(stringResource(R.string.audio_quality)) },
                    icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                    selectedValue = audioQuality,
                    onValueSelected = onAudioQualityChange,
                    valueText = {
                        when (it) {
                            AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                            AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                            AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                        }
                    }
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.permanent_shuffle)) },
                    description = stringResource(R.string.permanent_shuffle_desc),
                    icon = { Icon(painterResource(R.drawable.shuffle), null) },
                    checked = permanentShuffle,
                    onCheckedChange = onPermanentShuffleChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.skip_silence)) },
                    icon = { Icon(painterResource(R.drawable.fast_forward), null) },
                    checked = skipSilence,
                    onCheckedChange = onSkipSilenceChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.audio_normalization)) },
                    icon = { Icon(painterResource(R.drawable.volume_up), null) },
                    checked = audioNormalization,
                    onCheckedChange = onAudioNormalizationChange
                )},
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.queue),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.persistent_queue)) },
                    description = stringResource(R.string.persistent_queue_desc),
                    icon = { Icon(painterResource(R.drawable.queue_music), null) },
                    checked = persistentQueue,
                    onCheckedChange = onPersistentQueueChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.auto_load_more)) },
                    description = stringResource(R.string.auto_load_more_desc),
                    icon = { Icon(painterResource(R.drawable.playlist_add), null) },
                    checked = autoLoadMore,
                    onCheckedChange = onAutoLoadMoreChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_similar_content)) },
                    description = stringResource(R.string.similar_content_desc),
                    icon = { Icon(painterResource(R.drawable.similar), null) },
                    checked = similarContentEnabled,
                    onCheckedChange = similarContentEnabledChange,
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
                    description = stringResource(R.string.auto_skip_next_on_error_desc),
                    icon = { Icon(painterResource(R.drawable.skip_next), null) },
                    checked = autoSkipNextOnError,
                    onCheckedChange = onAutoSkipNextOnErrorChange
                )},
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                    icon = { Icon(painterResource(R.drawable.clear_all), null) },
                    checked = stopMusicOnTaskClear,
                    onCheckedChange = onStopMusicOnTaskClearChange
                )},
            )
        )
    }
}
