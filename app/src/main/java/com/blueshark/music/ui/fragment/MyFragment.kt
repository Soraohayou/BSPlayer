package com.blueshark.music.ui.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.CompoundButton
import com.afollestad.materialdialogs.DialogAction
import com.blueshark.music.R
import com.blueshark.music.db.room.DatabaseRepository
import com.blueshark.music.helper.M3UHelper
import com.blueshark.music.misc.handler.MsgHandler
import com.blueshark.music.misc.receiver.ExitReceiver
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.ui.activity.HistoryActivity
import com.blueshark.music.ui.activity.LancherMain
import com.blueshark.music.ui.activity.SearchActivity
import com.blueshark.music.ui.activity.SettingActivity
import com.blueshark.music.ui.fragment.base.BaseMusicFragment
import com.blueshark.music.ui.misc.FolderChooser
import com.blueshark.music.util.*
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_me.*
import kotlinx.android.synthetic.main.activity_setting.*
import timber.log.Timber
import java.io.File

class MyFragment : BaseMusicFragment(), SharedPreferences.OnSharedPreferenceChangeListener {


    private val scanSize = intArrayOf(0, 500 * Constants.KB, Constants.MB, 2 * Constants.MB, 5 * Constants.MB)

    private val disposables = ArrayList<Disposable>()

    private var pendingExportPlaylist: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_me, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

      //  top_bar.setBackgroundColor(ThemeStore.materialPrimaryColor)
        me_setting.setImageDrawable(activity?.let { Theme.tintVectorDrawable(it, R.drawable.setting, if (ColorUtil.isColorCloseToWhite(ThemeStore.materialPrimaryColor)) Color.BLACK else Color.WHITE) })
        me_title1.setTextColor(ThemeStore.accentColor)

        me_setting.setOnClickListener({
            startActivityForResult(Intent(activity, SettingActivity::class.java), REQUEST_SETTING)
        })

        layout2.setOnClickListener({
            startActivity(Intent(activity, SearchActivity::class.java))
        })

        layout3.setOnClickListener({
            startActivity(Intent(activity, HistoryActivity::class.java))
        })

        layout4.setOnClickListener({
            Timber.v("发送Exit广播")
            activity?.sendBroadcast(Intent(Constants.ACTION_EXIT).setComponent(ComponentName(
                requireActivity(), ExitReceiver::class.java)))
        })

