package com.github.livingwithhippos.nullseek.ui.search

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private object PosterBitmapCache {
    private val cache = object : LruCache<String, ImageBitmap>(60) {}

    val httpClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun get(url: String): ImageBitmap? = synchronized(cache) { cache.get(url) }

    fun put(url: String, bitmap: ImageBitmap) = synchronized(cache) { cache.put(url, bitmap) }
}

@Composable
fun PosterImage(
    posterUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    val cleanUrl = posterUrl?.trim()?.takeIf { it.isNotEmpty() }
    val bitmap by produceState<ImageBitmap?>(initialValue = cleanUrl?.let { PosterBitmapCache.get(it) }, key1 = cleanUrl) {
        if (cleanUrl == null) {
            value = null
            return@produceState
        }
        PosterBitmapCache.get(cleanUrl)?.let { cached ->
            value = cached
            return@produceState
        }
        val loaded =
            withContext(Dispatchers.IO) {
                runCatching {
                    val request = Request.Builder().url(cleanUrl).build()
                    PosterBitmapCache.httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use null
                        val bytes = response.body.bytes()
                        if (bytes.isEmpty()) return@use null
                        val options =
                            BitmapFactory.Options().apply {
                                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                            }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        if (loaded != null) {
            PosterBitmapCache.put(cleanUrl, loaded)
        }
        value = loaded
    }

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(
                text = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
