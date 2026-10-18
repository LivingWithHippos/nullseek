package com.github.livingwithhippos.nullseek.data.remote

import com.github.livingwithhippos.nullseek.data.model.IndexerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class IndexerRemoteClient(
    private val client: OkHttpClient,
) {
    private fun normalizeBaseUrl(address: String): String {
        val trimmed = address.trim()
        val withScheme =
            if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "http://$trimmed"
            }
        return withScheme.removeSuffix("/")
    }

    private fun buildRequest(
        type: IndexerType,
        address: String,
        apiToken: String,
    ): Request =
        when (type) {
            IndexerType.JACKETT -> {
                val url =
                    "${normalizeBaseUrl(address)}/api/v2.0/indexers/all/results/torznab/api".toHttpUrl().newBuilder()
                        .addQueryParameter("apikey", apiToken)
                        .build()
                Request.Builder().url(url).build()
            }

            IndexerType.PROWLARR ->
                Request.Builder()
                    .url("${normalizeBaseUrl(address)}/api/v1/system/status")
                    .header("X-Api-Key", apiToken)
                    .build()
        }

    suspend fun testConnection(
        type: IndexerType,
        address: String,
        apiToken: String,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                client.newCall(buildRequest(type, address, apiToken)).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("${type.displayName} HTTP error ${response.code}: ${response.message}"),
                        )
                    }
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