        //锁屏样式
        val lockScreen = SPUtil.getValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN, Constants.BSPLAYER_LOCKSCREEN)

        name.setTextColor(if (ColorUtil.isColorCloseToWhite(ThemeStore.materialPrimaryColor)) Color.BLACK else Color.WHITE)

        me_text1.setOnClickListener({ configFilterSize() })
        me_text2.setOnClickListener({ configFilterSize() })
        me_text4.setOnClickListener({ configBlackList() })
        me_text3.setOnClickListener({ configBlackList() })
        me_text5.setOnClickListener({ configLockScreen() })
        me_text6.setOnClickListener({ configLockScreen() })
        me_text7.setOnClickListener({ importPlayList() })
        me_text8.setOnClickListener({ importPlayList() })
        me_text9.setOnClickListener({ exportPlayList() })
        me_text10.setOnClickListener({ exportPlayList() })
        me_text12.setOnClickListener({ restoreDeleteSong() })
        me_text11.setOnClickListener({ restoreDeleteSong() })

    }

    var needRecreate: Boolean = false;
    var needRefreshAdapter: Boolean = false;
    var needRefreshLibrary: Boolean = false;

    /**
     * 配置过滤大小
     */
    private fun configFilterSize() {
        //读取以前设置
        var position = 0
        for (i in scanSize.indices) {
            position = i
            if (scanSize[i] == MediaStoreUtil.SCAN_SIZE) {
                break
            }
        }
        Theme.getBaseDialog(activity).title(R.string.set_filter_size).items("0K", "500K", "1MB", "2MB", "5MB").itemsCallbackSingleChoice(position) { dialog, itemView, which, text ->
            SPUtil.putValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCAN_SIZE, scanSize[which])
            MediaStoreUtil.SCAN_SIZE = scanSize[which]
            activity?.contentResolver?.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
            true
        }.show()
    }

    /**
     * 设置黑名单
     */
    private fun configBlackList() {
        val blackList: Set<String> = SPUtil.getStringSet(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST)
        val items = ArrayList<String>(blackList)
        items.sortWith(Comparator { left, right ->
            File(left).name.compareTo(File(right).name)
        })

        Theme.getBaseDialog(activity).items(items).itemsCallback { dialog, itemView, position, text ->
            Theme.getBaseDialog(activity).title(R.string.remove_from_blacklist).content(getString(R.string.do_you_want_remove_from_blacklist, text)).onPositive { dialog, which ->
                val mutableSet = LinkedHashSet<String>(blackList)
                val it = mutableSet.iterator()
                while (it.hasNext()) {
                    if (it.next().contentEquals(text)) {
                        it.remove()
                        break
                    }
                }
                SPUtil.putStringSet(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST, mutableSet)
                activity?.contentResolver?.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
            }.positiveText(R.string.confirm).negativeText(R.string.cancel).show()
        }.title(R.string.blacklist).neutralText(R.string.clear).positiveText(R.string.add).onAny { dialog, which ->
            when (which) {
                DialogAction.NEUTRAL -> {
                    //clear
                    Theme.getBaseDialog(activity).title(R.string.clear_blacklist_title).content(R.string.clear_blacklist_content).contentColor(Color.WHITE).negativeText(R.string.cancel).positiveText(R.string.confirm).onPositive { dialog, which ->
                        SPUtil.putStringSet(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST, LinkedHashSet<String>())
                        activity?.contentResolver?.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
                    }.show()
                }

                DialogAction.POSITIVE -> {
                    //add
                    FolderChooser(activity as LancherMain, MyFragment.TAG_BLACKLIST, null, null, null, object : FolderChooser.FolderCallback {
                        override fun onFolderSelection(chooser: FolderChooser, folder: File) {
                            if (folder.isDirectory) {
                                val newBlacklist = LinkedHashSet<String>(blackList)
                                newBlacklist.add(folder.absolutePath)
                                SPUtil.putStringSet(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST, newBlacklist)
                                activity?.contentResolver?.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
                            }
                            configBlackList()
                        }
                    }).show()
                }

                else -> {}
            }
        }.show()
    }

    /**
     * 配置锁屏界面
     */
    private fun configLockScreen() {
        //0:BSPlayer锁屏 1:系统锁屏 2:关闭
        Theme.getBaseDialog(activity).title(R.string.lockscreen_show).title(R.string.lockscreen_show).items(getString(R.string.bsplayer_lockscreen), getString(R.string.system_lockscreen), getString(R.string.close)).positiveText(R.string.confirm).itemsCallbackSingleChoice(SPUtil.getValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN, Constants.BSPLAYER_LOCKSCREEN)) { dialog, view, which, text ->
            SPUtil.putValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN, which)
            true
        }.show()
    }

    /**
     * 播放列表导入
     */
    @SuppressLint("CheckResult")
    private fun importPlayList() {
        Theme.getBaseDialog(activity).title(R.string.choose_import_way).positiveText(R.string.cancel).items(getString(R.string.import_from_external_storage), getString(R.string.import_from_others)).itemsCallback { _, _, select, _ ->
            if (select == 0) {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("m3u")
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                activity?.let { Util.startActivityForResultSafely(it, intent, MyFragment.REQUEST_IMPORT_PLAYLIST) }
            } else {
                Single.fromCallable { DatabaseRepository.getInstance().playlistFromMediaStore }.compose(RxUtil.applySingleScheduler()).subscribe({ localPlayLists ->
                    if (localPlayLists == null || localPlayLists.isEmpty()) {
                        ToastUtil.show(activity, R.string.import_fail, getString(R.string.no_playlist_can_import))
                        return@subscribe
                    }
                    val selectedIndices = ArrayList<Int>()
                    for (i in 0 until localPlayLists.size) {
                        selectedIndices.add(i)
                    }
                    Theme.getBaseDialog(activity).title(R.string.choose_import_playlist).positiveText(R.string.choose).items(localPlayLists.keys).itemsCallbackMultiChoice(selectedIndices.toTypedArray()) { dialog1, which, allSelects ->
                        activity?.let { M3UHelper.importLocalPlayList(it, localPlayLists, allSelects) }
                            ?.let { disposables.add(it) }
                        true
                    }.show()

                }, { throwable ->
                    ToastUtil.show(activity, R.string.import_fail, throwable.toString())
                })
            }
        }.theme(ThemeStore.mDDialogTheme).show()
    }

    /**
     * 播放列表导出
     */
    private fun exportPlayList() {
        disposables.add(DatabaseRepository.getInstance().getAllPlaylist().map<List<String>> { playLists ->
            val allplayListNames = ArrayList<String>()
            for ((_, name) in playLists) {
                allplayListNames.add(name)
            }
            allplayListNames
        }.compose(RxUtil.applySingleScheduler()).subscribe { allPlayListNames ->
            Theme.getBaseDialog(activity).title(R.string.choose_playlist_to_export).positiveText(R.string.cancel).items(allPlayListNames).itemsCallback { _, _, _, text ->
                pendingExportPlaylist = text.toString()
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("m3u")
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_TITLE, "$text.m3u")
                }
                activity?.let { Util.startActivityForResultSafely(it, intent, MyFragment.REQUEST_EXPORT_PLAYLIST) }
            }.show()
        })
    }

    /**
     * 恢复移除的歌曲
     */
    private fun restoreDeleteSong() {
        SPUtil.deleteValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST_SONG)
        activity?.contentResolver?.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
        ToastUtil.show(activity, R.string.alread_restore_songs)
    }

    companion object {

        //设置界面
        private const val REQUEST_SETTING = 1

        private const val RECREATE = 100
        private const val CACHE_SIZE = 101
        private const val CLEAR_FINISH = 102
        private const val REQUEST_THEME_COLOR = 0x10
        private const val REQUEST_IMPORT_PLAYLIST = 0x102
        private const val REQUEST_EXPORT_PLAYLIST = 0x103

        private const val TAG_SCAN = "Scan"
        private const val TAG_IMPORT_PLAYLIST = "ImportPlaylist"
        private const val TAG_EXPORT_PLAYLIST = "ExportPlaylist"
        private const val TAG_BLACKLIST = "Blacklist"
    }

    private lateinit var checkedChangedListener: CompoundButton.OnCheckedChangeListener

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String) {
        if (key == SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW) {
            setting_lrc_float_switch.setOnCheckedChangeListener(null)
            setting_lrc_float_switch.isChecked = SPUtil.getValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW, false)
            setting_lrc_float_switch.setOnCheckedChangeListener(checkedChangedListener)
        }
    }

    private val handler: MsgHandler by lazy {
        MsgHandler(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.remove()
        for (disposable in disposables) {
            if (!disposable.isDisposed) {
                disposable.dispose()
            }
        }
    }

}