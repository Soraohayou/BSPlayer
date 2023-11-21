package com.blueshark.music.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_artist.*
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Artist
import com.blueshark.music.misc.asynctask.WrappedAsyncTaskLoader
import com.blueshark.music.misc.interfaces.LoaderIds
import com.blueshark.music.misc.interfaces.OnItemClickListener
import com.blueshark.music.ui.activity.ChildHolderActivity
import com.blueshark.music.ui.adapter.ArtistAdapter
import com.blueshark.music.ui.adapter.HeaderAdapter
import com.blueshark.music.util.Constants
import com.blueshark.music.util.MediaStoreUtil.getAllArtist
import com.blueshark.music.util.SPUtil
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONObject

/**
 * Created by Remix on 2015/12/22.
 */
/**
 * 艺术家Fragment
 */
class ArtistFragment : LibraryFragment<Artist, ArtistAdapter>() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pageName = TAG
  }

  override val layoutID: Int = R.layout.fragment_artist

  override fun initAdapter() {

    adapter = ArtistAdapter(R.layout.item_artist_recycle_grid, multiChoice, recyclerView)
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {

        val artist = adapter.dataList[position]
        if (userVisibleHint && !multiChoice.click(position, artist)) {
          ChildHolderActivity.start(requireContext(), Constants.ARTIST, artist.artistID.toString(), artist.artist)
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
    val model = SPUtil.getValue(requireContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MODE_FOR_ARTIST, HeaderAdapter.GRID_MODE)
    recyclerView.layoutManager = if (model == HeaderAdapter.LIST_MODE) LinearLayoutManager(requireContext()) else GridLayoutManager(activity, spanCount)
    recyclerView.itemAnimator = DefaultItemAnimator()
    recyclerView.adapter = adapter
    recyclerView.setHasFixedSize(true)
  }

  override fun loader(): Loader<List<Artist>> {
    return AsyncArtistLoader(requireContext())
  }

  override val loaderId: Int = LoaderIds.FRAGMENT_ARTIST

  private class AsyncArtistLoader(context: Context?) : WrappedAsyncTaskLoader<List<Artist>>(context) {
    override fun loadInBackground(): List<Artist> {
      return getAllArtist()
    }
  }

  companion object {
    val TAG = ArtistFragment::class.java.simpleName
  }
}