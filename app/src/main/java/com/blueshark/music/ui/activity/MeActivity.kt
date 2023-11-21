package com.blueshark.music.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.CompoundButton
import com.afollestad.materialdialogs.DialogAction
import com.blueshark.music.App.Companion.context
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.databinding.ActivityMeBinding
import com.blueshark.music.db.room.DatabaseRepository
import com.blueshark.music.helper.M3UHelper
import com.blueshark.music.helper.ShakeDetector
import com.blueshark.music.misc.MediaScanner
import com.blueshark.music.misc.handler.MsgHandler
import com.blueshark.music.misc.receiver.ExitReceiver
import com.blueshark.music.service.Command
import com.blueshark.music.service.MusicService
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.ui.activity.launchmain.Companion.EXTRA_RECREATE
import com.blueshark.music.ui.activity.launchmain.Companion.EXTRA_REFRESH_ADAPTER
import com.blueshark.music.ui.activity.launchmain.Companion.EXTRA_REFRESH_LIBRARY
import com.blueshark.music.ui.activity.base.BaseActivity
import com.blueshark.music.ui.activity.base.BaseMusicActivity
import com.blueshark.music.ui.misc.FolderChooser
import com.blueshark.music.util.*
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_me.*
import kotlinx.android.synthetic.main.activity_setting.*
import timber.log.Timber
import java.io.File

