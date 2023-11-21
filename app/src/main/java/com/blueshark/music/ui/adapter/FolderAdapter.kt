package com.blueshark.music.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Folder
import com.blueshark.music.databinding.ItemFolderRecycleBinding
import com.blueshark.music.misc.menu.LibraryListener
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore.isLightTheme
import com.blueshark.music.theme.ThemeStore.libraryBtnColor
import com.blueshark.music.ui.adapter.FolderAdapter.FolderHolder
import com.blueshark.music.ui.adapter.holder.BaseViewHolder
import com.blueshark.music.ui.misc.MultipleChoice
import com.blueshark.music.util.Constants

/**
 * Created by taeja on 16-6-23.
 */
class FolderAdapter(layoutId: Int, private val multiChoice: MultipleChoice<Folder>) : BaseAdapter<Folder, FolderHolder>(layoutId) {

  override fun onBindViewHolder(holder: FolderHolder, position: Int) {
    convert(holder, getItem(position) ?: return, position)
  }

  @SuppressLint("DefaultLocale", "RestrictedApi")
  override fun convert(holder: FolderHolder, folder: Folder?, position: Int) {
    if (folder == null) {
      return
    }
    val context = holder.itemView.context
    //设置文件夹名字 路径名 歌曲数量
    holder.binding.folderName.text = folder.name
    holder.binding.folderPath.text = folder.path
    holder.binding.folderNum.text = String.format("%d首", folder.count)
    //根据主题模式 设置图片
    holder.binding.folderImage.setImageDrawable(Theme
        .tintDrawable(context.resources.getDrawable(R.drawable.icon_folder),
            if (isLightTheme) Color.BLACK else Color.WHITE))
    val tintColor = libraryBtnColor
    Theme.tintDrawable(holder.binding.folderButton, R.drawable.icon_player_more, tintColor)
    holder.binding.folderButton.setOnClickListener { v: View? ->
      val popupMenu = PopupMenu(context, holder.binding.folderButton)
      popupMenu.menuInflater.inflate(R.menu.menu_folder_item, popupMenu.menu)
      popupMenu.setOnMenuItemClickListener(LibraryListener(context,
          folder,
          Constants.FOLDER,
          folder.path))
      popupMenu.gravity = Gravity.END
      popupMenu.show()
    }
    if (onItemClickListener != null) {
      holder.binding.root.setOnClickListener { v: View? -> onItemClickListener?.onItemClick(v, position) }
      holder.binding.root.setOnLongClickListener { v: View? ->
        onItemClickListener?.onItemLongClick(v, position)
        true
      }
    }
    holder.binding.root.isSelected = multiChoice.isPositionCheck(position)
  }

  class FolderHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding: ItemFolderRecycleBinding = ItemFolderRecycleBinding.bind(itemView)

  }
}