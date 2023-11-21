package com.blueshark.music.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.github.promeg.pinyinhelper.Pinyin
import com.blueshark.music.App
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.databinding.ItemSongRecycleBinding
import com.blueshark.music.glide.GlideApp
import com.blueshark.music.helper.MusicServiceRemote.getCurrentSong
import com.blueshark.music.helper.MusicServiceRemote.setPlayQueue
import com.blueshark.music.helper.SortOrder
import com.blueshark.music.misc.menu.SongPopupListener
import com.blueshark.music.service.Command
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.theme.ThemeStore.accentColor
import com.blueshark.music.theme.ThemeStore.highLightTextColor
import com.blueshark.music.theme.ThemeStore.libraryBtnColor
import com.blueshark.music.theme.ThemeStore.textColorPrimary
import com.blueshark.music.ui.activity.base.BaseActivity
import com.blueshark.music.ui.adapter.holder.BaseViewHolder
import com.blueshark.music.ui.misc.MultipleChoice
import com.blueshark.music.ui.widget.fastcroll_recyclerview.FastScroller
import com.blueshark.music.util.ColorUtil
import com.blueshark.music.util.Constants
import com.blueshark.music.util.MusicUtil
import com.blueshark.music.util.SPUtil
import com.blueshark.music.util.SPUtil.SETTING_KEY
import com.blueshark.music.util.ToastUtil
import java.util.*

/**
 * Created by taeja on 16-6-24.
 */
