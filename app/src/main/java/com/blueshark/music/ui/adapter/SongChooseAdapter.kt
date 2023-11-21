package com.blueshark.music.ui.adapter

import android.view.View
import android.widget.CompoundButton
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.databinding.ItemSongChooseBinding
import com.blueshark.music.glide.GlideApp
import com.blueshark.music.misc.interfaces.OnSongChooseListener
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore.accentColor
import com.blueshark.music.theme.ThemeStore.isLightTheme
import com.blueshark.music.theme.TintHelper
import com.blueshark.music.ui.adapter.SongChooseAdapter.SongChooseHolder
import com.blueshark.music.ui.adapter.holder.BaseViewHolder
import java.util.*

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/21 10:02
 */
class SongChooseAdapter(layoutID: Int, private val checkListener: OnSongChooseListener) : BaseAdapter<Song, SongChooseHolder>(layoutID) {

  val checkedSong: ArrayList<Long> = ArrayList()

  override fun convert(holder: SongChooseHolder, song: Song?, position: Int) {
    if (song == null) {
      return
    }

    //歌曲名
    holder.binding.itemSong.text = song.showName
    //艺术家
    holder.binding.itemArtist.text = song.artist
    //封面
    GlideApp.with(holder.itemView)
        .load(song)
        .placeholder(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album))
        .error(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album))
        .centerCrop()
        .into(holder.binding.iv)

    //选中歌曲
    holder.binding.root.setOnClickListener { v: View? ->
      holder.binding.checkbox.isChecked = !holder.binding.checkbox.isChecked
      checkListener.OnSongChoose(checkedSong.size > 0)
    }
    val audioId = song.id
    TintHelper.setTint(holder.binding.checkbox, accentColor, !isLightTheme)
    holder.binding.checkbox.setOnCheckedChangeListener(null)
    holder.binding.checkbox.isChecked = true && checkedSong.contains(audioId)
    holder.binding.checkbox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
      if (isChecked && !checkedSong.contains(audioId)) {
        checkedSong.add(audioId)
      } else if (!isChecked) {
        checkedSong.remove(audioId)
      }
      checkListener.OnSongChoose(checkedSong.size > 0)
    }
  }

  class SongChooseHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding: ItemSongChooseBinding = ItemSongChooseBinding.bind(itemView)

  }
}