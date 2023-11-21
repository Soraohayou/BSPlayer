package com.blueshark.music.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_album.*
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Album
import com.blueshark.music.misc.asynctask.WrappedAsyncTaskLoader
import com.blueshark.music.misc.interfaces.LoaderIds
import com.blueshark.music.misc.interfaces.OnItemClickListener
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.ui.activity.ChildHolderActivity
import com.blueshark.music.ui.adapter.AlbumAdapter
import com.blueshark.music.ui.adapter.HeaderAdapter
import com.blueshark.music.util.Constants
import com.blueshark.music.util.MediaStoreUtil.getAllAlbum
import com.blueshark.music.util.SPUtil
import kotlinx.android.synthetic.main.fragment_song.root

/**
 * Created by Remix on 2015/12/20.
 */
/**
 * 专辑Fragment
 */
class AlbumFragment : LibraryFragment<Album, AlbumAdapter>() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pageName = TAG
  }

  override val layoutID: Int = R.layout.fragment_album

  override fun initAdapter() {
    adapter = AlbumAdapter(R.layout.item_album_recycle_grid, multiChoice, recyclerView)
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val album = adapter.dataList[position]
        if (userVisibleHint && !multiChoice.click(position, album)) {
          ChildHolderActivity.start(requireContext(), Constants.ALBUM, album.albumID.toString(), album.album)
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        if (userVisibleHint) {
          multiChoice.longClick(position, adapter.dataList.get(position))
        }
      }
    }
  }

  override fun initView() {
    val mode = SPUtil.getValue(requireContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MODE_FOR_ALBUM, HeaderAdapter.GRID_MODE)
    recyclerView.itemAnimator = DefaultItemAnimator()
    recyclerView.layoutManager = if (mode == HeaderAdapter.LIST_MODE) LinearLayoutManager(requireContext()) else GridLayoutManager(requireContext(), spanCount)
    recyclerView.adapter = adapter
    recyclerView.setHasFixedSize(true)
  }


  override fun loader(): Loader<List<Album>> {
    return AsyncAlbumLoader(requireContext())
  }

  override val loaderId: Int = LoaderIds.FRAGMENT_ALBUM

  private class AsyncAlbumLoader(context: Context?) : WrappedAsyncTaskLoader<List<Album>>(context) {
    override fun loadInBackground(): List<Album> {
      return getAllAlbum()
    }
  }

  companion object {
    val TAG = AlbumFragment::class.java.simpleName
  }
}