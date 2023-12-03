package com.blueshark.music.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.ThemeUtils
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.signature.ObjectKey
import com.github.promeg.pinyinhelper.Pinyin
import com.blueshark.music.App
import com.blueshark.music.App.Companion.context
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.databinding.ItemSongRecycleBinding
import com.blueshark.music.databinding.LayoutHeader1Binding
import com.blueshark.music.db.room.DatabaseRepository
import com.blueshark.music.glide.GlideApp
import com.blueshark.music.glide.UriFetcher.albumVersion
import com.blueshark.music.helper.MusicServiceRemote
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
import com.blueshark.music.ui.dialog.AddtoPlayListDialog
import com.blueshark.music.ui.misc.MultipleChoice
import com.blueshark.music.ui.widget.fastcroll_recyclerview.FastScroller
import com.blueshark.music.util.ColorUtil
import com.blueshark.music.util.MusicUtil
import com.blueshark.music.util.RxUtil
import com.blueshark.music.util.SPUtil
import com.blueshark.music.util.ToastUtil
import com.blueshark.music.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 全部歌曲和最近添加页面所用adapter
 */
/**
 * Created by Remix on 2016/4/11.
 */
class SongAdapter(layoutId: Int, multiChoice: MultipleChoice<Song>, recyclerView: RecyclerView)
    : HeaderAdapter<Song, BaseViewHolder>(layoutId, multiChoice, recyclerView), FastScroller.SectionIndexer {

    private var lastPlaySong = getCurrentSong()

    val repository = DatabaseRepository.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return if (viewType == TYPE_HEADER) HeaderHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_header_1, parent, false))
        else SongViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_song_recycle, parent, false))
    }

    @SuppressLint("RestrictedApi")
    override fun convert(holder: BaseViewHolder, song: Song?, position: Int) {

        val isPrimaryColorCloseToWhite = ThemeStore.isMDColorCloseToWhite

        val context = holder.itemView.context
        if (position == 0) {
            val headerHolder = holder as HeaderHolder
            //没有歌曲时隐藏
            if (dataList.size == 0) {
                headerHolder.binding.root.visibility = View.GONE
                return
            } else {
                headerHolder.binding.root.visibility = View.VISIBLE
            }
            /*if (ColorUtil.isColorCloseToWhite(textColorPrimary)) {
                headerHolder.binding.playShuffleButton.setImageDrawable(Theme.tintVectorDrawable(context, R.drawable.play, textColorPrimary))
            } else {
                headerHolder.binding.playShuffleButton.setImageDrawable(Theme.tintVectorDrawable(context, R.drawable.play, textColorPrimary))
            }*/
            headerHolder.binding.tvShuffleCount.setTextColor(textColorPrimary)
            headerHolder.binding.tvShuffleCount.text = context.getString(R.string.play_random, itemCount - 1)
            headerHolder.binding.root.setOnClickListener { v: View? ->
                val intent = MusicUtil.makeCmdIntent(Command.NEXT, true)
                if (dataList.isEmpty()) {
                    ToastUtil.show(context, R.string.no_song)
                    return@setOnClickListener
                }
                setPlayQueue(dataList, intent)
            }
            return
        }
        if (holder !is SongViewHolder || song == null) {
            return
        }

        //封面
        GlideApp.with(holder.itemView).load(song).centerCrop().placeholder(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album)).error(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album)).signature(ObjectKey(albumVersion)).into(holder.binding.iv)

