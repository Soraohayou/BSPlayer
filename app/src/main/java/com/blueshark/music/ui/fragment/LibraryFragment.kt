package com.blueshark.music.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import com.blueshark.music.R
import com.blueshark.music.helper.MusicEventCallback
import com.blueshark.music.misc.isPortraitOrientation
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.ui.adapter.BaseAdapter
import com.blueshark.music.ui.fragment.base.BaseMusicFragment
import com.blueshark.music.ui.misc.MultipleChoice
import com.blueshark.music.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView
import com.blueshark.music.util.ColorUtil
import com.blueshark.music.util.Constants
import com.blueshark.music.util.DensityUtil
import java.util.*

/**
 * Created by Remix on 2016/12/23.
 */
abstract class LibraryFragment<Data, A : BaseAdapter<Data, *>> : BaseMusicFragment(), MusicEventCallback, LoaderManager.LoaderCallbacks<List<Data>> {
  lateinit var adapter: A
  lateinit var multiChoice: MultipleChoice<Data>
    private set

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val type = if (this is SongFragment) Constants.SONG else if (this is AlbumFragment) Constants.ALBUM else if (this is ArtistFragment) Constants.ARTIST else if (this is PlayListFragment) Constants.PLAYLIST else Constants.FOLDER
    multiChoice = MultipleChoice(requireActivity(), type)
    initAdapter()
    initView()

    //recyclerView的滚动条
    val accentColor = ThemeStore.accentColor
    val recyclerView: RecyclerView? = view.findViewById(R.id.recyclerView)
    if (recyclerView is FastScrollRecyclerView) {
      recyclerView.setBubbleColor(accentColor)
      recyclerView.setHandleColor(accentColor)
      recyclerView.setBubbleTextColor(ColorUtil.getColor(if (ColorUtil.isColorLight(accentColor)) R.color.light_text_color_primary else R.color.dark_text_color_primary))
    }

    multiChoice.adapter = adapter

    if (hasPermission) {
      loaderManager.initLoader(loaderId, null, this)
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(layoutID, container, false)
  }

  protected abstract val layoutID: Int
  protected abstract fun initAdapter()
  protected abstract fun initView()
  override fun onDestroy() {
    super.onDestroy()
    adapter.setDataList(Collections.emptyList())
  }

  override fun onMediaStoreChanged() {
    if (hasPermission) {
      loaderManager.restartLoader(loaderId, null, this)
    } else {
      adapter.setDataList(Collections.emptyList())
    }
  }

  override fun onPermissionChanged(has: Boolean) {
    if (has != hasPermission) {
      hasPermission = has
      onMediaStoreChanged()
    }
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Data>> {
    return loader()
  }

  override fun onLoadFinished(loader: Loader<List<Data>>, data: List<Data>?) {
    adapter.setDataList(data)
  }

  override fun onLoaderReset(loader: Loader<List<Data>>) {
    adapter.setDataList(Collections.emptyList())
  }

  protected val spanCount: Int
    get() {
      val portraitOrientation = requireContext().isPortraitOrientation()
      return if (portraitOrientation) {
        PORTRAIT_ORIENTATION_COUNT
      } else {
        val count = resources.displayMetrics.widthPixels / LANDSCAPE_ORIENTATION_ITEM_WIDTH
        if (count > PORTRAIT_ORIENTATION_MAX_ITEM_COUNT) PORTRAIT_ORIENTATION_MAX_ITEM_COUNT else count
      }
    }

  protected abstract fun loader(): Loader<List<Data>>
  protected abstract val loaderId: Int

  companion object {
    private const val PORTRAIT_ORIENTATION_COUNT = 2
    private val LANDSCAPE_ORIENTATION_ITEM_WIDTH = DensityUtil.dip2px(180f)
    private const val PORTRAIT_ORIENTATION_MAX_ITEM_COUNT = 6
  }
}