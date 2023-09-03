package com.senierr.media

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.imageLoader
import coil.memory.MemoryCache
import com.pateo.module.usb.UsbApplication
import com.senierr.base.util.LogUtil

/**
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class SessionApplication : Application(), ImageLoaderFactory {

    companion object {
        private const val TAG = "SessionApplication"
    }

    private val isDebug = true

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        LogUtil.isDebug = isDebug

        UsbApplication.onCreate(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        imageLoader.memoryCache?.trimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        imageLoader.memoryCache?.clear()
    }

    // Coil图片加载框架配置项
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizeBytes(10 * 1024 * 1024)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .allowRgb565(true)
            .crossfade(true)
//            .components {
//                // 视频解码器
//                add(VideoFrameDecoder.Factory())
//                // GIF解码器
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                    add(ImageDecoderDecoder.Factory())
//                } else {
//                    add(GifDecoder.Factory())
//                }
//            }
            .build()
    }
}