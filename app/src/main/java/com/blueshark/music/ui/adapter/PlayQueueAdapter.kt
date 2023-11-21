package com.blueshark.music.ui.adapter

import android.view.View
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.databinding.ItemPlayqueueBinding
import com.blueshark.music.db.room.DatabaseRepository.Companion.getInstance
import com.blueshark.music.helper.MusicServiceRemote.getCurrentSong
import com.blueshark.music.util.RxUtil
import com.blueshark.music.service.Command
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.theme.ThemeStore.textColorPrimary
import com.blueshark.music.ui.adapter.PlayQueueAdapter.PlayQueueHolder
import com.blueshark.music.ui.adapter.holder.BaseViewHolder
import com.blueshark.music.util.Util

/**
 * Created by Remix on 2015/12/2.
 */
/**
 * 正在播放列表的适配器
 */
class PlayQueueAdapter(layoutId: Int) : BaseAdapter<Song, PlayQueueHolder>(layoutId) {
  private val accentColor: Int = ThemeStore.accentColor
  private val textColor: Int = textColorPrimary

  override fun convert(holder: PlayQueueHolder, song: Song?, position: Int) {
    if (song == null) {
      return
    }
    if (song == Song.EMPTY_SONG) {
      //歌曲已经失效
      holder.binding.playlistItemName.setText(R.string.song_lose_effect)
      holder.binding.playlistItemArtist.visibility = View.GONE
      return
    }
    //设置歌曲与艺术家
    holder.binding.playlistItemName.text = song.showName
    holder.binding.playlistItemArtist.text = song.artist
    holder.binding.playlistItemArtist.visibility = View.VISIBLE
    //高亮
    if (getCurrentSong().id == song.id) {
      holder.binding.playlistItemName.setTextColor(accentColor)
    } else {
//                holder.mSong.setTextColor(Color.parseColor(ThemeStore.isDay() ? "#323335" : "#ffffff"));
      holder.binding.playlistItemName.setTextColor(textColor)
    }
    //删除按钮
    holder.binding.playqueueDelete.setOnClickListener { v: View? ->
      getInstance()
          .deleteFromPlayQueue(listOf(song.id))
          .compose(RxUtil.applySingleScheduler())
          .subscribe { num: Int ->
            //删除的是当前播放的歌曲
            if (num > 0 && getCurrentSong().id == song.id) {
              Util.sendCMDLocalBroadcast(Command.NEXT)
            }
          }
    }
    holder.binding.itemRoot.setOnClickListener { v: View? -> onItemClickListener?.onItemClick(v, holder.adapterPosition) }
  }

  class PlayQueueHolder(view: View) : BaseViewHolder(view) {
    val binding: ItemPlayqueueBinding = ItemPlayqueueBinding.bind(view)
  }
}