class MeActivity : BaseActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: ActivityMeBinding

    private val scanSize = intArrayOf(0, 500 * Constants.KB, Constants.MB, 2 * Constants.MB, 5 * Constants.MB)

    private val disposables = ArrayList<Disposable>()

    private var pendingExportPlaylist: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        top_bar.setBackgroundColor(ThemeStore.materialPrimaryColor)
        me_setting.setImageDrawable(Theme.tintVectorDrawable(context, R.drawable.setting, if (ColorUtil.isColorCloseToWhite(ThemeStore.materialPrimaryColor)) Color.BLACK else Color.WHITE))
        me_title1.setTextColor(ThemeStore.accentColor)

        me_setting.setOnClickListener({
            startActivityForResult(Intent(this@MeActivity, SettingActivity::class.java), REQUEST_SETTING)
        })

        layout2.setOnClickListener({
            FolderChooser(
                    this@MeActivity,
                    "Scan",
                    null,
                    SPUtil.SETTING_KEY.NAME,
                    SPUtil.SETTING_KEY.MANUAL_SCAN_FOLDER,
                    object : FolderChooser.FolderCallback {
                        override fun onFolderSelection(chooser: FolderChooser, folder: File) {
                            MediaScanner(this@MeActivity).scanFiles(folder)
                        }
                    }).show()
        })

        layout3.setOnClickListener({
            startActivity(Intent(this@MeActivity, HistoryActivity::class.java))
        })

        layout4.setOnClickListener({
            Timber.v("发送Exit广播")
            sendBroadcast(Intent(Constants.ACTION_EXIT).setComponent(ComponentName(this@MeActivity, ExitReceiver::class.java)))
        })

        //锁屏样式
        val lockScreen = SPUtil.getValue(this@MeActivity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN, Constants.BSPLAYER_LOCKSCREEN)

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

    override fun onBackPressed() {
        val intent = intent
        intent.putExtra(EXTRA_RECREATE, needRecreate)
        intent.putExtra(EXTRA_REFRESH_ADAPTER, needRefreshAdapter)
        intent.putExtra(EXTRA_REFRESH_LIBRARY, needRefreshLibrary)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    var needRecreate: Boolean = false;
    var needRefreshAdapter: Boolean = false;
    var needRefreshLibrary: Boolean = false;

    @SuppressLint("CheckResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SETTING -> {
                if (data == null) {
                    return
                }
                needRecreate = true;
                needRefreshAdapter = true;
                needRefreshLibrary = true;

                recreate()
            }
        }
    }


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
        Theme.getBaseDialog(this).title(R.string.set_filter_size).items("0K", "500K", "1MB", "2MB", "5MB").itemsCallbackSingleChoice(position) { dialog, itemView, which, text ->
            SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCAN_SIZE, scanSize[which])
            MediaStoreUtil.SCAN_SIZE = scanSize[which]
            contentResolver.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
            true
        }.show()
    }

    /**
     * 设置黑名单
     */
    private fun configBlackList() {
        val blackList: Set<String> = SPUtil.getStringSet(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST)
        val items = ArrayList<String>(blackList)
        items.sortWith(Comparator { left, right ->
            File(left).name.compareTo(File(right).name)
        })

        Theme.getBaseDialog(this).items(items).itemsCallback { dialog, itemView, position, text ->
            Theme.getBaseDialog(this).title(R.string.remove_from_blacklist).content(getString(R.string.do_you_want_remove_from_blacklist, text)).onPositive { dialog, which ->
                val mutableSet = LinkedHashSet<String>(blackList)
                val it = mutableSet.iterator()
                while (it.hasNext()) {
                    if (it.next().contentEquals(text)) {
                        it.remove()
                        break
                    }
                }
                SPUtil.putStringSet(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST, mutableSet)
                contentResolver.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
            }.positiveText(R.string.confirm).negativeText(R.string.cancel).show()
        }.title(R.string.blacklist).neutralText(R.string.clear).positiveText(R.string.add).onAny { dialog, which ->
            when (which) {
                DialogAction.NEUTRAL -> {
                    //clear
                    Theme.getBaseDialog(this).title(R.string.clear_blacklist_title).content(R.string.clear_blacklist_content).contentColor(Color.WHITE).negativeText(R.string.cancel).positiveText(R.string.confirm).onPositive { dialog, which ->
                        SPUtil.putStringSet(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST, LinkedHashSet<String>())
                        contentResolver.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
                    }.show()
                }

                DialogAction.POSITIVE -> {
                    //add
                    FolderChooser(this, MeActivity.TAG_BLACKLIST, null, null, null, object : FolderChooser.FolderCallback {
                        override fun onFolderSelection(chooser: FolderChooser, folder: File) {
                            if (folder.isDirectory) {
                                val newBlacklist = LinkedHashSet<String>(blackList)
                                newBlacklist.add(folder.absolutePath)
                                SPUtil.putStringSet(this@MeActivity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST, newBlacklist)
                                contentResolver.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
                            }
                            configBlackList()
                        }
                    }).show()
                }
            }
        }.show()
    }

    /**
     * 配置锁屏界面
     */
    private fun configLockScreen() {
        //0:BSPlayer锁屏 1:系统锁屏 2:关闭
        Theme.getBaseDialog(this).title(R.string.lockscreen_show).title(R.string.lockscreen_show).items(getString(R.string.bsplayer_lockscreen), getString(R.string.system_lockscreen), getString(R.string.close)).positiveText(R.string.confirm).itemsCallbackSingleChoice(SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN, Constants.BSPLAYER_LOCKSCREEN)) { dialog, view, which, text ->
            SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN, which)
            true
        }.show()
    }

    /**
     * 播放列表导入
     */
    @SuppressLint("CheckResult")
    private fun importPlayList() {
        Theme.getBaseDialog(this).title(R.string.choose_import_way).positiveText(R.string.cancel).items(getString(R.string.import_from_external_storage), getString(R.string.import_from_others)).itemsCallback { _, _, select, _ ->
            if (select == 0) {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("m3u")
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                Util.startActivityForResultSafely(this, intent, MeActivity.REQUEST_IMPORT_PLAYLIST)
            } else {
                Single.fromCallable { DatabaseRepository.getInstance().playlistFromMediaStore }.compose(RxUtil.applySingleScheduler()).subscribe({ localPlayLists ->
                    if (localPlayLists == null || localPlayLists.isEmpty()) {
                        ToastUtil.show(this, R.string.import_fail, getString(R.string.no_playlist_can_import))
                        return@subscribe
                    }
                    val selectedIndices = ArrayList<Int>()
                    for (i in 0 until localPlayLists.size) {
                        selectedIndices.add(i)
                    }
                    Theme.getBaseDialog(this).title(R.string.choose_import_playlist).positiveText(R.string.choose).items(localPlayLists.keys).itemsCallbackMultiChoice(selectedIndices.toTypedArray()) { dialog1, which, allSelects ->
                        disposables.add(M3UHelper.importLocalPlayList(this, localPlayLists, allSelects))
                        true
                    }.show()

                }, { throwable ->
                    ToastUtil.show(this, R.string.import_fail, throwable.toString())
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
            Theme.getBaseDialog(this).title(R.string.choose_playlist_to_export).positiveText(R.string.cancel).items(allPlayListNames).itemsCallback { _, _, _, text ->
                pendingExportPlaylist = text.toString()
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("m3u")
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_TITLE, "$text.m3u")
                }
                Util.startActivityForResultSafely(this, intent, MeActivity.REQUEST_EXPORT_PLAYLIST)
            }.show()
        })
    }

    /**
     * 恢复移除的歌曲
     */
    private fun restoreDeleteSong() {
        SPUtil.deleteValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST_SONG)
        contentResolver.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
        ToastUtil.show(this, R.string.alread_restore_songs)
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
            setting_lrc_float_switch.isChecked = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW, false)
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