//        //是否为无损
//        if(!TextUtils.isEmpty(song.getDisplayName())){
//            String prefix = song.getDisplayName().substring(song.getDisplayName().lastIndexOf(".") + 1);
//            holder.mSQ.setVisibility(prefix.equals("flac") || prefix.equals("ape") || prefix.equals("wav")? View.VISIBLE : View.GONE);
//        }

        //高亮
        if (getCurrentSong().id == song.id) {
            lastPlaySong = song
            //holder.binding.songTitle.setTextColor(if (isPrimaryColorCloseToWhite) Color.BLACK else Color.WHITE)
            holder.binding.songTitle.setTextColor(textColorPrimary)
            holder.binding.indicator.setImageResource(R.drawable.indicator);
            holder.binding.indicator.setOnClickListener(null)
        } else {
            holder.binding.songTitle.setTextColor(textColorPrimary)
            //holder.binding.songTitle.setTextColor(if (isPrimaryColorCloseToWhite) Color.BLACK else Color.WHITE)
            holder.binding.indicator.setImageResource(R.drawable.add_to_playlist);
            holder.binding.indicator.setOnClickListener({
                DatabaseRepository.getInstance().insertToPlayQueue(listOf(getCurrentSong().id)).compose(RxUtil.applySingleScheduler()).subscribe { it -> ToastUtil.show(App.context, App.context.getString(R.string.add_song_playqueue_success, it)) }
            })
        }
        //holder.binding.indicator.setBackgroundColor(highLightTextColor)

        //标题
        holder.binding.songTitle.text = song.showName

        //艺术家与专辑
        holder.binding.songOther.text = String.format("%s-%s", song.artist, song.album)

        //设置按钮着色
        val tintColor = libraryBtnColor
        Theme.tintDrawable(holder.binding.songButton, R.drawable.icon_player_more, tintColor)
        holder.binding.songButton.setOnClickListener { v: View? ->
            if (choice.isActive) {
                return@setOnClickListener
            }
            val popupMenu = PopupMenu(context, holder.binding.songButton, Gravity.END)
            popupMenu.menuInflater.inflate(R.menu.menu_song_item, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener(SongPopupListener((context as BaseActivity), song, false, ""))
            popupMenu.show()
        }
        holder.binding.itemRoot.setOnClickListener { v: View? ->
            if (position - 1 < 0) {
                ToastUtil.show(context, R.string.illegal_arg)
                return@setOnClickListener
            }
            onItemClickListener?.onItemClick(v, position - 1)
        }
        holder.binding.itemRoot.setOnLongClickListener { v: View? ->
            if (position - 1 < 0) {
                ToastUtil.show(context, R.string.illegal_arg)
                return@setOnLongClickListener true
            }
            onItemClickListener?.onItemLongClick(v, position - 1)
            true
        }
        holder.binding.itemRoot.isSelected = choice.isPositionCheck(position - 1)
        val isLove =  MusicServiceRemote.service?.isSongLove(song.id)
        if (isLove == true) {
            holder.binding.favorite.setImageResource(R.drawable.favorite_checked)
        } else {
            holder.binding.favorite.setImageDrawable(Theme.tintVectorDrawable(context, R.drawable.favorite_unchecked, if (ColorUtil.isColorCloseToWhite(ThemeStore.materialPrimaryColor)) Color.BLACK else Color.WHITE))
        }
        //holder.binding.favorite.setImageResource(if (MusicServiceRemote.service?.isLove == true) R.drawable.favorite_checked else R.drawable.favorite_unchecked)
        holder.binding.favorite.setOnClickListener {
            holder.binding.favorite.setImageResource(if (MusicServiceRemote.service?.isLove == true) R.drawable.favorite_unchecked else R.drawable.favorite_checked)
            Util.sendCMDLocalBroadcast(Command.LOVE)
        }
    }

    override fun getSectionText(position: Int): String {
        if (position in 1..dataList.size) {
            val data = dataList[position - 1]
            val key = when (SPUtil.getValue(App.context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, SortOrder.SONG_A_Z)) {
                SortOrder.SONG_A_Z, SortOrder.SONG_Z_A -> data.title
                SortOrder.ARTIST_A_Z, SortOrder.ARTIST_Z_A -> data.artist
                SortOrder.ALBUM_A_Z, SortOrder.ALBUM_Z_A -> data.album
                SortOrder.DISPLAY_NAME_A_Z, SortOrder.DISPLAY_NAME_Z_A -> data.displayName
                else -> ""
            }
            if (key.isNotEmpty()) return Pinyin.toPinyin(key[0]).toUpperCase(Locale.getDefault()).substring(0, 1)
        }
        return ""
    }

    /**
     * 更新高亮歌曲
     */
    fun updatePlayingSong() {
        val currentSong = getCurrentSong()
        if (currentSong == Song.EMPTY_SONG || currentSong.id == lastPlaySong.id) {
            return
        }
        if (dataList.contains(currentSong)) {
            // 找到新的高亮歌曲
            val index = dataList.indexOf(currentSong) + 1
            val lastIndex = dataList.indexOf(lastPlaySong) + 1
            var newHolder: SongViewHolder? = null
            if (recyclerView.findViewHolderForAdapterPosition(index) is SongViewHolder) {
                newHolder = recyclerView.findViewHolderForAdapterPosition(index) as SongViewHolder?
            }
            var oldHolder: SongViewHolder? = null
            if (recyclerView.findViewHolderForAdapterPosition(lastIndex) is SongViewHolder) {
                oldHolder = recyclerView.findViewHolderForAdapterPosition(lastIndex) as SongViewHolder?
            }
            if (newHolder != null) {
                newHolder.binding.songTitle.setTextColor(textColorPrimary)
                newHolder.binding.indicator.setImageResource(R.drawable.indicator);
                newHolder.binding.indicator.setOnClickListener(null);
                //newHolder.binding.indicator.visibility = View.VISIBLE
            }
            if (oldHolder != null) {
                oldHolder.binding.songTitle.setTextColor(textColorPrimary)
                oldHolder.binding.indicator.setImageResource(R.drawable.add_to_playlist);
                oldHolder.binding.indicator.setOnClickListener({
                    DatabaseRepository.getInstance().insertToPlayQueue(listOf(currentSong.id)).compose(RxUtil.applySingleScheduler()).subscribe { it -> ToastUtil.show(context, context.getString(R.string.add_song_playqueue_success, it)) }
                })
                //oldHolder.binding.indicator.visibility = View.GONE
            }
            lastPlaySong = currentSong
        }
    }

    internal class SongViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val binding: ItemSongRecycleBinding = ItemSongRecycleBinding.bind(itemView)
    }

    internal class HeaderHolder(itemView: View) : BaseViewHolder(itemView) {
        val binding: LayoutHeader1Binding = LayoutHeader1Binding.bind(itemView)

    }

}