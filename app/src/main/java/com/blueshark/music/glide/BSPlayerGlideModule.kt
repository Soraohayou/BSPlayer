package com.blueshark.music.glide

import android.content.Context
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.executor.GlideExecutor
import com.bumptech.glide.module.AppGlideModule
import com.blueshark.music.bean.mp3.BSPlayerModel
import java.io.InputStream

@GlideModule
class BSPlayerGlideModule : AppGlideModule() {

  override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
    registry.append(Uri::class.java, InputStream::class.java, EmbeddedLoader.Factory())
    registry.append(BSPlayerModel::class.java, InputStream::class.java, BSPlayerUriLoader.Factory())
  }

  override fun applyOptions(context: Context, builder: GlideBuilder) {
    super.applyOptions(context, builder)
    builder.setDiskCacheExecutor(GlideExecutor.newSourceBuilder()
        .setName("custom-disk-cache")
        .setThreadCount(1)
        .setThreadTimeoutMillis(10000)
        .setUncaughtThrowableStrategy(GlideExecutor.UncaughtThrowableStrategy.LOG)
        .build())
  }
}