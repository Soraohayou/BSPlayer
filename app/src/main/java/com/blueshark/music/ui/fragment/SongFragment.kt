package com.blueshark.music.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_song.*
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.helper.MusicServiceRemote.getCurrentSong
import com.blueshark.music.helper.MusicServiceRemote.isPlaying
import com.blueshark.music.helper.MusicServiceRemote.setPlayQueue
import com.blueshark.music.misc.asynctask.WrappedAsyncTaskLoader
import com.blueshark.music.misc.interfaces.LoaderIds
import com.blueshark.music.misc.interfaces.OnItemClickListener
import com.blueshark.music.service.Command
import com.blueshark.music.service.MusicService
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.ui.activity.launchmain
import com.blueshark.music.ui.adapter.SongAdapter
import com.blueshark.music.util.ColorUtil
import com.blueshark.music.util.MediaStoreUtil.getAllSong
import com.blueshark.music.util.MusicUtil
import kotlinx.android.synthetic.main.activity_main.top_searchbar

/**
 * Created by Remix on 2015/11/30.
 */
/**
 * 全部歌曲的Fragment
 */
class SongFragment : LibraryFragment<Song, SongAdapter>() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pageName = TAG
  }

  override val layoutID: Int = R.layout.fragment_song

  override fun initAdapter() {
    adapter = SongAdapter(R.layout.item_song_recycle, multiChoice, location_recyclerView)
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val song = adapter.dataList[position]
        if (userVisibleHint && !multiChoice.click(position, song)) {
          if (isPlaying() && song == getCurrentSong()) {
            if (requireActivity() is launchmain) {
              (requireActivity() as launchmain).toPlayerActivity()
            }
          } else {
            //设置正在播放列表
            val songs = adapter.dataList
            if (songs.isEmpty()) {
              return
            }
            setPlayQueue(songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
                .putExtra(MusicService.EXTRA_POSITION, position))
          }
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

    //root.setBackgroundColor(ThemeStore.materialPrimaryColor);

    location_recyclerView.layoutManager = LinearLayoutManager(context)
    location_recyclerView.itemAnimator = DefaultItemAnimator()
    location_recyclerView.adapter = adapter
    location_recyclerView.setHasFixedSize(true)

    val accentColor = ThemeStore.accentColor
    location_recyclerView.setBubbleColor(accentColor)
    location_recyclerView.setHandleColor(accentColor)
    location_recyclerView.setBubbleTextColor(
      if (ColorUtil.isColorLight(accentColor)) {
        ColorUtil.getColor(R.color.light_text_color_primary)
      } else {
        ColorUtil.getColor(R.color.dark_text_color_primary)
      }
    )
  }

  override fun loader(): Loader<List<Song>> {
    return AsyncSongLoader(requireContext())
  }

  override val loaderId: Int = LoaderIds.FRAGMENT_SONG

//  override val adapter: SongAdapter? = adapter

  override fun onMetaChanged() {
    super.onMetaChanged()
    if(adapter.dataList.size>0){
      location_recyclerView.visibility = VISIBLE
      no_song.visibility = GONE
    }else{
      location_recyclerView.visibility = GONE
      no_song.visibility = VISIBLE
    }
    adapter.updatePlayingSong()
  }

  fun scrollToCurrent() {
    location_recyclerView.smoothScrollToCurrentSong(adapter.dataList)
  }

  private class AsyncSongLoader(context: Context?) : WrappedAsyncTaskLoader<List<Song>>(context) {
    override fun loadInBackground(): List<Song> {
      return getAllSong();
    }
  }

  companion object {
    val TAG = SongFragment::class.java.simpleName
  }
}