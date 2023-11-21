package com.blueshark.music.glide

import android.net.Uri
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.blueshark.music.bean.mp3.*
import com.blueshark.music.db.room.model.PlayList
import java.io.InputStream

class BSPlayerUriLoader(private val concreteLoader: ModelLoader<Uri, InputStream>) : ModelLoader<BSPlayerModel, InputStream> {
  private val uriFetcher = UriFetcher

  override fun buildLoadData(model: BSPlayerModel, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
    val uri = uriFetcher.fetch(model)
    return concreteLoader.buildLoadData(uri, width, height, options)
  }

  override fun handles(model: BSPlayerModel): Boolean {
    return model is Song || model is Album || model is Artist || model is PlayList
  }

  class Factory : ModelLoaderFactory<BSPlayerModel, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<BSPlayerModel, InputStream> {
      return BSPlayerUriLoader(multiFactory.build(Uri::class.java, InputStream::class.java))
    }

    override fun teardown() {
    }
  }
}