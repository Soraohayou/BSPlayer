package com.blueshark.music.ui.adapter

import android.view.View
import com.blueshark.music.databinding.ItemPlaylistAddtoBinding
import com.blueshark.music.db.room.model.PlayList
import com.blueshark.music.ui.adapter.AddToPlayListAdapter.PlayListAddToHolder
import com.blueshark.music.ui.adapter.holder.BaseViewHolder

/**
 * Created by taeja on 16-2-1.
 */
/**
 * 将歌曲添加到播放列表的适配器
 */
class AddToPlayListAdapter(layoutId: Int) : BaseAdapter<PlayList, PlayListAddToHolder>(layoutId) {

  override fun convert(holder: PlayListAddToHolder, data: PlayList?, position: Int) {
    if (data == null) {
      return
    }
    holder.binding.playlistAddtoText.text = data.name
    holder.binding.playlistAddtoText.tag = data.id
    holder.binding.itemRoot.setOnClickListener { v: View? ->
      onItemClickListener?.onItemClick(v, position)
    }
  }

  class PlayListAddToHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding: ItemPlaylistAddtoBinding = ItemPlaylistAddtoBinding.bind(itemView)
  }
}