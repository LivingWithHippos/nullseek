package com.github.livingwithhippos.nullseek.ui.search

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Downloads and caches poster images (IMDb / Cinemeta) for the search screens. */
object PosterLoader {
    private val cache = LruCache<String, Bitmap>(60)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun cached(url: String): Bitmap? = synchronized(cache) { cache.get(url) }

    suspend fun load(url: String): Bitmap? {
        cached(url)?.let { return it }
        val loaded =
            withContext(Dispatchers.IO) {
                runCatching {
                    val request = Request.Builder().url(url).build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use null
                        val bytes = response.body.bytes()
                        if (bytes.isEmpty()) return@use null
                        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    }
                }.getOrNull()
            }
        if (loaded != null) synchronized(cache) { cache.put(url, loaded) }
        return loaded
    }
}
