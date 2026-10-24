package com.github.livingwithhippos.nullseek.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder

class VlcRemoteClient(
    private val client: OkHttpClient,
) {
    private fun normalizeBaseUrl(address: String): String {
        val trimmed = address.trim()
        val withScheme =
            if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith(
                    "https://",
                    ignoreCase = true,
                )
            ) {
                trimmed
            } else {
                "http://$trimmed"
            }
        return withScheme.removeSuffix("/")
    }

    private fun buildRequest(
        url: String,
        username: String?,
        password: String?,
    ): Request {
        val builder = Request.Builder().url(url)
        val user = username ?: ""
        val pass = password ?: ""
        if (user.isNotEmpty() || pass.isNotEmpty()) {
            builder.header("Authorization", Credentials.basic(user, pass))
        }
        return builder.build()
    }

    suspend fun openUrl(
        address: String,
        mediaUrl: String,
        username: String? = null,
        password: String? = null,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val encodedUrl = URLEncoder.encode(mediaUrl, "UTF-8")
                val requestUrl =
                    "${normalizeBaseUrl(address)}/requests/status.xml?command=in_play&input=$encodedUrl"
                val request = buildRequest(requestUrl, username, password)

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("VLC HTTP error ${response.code}: ${response.message}"),
                        )
                    }
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun testConnection(
        address: String,
        username: String? = null,
        password: String? = null,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val requestUrl = "${normalizeBaseUrl(address)}/requests/status.xml"
                val request = buildRequest(requestUrl, username, password)

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("VLC HTTP error ${response.code}: ${response.message}"),
                        )
                    }
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
