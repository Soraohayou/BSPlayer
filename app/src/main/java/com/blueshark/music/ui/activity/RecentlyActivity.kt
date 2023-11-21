package com.blueshark.music.ui.activity

import android.content.Context
import android.content.Loader
import android.os.Bundle
import android.os.Message
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.databinding.ActivityRecentlyBinding
import com.blueshark.music.helper.MusicServiceRemote.setPlayQueue
import com.blueshark.music.misc.asynctask.AppWrappedAsyncTaskLoader
import com.blueshark.music.misc.handler.MsgHandler
import com.blueshark.music.misc.handler.OnHandleMessage
import com.blueshark.music.misc.interfaces.LoaderIds
import com.blueshark.music.misc.interfaces.OnItemClickListener
import com.blueshark.music.service.Command
import com.blueshark.music.service.MusicService
import com.blueshark.music.ui.adapter.SongAdapter
import com.blueshark.music.util.MediaStoreUtil.getLastAddedSong
import com.blueshark.music.util.MusicUtil

/**
 * Created by taeja on 16-3-4.
 */
/**
 * 最近添加歌曲的界面 目前为最近7天添加
 */
class RecentlyActivity : LibraryActivity<Song, SongAdapter>() {
  private val binding: ActivityRecentlyBinding by lazy {
    ActivityRecentlyBinding.inflate(layoutInflater)
  }
  private val handler by lazy {
    MsgHandler(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    adapter = SongAdapter(R.layout.item_song_recycle, choice, binding.recyclerview)
    choice.adapter = adapter
    adapter?.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val song = adapter?.getDataList()?.get(position) ?: return

        if (!choice.click(position, song)) {
          val songs = adapter?.getDataList() ?: return
          if (songs.isEmpty()) {
            return
          }
          setPlayQueue(songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
              .putExtra(MusicService.EXTRA_POSITION, position))
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        choice.longClick(position, adapter!!.getDataList()[position])
      }
    }
    binding.recyclerview.layoutManager = LinearLayoutManager(this)
    binding.recyclerview.itemAnimator = DefaultItemAnimator()
    binding.recyclerview.adapter = adapter
    setUpToolbar(getString(R.string.recently))
  }

  override fun onLoadFinished(loader: Loader<List<Song>>, data: List<Song>?) {
    super.onLoadFinished(loader, data)
    if (data != null) {
      binding.recyclerview.visibility = if (data.isNotEmpty()) View.VISIBLE else View.GONE
      binding.recentlyPlaceholder.visibility = if (data.isNotEmpty()) View.GONE else View.VISIBLE
    } else {
      binding.recyclerview.visibility = View.GONE
      binding.recentlyPlaceholder.visibility = View.VISIBLE
    }
  }

  @OnHandleMessage
  fun handleMessage(msg: Message) {
    when (msg.what) {
      MSG_RESET_MULTI, MSG_UPDATE_ADAPTER -> adapter?.notifyDataSetChanged()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    handler.remove()
  }

  override val loader: Loader<List<Song>>
    get() = AsyncRecentlySongLoader(this)

  override val loaderId = LoaderIds.ACTIVITY_RECENTLY

  private class AsyncRecentlySongLoader(context: Context) : AppWrappedAsyncTaskLoader<List<Song>>(context) {
    override fun loadInBackground(): List<Song> {
      return getLastAddedSong()
    }
  }

  companion object {
    val TAG = RecentlyActivity::class.java.simpleName
  }
}