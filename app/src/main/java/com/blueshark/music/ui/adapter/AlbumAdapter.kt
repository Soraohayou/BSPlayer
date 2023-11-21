package com.blueshark.music.ui.adapter

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.github.promeg.pinyinhelper.Pinyin
import com.blueshark.music.App
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Album
import com.blueshark.music.databinding.ItemAlbumRecycleGridBinding
import com.blueshark.music.databinding.ItemAlbumRecycleListBinding
import com.blueshark.music.glide.GlideApp
import com.blueshark.music.glide.UriFetcher
import com.blueshark.music.helper.SortOrder
import com.blueshark.music.misc.menu.LibraryListener
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore.getBackgroundColorMain
import com.blueshark.music.theme.ThemeStore.libraryBtnColor
import com.blueshark.music.ui.adapter.holder.BaseViewHolder
import com.blueshark.music.ui.adapter.holder.HeaderHolder
import com.blueshark.music.ui.misc.MultipleChoice
import com.blueshark.music.ui.widget.fastcroll_recyclerview.FastScroller
import com.blueshark.music.util.Constants
import com.blueshark.music.util.DensityUtil
import com.blueshark.music.util.SPUtil
import com.blueshark.music.util.SPUtil.SETTING_KEY
import com.blueshark.music.util.ToastUtil
import java.util.*


/**
 * Created by Remix on 2015/12/20.
 */
/**
 * 专辑界面的适配器
 */
class AlbumAdapter(layoutId: Int, multipleChoice: MultipleChoice<Album>,
                   recyclerView: RecyclerView) : BaseAdapter<Album, BaseViewHolder>(layoutId), FastScroller.SectionIndexer {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return AlbumGridHolder(ItemAlbumRecycleGridBinding.inflate(LayoutInflater.from(parent.context), parent, false))
  }


  @SuppressLint("RestrictedApi")
  override fun convert(holder: BaseViewHolder, album: Album?, position: Int) {
    if (holder !is AlbumHolder || album == null) {
      return
    }
    val context = holder.itemView.context
    holder.tv1.text = album.album

    //设置封面
    val options = RequestOptions()
        .placeholder(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album))
        .error(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album))

    GlideApp.with(holder.itemView)
        .load(album)
        .apply(options)
        .signature(ObjectKey(UriFetcher.albumVersion))
        .into(holder.iv)

    if (holder is AlbumListHolder) {
      holder.tv2.text = App.context.getString(R.string.song_count_2, album.artist, album.count)
    } else {
      holder.tv2.text = album.artist
    }
    holder.container.setOnClickListener { v: View? ->
      if (position - 1 < 0) {
        ToastUtil.show(context, R.string.illegal_arg)
        return@setOnClickListener
      }
      onItemClickListener?.onItemClick(holder.container, position - 1)
    }
    //多选菜单
    holder.container.setOnLongClickListener { v: View? ->
      if (position - 1 < 0) {
        ToastUtil.show(context, R.string.illegal_arg)
        return@setOnLongClickListener true
      }
      onItemClickListener?.onItemLongClick(holder.container, position - 1)
      true
    }

    //着色
    val tintColor = libraryBtnColor
    Theme.tintDrawable(holder.btn, R.drawable.icon_player_more, tintColor)
    holder.btn.setOnClickListener { v: View? ->
      val popupMenu = PopupMenu(context, holder.btn, Gravity.END)
      popupMenu.menuInflater.inflate(R.menu.menu_album_item, popupMenu.menu)
      popupMenu.setOnMenuItemClickListener(LibraryListener(
          context, album,
          Constants.ALBUM,
          album.album))
      popupMenu.show()
    }

  }

  override fun getSectionText(position: Int): String {
    if (position in 1..dataList.size) {
      val data = dataList[position - 1]
      val key = when (SPUtil.getValue(
          App.context,
          SETTING_KEY.NAME,
          SETTING_KEY.ALBUM_SORT_ORDER,
          SortOrder.ALBUM_A_Z
      )) {
        SortOrder.ALBUM_A_Z, SortOrder.ALBUM_Z_A -> data.album
        SortOrder.ARTIST_A_Z, SortOrder.ARTIST_Z_A -> data.artist
        else -> ""
      }
      if (key.isNotEmpty())
        return Pinyin.toPinyin(key[0]).toUpperCase(Locale.getDefault()).substring(0, 1)
    }
    return ""
  }

  internal open class AlbumHolder(v: View?) : BaseViewHolder(v) {
    lateinit var ivHalfCircle: ImageView
    lateinit var tv1: TextView
    lateinit var tv2: TextView
    lateinit var btn: ImageButton
    lateinit var iv: ImageView
    lateinit var container: ViewGroup
  }

  internal class AlbumGridHolder(binding: ItemAlbumRecycleGridBinding) : AlbumHolder(binding.root) {
    init {
      ivHalfCircle = binding.itemHalfCircle
      tv1 = binding.itemText1
      tv2 = binding.itemText2
      btn = binding.itemButton
      iv = binding.iv
      container = binding.itemContainer
    }
  }

  internal class AlbumListHolder(binding: ItemAlbumRecycleListBinding) : AlbumHolder(binding.root) {
    init {
      tv1 = binding.itemText1
      tv2 = binding.itemText2
      btn = binding.itemButton
      iv = binding.iv
      container = binding.itemContainer
    }
  }
}