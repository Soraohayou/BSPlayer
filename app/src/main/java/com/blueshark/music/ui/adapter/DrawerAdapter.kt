package com.blueshark.music.ui.adapter

import android.view.View
import com.blueshark.music.R
import com.blueshark.music.databinding.ItemDrawerBinding
import com.blueshark.music.theme.GradientDrawableMaker
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore.accentColor
import com.blueshark.music.theme.ThemeStore.drawerDefaultColor
import com.blueshark.music.theme.ThemeStore.drawerEffectColor
import com.blueshark.music.ui.adapter.DrawerAdapter.DrawerHolder
import com.blueshark.music.ui.adapter.holder.BaseViewHolder

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/26 11:05
 */
class DrawerAdapter(layoutId: Int) : BaseAdapter<Int, DrawerHolder>(layoutId) {
  //当前选中项
  private var selectIndex = 0

  private val IMAGES = intArrayOf(R.drawable.drawer_icon_musicbox,
      R.drawable.ic_history_24dp,
      R.drawable.drawer_icon_recently_add,
      R.drawable.darwer_icon_set,
      R.drawable.drawer_icon_exit)
  private val TITLES: IntArray = intArrayOf(R.string.drawer_song,
      R.string.drawer_history,
//      R.string.drawer_recently_add,
      R.string.manual_scan,
      R.string.drawer_setting,
      R.string.exit)

  fun setSelectIndex(index: Int) {
    selectIndex = index
    notifyDataSetChanged()
  }

  override fun getItem(position: Int): Int {
    return TITLES[position]
  }

  override fun convert(holder: DrawerHolder, titleRes: Int?, position: Int) {
    if(titleRes == null){
      return
    }
    Theme.tintDrawable(holder.binding.itemImg, IMAGES[position], accentColor)
    holder.binding.itemText.setText(titleRes)
    holder.binding.itemText
        .setTextColor(Theme.resolveColor(holder.itemView.context, R.attr.text_color_primary))
    holder.binding.itemRoot
        .setOnClickListener { v: View? -> onItemClickListener?.onItemClick(v, position) }
    holder.binding.itemRoot.isSelected = selectIndex == position
    holder.binding.itemRoot.background = Theme.getPressAndSelectedStateListRippleDrawable(
        holder.itemView.context,
        GradientDrawableMaker()
            .color(drawerEffectColor).make(),
        GradientDrawableMaker()
            .color(drawerDefaultColor).make(),
        drawerEffectColor)
  }

  override fun getItemCount(): Int {
    return TITLES.size
  }

  class DrawerHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding: ItemDrawerBinding = ItemDrawerBinding.bind(itemView)

  }
}