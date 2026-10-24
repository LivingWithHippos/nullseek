package com.github.livingwithhippos.nullseek.data.remote

import com.github.livingwithhippos.nullseek.data.model.IndexerType
import com.github.livingwithhippos.nullseek.data.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

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
                    "${normalizeBaseUrl(address)}/api/v2.0/indexers/all/results/torznab/api"
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("apikey", apiToken)
                        .build()
                Request.Builder().url(url).build()
            }

            IndexerType.PROWLARR -> {
                Request
                    .Builder()
                    .url("${normalizeBaseUrl(address)}/api/v1/system/status")
                    .header("X-Api-Key", apiToken)
                    .build()
            }
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

    private val searchClient: OkHttpClient by lazy {
        client
            .newBuilder()
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    private fun buildSearchRequest(
        type: IndexerType,
        address: String,
        apiToken: String,
        query: String,
    ): Request =
        when (type) {
            IndexerType.JACKETT -> {
                Request
                    .Builder()
                    .url(
                        "${normalizeBaseUrl(address)}/api/v2.0/indexers/all/results"
                            .toHttpUrl()
                            .newBuilder()
                            .addQueryParameter("apikey", apiToken)
                            .addQueryParameter("Query", query)
                            .build(),
                    ).build()
            }

            IndexerType.PROWLARR -> {
                Request
                    .Builder()
                    .url(
                        "${normalizeBaseUrl(address)}/api/v1/search"
                            .toHttpUrl()
                            .newBuilder()
                            .addQueryParameter("query", query)
                            .addQueryParameter("type", "search")
                            .build(),
                    ).header("X-Api-Key", apiToken)
                    .build()
            }
        }

    private suspend fun Call.await(): Response =
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { cancel() }
            enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        if (cont.isActive) cont.resumeWith(Result.failure(e))
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        cont.resumeWith(Result.success(response))
                    }
                },
            )
        }

    /** Cancelling the calling coroutine cancels the underlying HTTP call. */
    suspend fun search(
        type: IndexerType,
        address: String,
        apiToken: String,
        query: String,
        indexerName: String = type.displayName,
    ): Result<List<SearchResult>> =
        try {
            val request = buildSearchRequest(type, address, apiToken, query)
            searchClient.newCall(request).await().use { response ->
                if (!response.isSuccessful) {
                    return@use Result.failure(
                        IOException("${type.displayName} HTTP error ${response.code}: ${response.message}"),
                    )
                }
                val body = withContext(Dispatchers.IO) { response.body.string() }
                Result.success(
                    when (type) {
                        IndexerType.JACKETT -> parseJackettResults(body, indexerName)
                        IndexerType.PROWLARR -> parseProwlarrResults(body, indexerName)
                    },
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
