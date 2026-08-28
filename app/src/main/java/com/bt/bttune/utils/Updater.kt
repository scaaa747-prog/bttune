package com.bt.bttune.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set

    suspend fun getLatestVersionName(): Result<String> =
        runCatching {
            val response =
                client.get("https://api.github.com/repos/scaaa747-prog/bttune/releases/latest")
                    .bodyAsText()
            val json = JSONObject(response)
            val versionName = json.optString("tag_name", json.optString("name", ""))
            lastCheckTime = System.currentTimeMillis()
            versionName
        }
}
