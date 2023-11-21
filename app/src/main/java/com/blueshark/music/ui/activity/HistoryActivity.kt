package com.blueshark.music.ui.activity

import android.content.Context
import android.content.Loader
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.databinding.ActivityHistoryBinding
import com.blueshark.music.db.room.DatabaseRepository
import com.blueshark.music.helper.MusicServiceRemote.setPlayQueue
import com.blueshark.music.misc.asynctask.AppWrappedAsyncTaskLoader
import com.blueshark.music.misc.interfaces.LoaderIds.Companion.ACTIVITY_HISTORY
import com.blueshark.music.misc.interfaces.OnItemClickListener
import com.blueshark.music.service.Command
import com.blueshark.music.service.MusicService
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.ui.adapter.SongAdapter
import com.blueshark.music.util.MusicUtil
import kotlinx.android.synthetic.main.activity_history.root
import kotlinx.android.synthetic.main.activity_setting.root

class HistoryActivity : LibraryActivity<Song, SongAdapter>() {

  private lateinit var binding: ActivityHistoryBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityHistoryBinding.inflate(layoutInflater)
    setContentView(binding.root)

    adapter = SongAdapter(R.layout.item_song_recycle, choice, binding.recyclerview)
    choice.adapter = adapter
    adapter?.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        adapter?.let { adapter ->
          val song = adapter.dataList[position]
          if (!choice.click(position, song)) {
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
        choice.longClick(position, adapter?.dataList?.get(position))
      }
    }

    binding.recyclerview.layoutManager = LinearLayoutManager(this)
    binding.recyclerview.itemAnimator = DefaultItemAnimator()
    binding.recyclerview.adapter = adapter

    setUpToolbar(getString(R.string.drawer_history))

    binding.root.setBackgroundColor(ThemeStore.materialPrimaryColor)
  }

  override val loader: Loader<List<Song>>
    get() = AsyncHistorySongLoader(this)

  override val loaderId: Int = ACTIVITY_HISTORY

  private class AsyncHistorySongLoader(context: Context) : AppWrappedAsyncTaskLoader<List<Song>>(context) {
    override fun loadInBackground(): List<Song> {
      return DatabaseRepository.getInstance().getHistorySongs().blockingGet()
    }
  }
}