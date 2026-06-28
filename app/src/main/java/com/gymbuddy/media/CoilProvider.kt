package com.gymbuddy.media

import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache

/** App-wide Coil [ImageLoader] with a large, non-expiring disk cache so viewed media stays offline. */
object CoilProvider {
    fun build(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.filesDir.resolve("media_cache"))
                    .maxSizeBytes(300L * 1024 * 1024) // 300 MB — comfortably fits the full set
                    .build()
            }
            .respectCacheHeaders(false) // keep cached media indefinitely
            .crossfade(true)
            .build()
}
