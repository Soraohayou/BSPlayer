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
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.github.promeg.pinyinhelper.Pinyin
import com.blueshark.music.App
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Artist
import com.blueshark.music.databinding.ItemArtistRecycleGridBinding
import com.blueshark.music.databinding.ItemArtistRecycleListBinding
import com.blueshark.music.glide.GlideApp
import com.blueshark.music.glide.UriFetcher
import com.blueshark.music.helper.SortOrder
import com.blueshark.music.misc.menu.LibraryListener
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore.libraryBtnColor
import com.blueshark.music.ui.adapter.holder.BaseViewHolder
import com.blueshark.music.ui.adapter.holder.HeaderHolder
import com.blueshark.music.ui.misc.MultipleChoice
import com.blueshark.music.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView
import com.blueshark.music.ui.widget.fastcroll_recyclerview.FastScroller
import com.blueshark.music.util.*
import com.blueshark.music.util.SPUtil.SETTING_KEY
import java.util.*

/**
 * Created by Remix on 2015/12/22.
 */
/**
 * 艺术家界面的适配器
 */
class ArtistAdapter(layoutId: Int, multiChoice: MultipleChoice<Artist>, recyclerView: FastScrollRecyclerView)
  : BaseAdapter<Artist, BaseViewHolder>(layoutId), FastScroller.SectionIndexer {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return ArtistGridHolder(ItemArtistRecycleGridBinding.inflate(LayoutInflater.from(parent.context), parent, false))
  }

  @SuppressLint("RestrictedApi", "CheckResult")
  override fun convert(holder: BaseViewHolder, artist: Artist?, position: Int) {
    if (holder !is ArtistHolder || artist == null) {
      return
    }
    val context = holder.itemView.context
    //设置歌手名
    holder.tv1.text = artist.artist
    if (holder is ArtistListHolder) {
      if (artist.count > 0) {
        holder.tv2.text = context.getString(R.string.song_count_1, artist.count)
      } else {
        holder.tv2.text = App.context.getString(R.string.song_count_1, artist.count)
      }
    }else if(holder is ArtistGridHolder){
      if (artist.count > 0) {
        holder.tv2.text = context.getString(R.string.song_count_1, artist.count)
      } else {
        holder.tv2.text = App.context.getString(R.string.song_count_1, artist.count)
      }
    }
    //设置封面
    val options = RequestOptions()
        .placeholder(Theme.resolveDrawable(holder.itemView.context, R.attr.default_artist))
        .error(Theme.resolveDrawable(holder.itemView.context, R.attr.default_artist))

    GlideApp.with(holder.itemView)
        .load(artist)
        .apply(options)
        .signature(ObjectKey(UriFetcher.artistVersion))
        .into(holder.iv)

    holder.container.setOnClickListener { v: View? ->
      if (holder.adapterPosition - 1 < 0) {
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

    //popupmenu
    val tintColor = libraryBtnColor
    Theme.tintDrawable(holder.btn, R.drawable.icon_player_more, tintColor)
    holder.btn.setOnClickListener { v: View? ->
      val popupMenu = PopupMenu(context, holder.btn)
      popupMenu.menuInflater.inflate(R.menu.menu_artist_item, popupMenu.menu)
      popupMenu.setOnMenuItemClickListener(LibraryListener(context, artist,
          Constants.ARTIST,
          artist.artist))
      popupMenu.gravity = Gravity.END
      popupMenu.show()
    }

  }

  override fun getSectionText(position: Int): String {
    if (position in 1..dataList.size) {
      val data = dataList[position - 1]
      val key = when (SPUtil.getValue(
        App.context,
        SETTING_KEY.NAME,
        SETTING_KEY.ARTIST_SORT_ORDER,
        SortOrder.ARTIST_A_Z
      )) {
        SortOrder.ARTIST_A_Z, SortOrder.ARTIST_Z_A -> data.artist
        else -> ""
      }
      if (key.isNotEmpty())
        return Pinyin.toPinyin(key[0]).toUpperCase(Locale.getDefault()).substring(0, 1)
    }
    return ""
  }

  internal open class ArtistHolder(v: View) : BaseViewHolder(v) {
    lateinit var tv1: TextView
    lateinit var tv2: TextView
    lateinit var iv: ImageView
    lateinit var btn: ImageButton
    lateinit var container: ViewGroup
  }

  internal class ArtistListHolder(binding: ItemArtistRecycleListBinding) : ArtistHolder(binding.root) {
    init {
      tv1 = binding.itemText1
      tv2 = binding.itemText2
      iv = binding.iv
      btn = binding.itemButton
      container = binding.itemContainer
    }
  }

  internal class ArtistGridHolder(binding: ItemArtistRecycleGridBinding) : ArtistHolder(binding.root) {
    init {
      tv1 = binding.itemText1
      tv2 = binding.itemText2
      iv = binding.iv
      btn = binding.itemButton
      container = binding.itemContainer
    }
  }
}