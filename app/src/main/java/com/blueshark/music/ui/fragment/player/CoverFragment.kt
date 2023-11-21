package com.blueshark.music.ui.fragment.player

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.facebook.rebound.Spring
import kotlinx.android.synthetic.main.fragment_cover_round.*
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.glide.GlideApp
import com.blueshark.music.theme.Theme
import com.blueshark.music.ui.fragment.base.BaseMusicFragment
import com.blueshark.music.util.DensityUtil
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import timber.log.Timber

abstract class CoverFragment : BaseMusicFragment() {
  open val layoutId = R.layout.fragment_cover_round
  val width by lazy {
    resources.displayMetrics.widthPixels
  }
  var inAnim: Spring? = null
  var outAnim: Spring? = null
  var coverCallback: CoverCallback? = null
  var reRunnable: Runnable? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    Timber.v("view: onCreateView")
    return inflater.inflate(layoutId, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    reRunnable?.run()
    reRunnable = null
  }

  override fun onDestroyView() {
    super.onDestroyView()
    outAnim?.destroy()
    inAnim?.destroy()
  }

  fun setImage(song: Song, playAnim: Boolean, updateBackground: Boolean) {
    if (!isAdded || (context as Activity).isFinishing) {
      reRunnable = Runnable {
        setImage(song, playAnim, updateBackground)
      }
      return
    }

    val options :RequestOptions = RequestOptions().transform( RoundedCorners(DensityUtil.dip2px(14f)));

    GlideApp.with(this)
        .asBitmap()
        .load(song)
        .centerCrop()
        .dontAnimate()
      .apply(options)
        .placeholder(Theme.resolveDrawable(requireContext(), R.attr.default_album))
        .error(Theme.resolveDrawable(requireContext(), R.attr.default_album))
        .addListener(object : RequestListener<Bitmap> {
          override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
            coverCallback?.onBitmap(null)
            return false
          }

          override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            coverCallback?.onBitmap(resource)
            return false
          }
        })
        .into(cover_image)

    if (playAnim) {
      playAnimation(song)
    }
  }

  fun clearAnim() {
    outAnim?.destroy()
    outAnim = null
    inAnim?.destroy()
    inAnim = null
  }

  abstract fun playAnimation(song: Song)

  interface CoverCallback {
    fun onBitmap(bitmap: Bitmap?)
  }

}