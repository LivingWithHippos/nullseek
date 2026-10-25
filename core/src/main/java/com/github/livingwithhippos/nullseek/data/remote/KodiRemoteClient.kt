package com.github.livingwithhippos.nullseek.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class KodiRemoteClient(
    private val client: OkHttpClient,
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

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

    private fun buildJsonRpcRequest(
        address: String,
        bodyJson: String,
        username: String?,
        password: String?,
    ): Request {
        val url = "${normalizeBaseUrl(address)}/jsonrpc"
        val requestBody = bodyJson.toRequestBody(jsonMediaType)
        val builder =
            Request
                .Builder()
                .url(url)
                .post(requestBody)

        if (!username.isNullOrBlank() || !password.isNullOrBlank()) {
            builder.header("Authorization", Credentials.basic(username ?: "", password ?: ""))
        }

        return builder.build()
    }

    private fun escapeJson(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    suspend fun openUrl(
        address: String,
        mediaUrl: String,
        username: String? = null,
        password: String? = null,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val escapedUrl = escapeJson(mediaUrl)
                val body =
                    """{"jsonrpc":"2.0","id":616,"method":"Player.Open","params":{"item":{"file":"$escapedUrl"}}}"""
                val request = buildJsonRpcRequest(address, body, username, password)

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IOException("Kodi HTTP error ${response.code}: ${response.message}"))
                    }
                    val responseBody = response.body.string().orEmpty()
                    if (responseBody.contains("\"error\"")) {
                        return@withContext Result.failure(IOException("Kodi returned error: $responseBody"))
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
                val body = """{"jsonrpc":"2.0","id":616,"method":"JSONRPC.Ping"}"""
                val request = buildJsonRpcRequest(address, body, username, password)

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IOException("Kodi HTTP error ${response.code}: ${response.message}"))
                    }
                    val responseBody = response.body.string().orEmpty()
                    if (responseBody.contains("\"error\"")) {
                        return@withContext Result.failure(IOException("Kodi authentication/error: $responseBody"))
                    }
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