@SuppressLint("RestrictedApi")
open class ChildHolderAdapter(layoutId: Int, private val type: Int, private val arg: String, multiChoice: MultipleChoice<Song>, recyclerView: RecyclerView)
  : HeaderAdapter<Song, BaseViewHolder>(layoutId, multiChoice, recyclerView), FastScroller.SectionIndexer {

  private var lastPlaySong = getCurrentSong()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return if (viewType == TYPE_HEADER) SongAdapter.HeaderHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_header_1, parent, false)) else ChildHolderViewHolder(
        ItemSongRecycleBinding.inflate(LayoutInflater.from(parent.context), parent,
            false))
  }

  override fun convert(holder: BaseViewHolder, data: Song?, position: Int) {
    val context = holder.itemView.context
    if (position == 0) {
      val headerHolder = holder as SongAdapter.HeaderHolder
      //没有歌曲时隐藏
      if (dataList.size == 0) {
        headerHolder.binding.root.visibility = View.GONE
        return
      }
      headerHolder.binding.root.visibility = View.VISIBLE
      /*headerHolder.binding.playShuffleButton.setImageDrawable(
          Theme.tintVectorDrawable(context, R.drawable.play,
                  if (ColorUtil.isColorCloseToWhite(ThemeStore.materialPrimaryColor)) Color.WHITE else Color.BLACK)
      )*/
      headerHolder.binding.tvShuffleCount.text = context.getString(R.string.play_random, itemCount - 1)

      //显示当前排序方式
      headerHolder.binding.root.setOnClickListener { v: View? ->
        //设置正在播放列表
        if (dataList.isEmpty()) {
          ToastUtil.show(context, R.string.no_song)
          return@setOnClickListener
        }
        setPlayQueue(dataList, MusicUtil.makeCmdIntent(Command.NEXT, true))
      }
      return
    }

    if (data == null) {
      return
    }
    val holder = holder as ChildHolderViewHolder
    if (data.id < 0 || (data.title == context.getString(R.string.song_lose_effect))) {
      holder.binding.songTitle.setText(R.string.song_lose_effect)
      holder.binding.songButton.visibility = View.INVISIBLE
    } else {
      holder.binding.songButton.visibility = View.VISIBLE

      //封面
      GlideApp.with(holder.itemView)
          .load(data)
          .placeholder(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album))
          .error(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album))
          .centerCrop()
          .into(holder.binding.iv)

      //高亮
      if (getCurrentSong().id == data.id) {
        lastPlaySong = data
        holder.binding.songTitle.setTextColor(highLightTextColor)
        holder.binding.indicator.visibility = View.VISIBLE
      } else {
        holder.binding.songTitle.setTextColor(textColorPrimary)
        holder.binding.indicator.visibility = View.GONE
      }
      holder.binding.indicator.setBackgroundColor(highLightTextColor)

      //设置标题
      holder.binding.songTitle.text = data.showName

      //艺术家与专辑
      holder.binding.songOther.text = String.format("%s-%s", data.artist, data.album)
      //设置按钮着色
      val tintColor = libraryBtnColor
      Theme.tintDrawable(holder.binding.songButton, R.drawable.icon_player_more, tintColor)
      holder.binding.songButton.setOnClickListener { v: View? ->
        if (choice.isActive) {
          return@setOnClickListener
        }
        val popupMenu = PopupMenu(context, holder.binding.songButton, Gravity.END)
        popupMenu.menuInflater.inflate(R.menu.menu_song_item, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener(
            SongPopupListener((context as BaseActivity), data, type == Constants.PLAYLIST,
                arg))
        popupMenu.show()
      }
    }
    if (onItemClickListener != null) {
      holder.binding.root.setOnClickListener { v: View? ->
        if (holder.adapterPosition - 1 < 0) {
          ToastUtil.show(context, R.string.illegal_arg)
          return@setOnClickListener
        }
        if (data.id > 0) {
          onItemClickListener?.onItemClick(v, holder.adapterPosition - 1)
        }
      }
      holder.binding.root.setOnLongClickListener { v: View? ->
        if (holder.adapterPosition - 1 < 0) {
          ToastUtil.show(context, R.string.illegal_arg)
          return@setOnLongClickListener true
        }
        onItemClickListener?.onItemLongClick(v, holder.adapterPosition - 1)
        true
      }
    }
    holder.binding.root.isSelected = choice.isPositionCheck(position - 1)
  }

  override fun getSectionText(position: Int): String {
    if (position in 1..dataList.size) {
      val settingKey = when (type) {
        Constants.ALBUM -> SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER
        Constants.ARTIST -> SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER
        Constants.FOLDER -> SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER
        Constants.PLAYLIST -> SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER
        else -> null
      }
      if (settingKey != null) {
        val data = dataList[position - 1]
        val key = when (SPUtil.getValue(
            App.context,
            SETTING_KEY.NAME,
            settingKey,
            SortOrder.SONG_A_Z
        )) {
          SortOrder.SONG_A_Z, SortOrder.SONG_Z_A -> data.title
          SortOrder.ARTIST_A_Z, SortOrder.ARTIST_Z_A -> data.artist
          SortOrder.ALBUM_A_Z, SortOrder.ALBUM_Z_A -> data.album
          SortOrder.DISPLAY_NAME_A_Z, SortOrder.DISPLAY_NAME_Z_A -> data.displayName
          else -> ""
        }
        if (key.isNotEmpty())
          return Pinyin.toPinyin(key[0]).toUpperCase(Locale.getDefault()).substring(0, 1)
      }
    }
    return ""
  }

  fun updatePlayingSong() {
    val currentSong = getCurrentSong()
    if (currentSong == Song.EMPTY_SONG || currentSong.id == lastPlaySong.id) {
      return
    }
    if (dataList.contains(currentSong)) {
      // 找到新的高亮歌曲
      val index = dataList.indexOf(currentSong) + 1
      val lastIndex = dataList.indexOf(lastPlaySong) + 1
      var newHolder: ChildHolderViewHolder? = null
      if (recyclerView.findViewHolderForAdapterPosition(index) is ChildHolderViewHolder) {
        newHolder = recyclerView.findViewHolderForAdapterPosition(index) as ChildHolderViewHolder?
      }
      var oldHolder: ChildHolderViewHolder? = null
      if (recyclerView
              .findViewHolderForAdapterPosition(lastIndex) is ChildHolderViewHolder) {
        oldHolder = recyclerView
            .findViewHolderForAdapterPosition(lastIndex) as ChildHolderViewHolder?
      }
      if (newHolder != null) {
        newHolder.binding.songTitle.setTextColor(highLightTextColor)
        newHolder.binding.indicator.visibility = View.VISIBLE
      }
      if (oldHolder != null) {
        oldHolder.binding.songTitle.setTextColor(textColorPrimary)
        oldHolder.binding.indicator.visibility = View.GONE
      }
      lastPlaySong = currentSong
    }
  }

  internal class ChildHolderViewHolder(val binding: ItemSongRecycleBinding) : BaseViewHolder(binding.root)
}