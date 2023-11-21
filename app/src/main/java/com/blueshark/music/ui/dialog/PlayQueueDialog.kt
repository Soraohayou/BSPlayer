package com.blueshark.music.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.databinding.DialogPlayqueueBinding
import com.blueshark.music.db.room.DatabaseRepository
import com.blueshark.music.db.room.model.PlayQueue
import com.blueshark.music.helper.MusicServiceRemote
import com.blueshark.music.misc.asynctask.WrappedAsyncTaskLoader
import com.blueshark.music.misc.interfaces.OnItemClickListener
import com.blueshark.music.service.Command
import com.blueshark.music.service.MusicService.Companion.EXTRA_POSITION
import com.blueshark.music.theme.Theme
import com.blueshark.music.ui.adapter.PlayQueueAdapter
import com.blueshark.music.ui.dialog.base.BaseMusicDialog
import com.blueshark.music.util.DensityUtil
import com.blueshark.music.util.MusicUtil.makeCmdIntent
import com.blueshark.music.util.Util.sendLocalBroadcast
import timber.log.Timber

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 正在播放列表Dialog
 */

class PlayQueueDialog : BaseMusicDialog(), LoaderManager.LoaderCallbacks<List<Song>> {
  private var _binding: DialogPlayqueueBinding? = null
  private val binding get() = _binding!!

  val adapter: PlayQueueAdapter by lazy {
    PlayQueueAdapter(R.layout.item_playqueue)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = Theme.getBaseDialog(activity)
        .customView(R.layout.dialog_playqueue, false)
        .build()
    _binding = DialogPlayqueueBinding.bind(dialog.customView!!)

    binding.playqueueRecyclerview.adapter = adapter
    binding.playqueueRecyclerview.layoutManager = LinearLayoutManager(context)
    binding.playqueueRecyclerview.itemAnimator = DefaultItemAnimator()

    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        sendLocalBroadcast(makeCmdIntent(Command.PLAYSELECTEDSONG)
            .putExtra(EXTRA_POSITION, position))
      }

      override fun onItemLongClick(view: View, position: Int) {}
    }

    //改变播放列表高度，并置于底部
    val window = dialog.window
    window!!.setWindowAnimations(R.style.DialogAnimBottom)
    val display = requireActivity().windowManager.defaultDisplay
    val metrics = DisplayMetrics()
    display.getMetrics(metrics)
    val lp = window.attributes
    lp.height = DensityUtil.dip2px(context, 354f)
    lp.width = metrics.widthPixels
    window.attributes = lp
    window.setGravity(Gravity.BOTTOM)

    //初始化LoaderManager
    loaderManager.initLoader(LOADER_ID++, null, this)

    onViewCreated(dialog.customView!!, savedInstanceState)
    return dialog

  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Song>> {
    return AsyncPlayQueueSongLoader(requireContext())
  }

  override fun onLoadFinished(loader: Loader<List<Song>>, data: List<Song>?) {
    if (data == null) {
      return
    }
    binding.tvTitle.text = getString(R.string.play_queue, data.size)
    adapter.setDataList(data)
    val currentId = MusicServiceRemote.getCurrentSong().id
    if (currentId < 0) {
      return
    }
    binding.playqueueRecyclerview.smoothScrollToCurrentSong(data)
  }

  override fun onLoaderReset(loader: Loader<List<Song>>) {
    adapter.setDataList(null)
  }


  override fun onMetaChanged() {
    super.onMetaChanged()
    adapter.notifyDataSetChanged()
  }

  override fun onPlayListChanged(name: String) {
    super.onPlayListChanged(name)
    if (name == PlayQueue.TABLE_NAME) {
      if (hasPermission) {
        loaderManager.restartLoader(LOADER_ID, null, this)
      } else {
        adapter.setDataList(null)
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }


  class AsyncPlayQueueSongLoader constructor(context: Context) : WrappedAsyncTaskLoader<List<Song>>(context) {

    override fun loadInBackground(): List<Song>? {
      return DatabaseRepository.getInstance()
          .getPlayQueueSongs()
          .onErrorReturn { throwable ->
            Timber.v(throwable)
            emptyList()
          }
          .blockingGet()
    }

  }

  companion object {

    @JvmStatic
    fun newInstance(): PlayQueueDialog {
      val playQueueDialog = PlayQueueDialog()
      return playQueueDialog
    }

    private var LOADER_ID = 0
  }

}
