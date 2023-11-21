package com.blueshark.music.ui.fragment

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.os.Bundle
import android.os.Message
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.webkit.MimeTypeMap
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.blueshark.music.App
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.databinding.ActivityPlayerBinding
import com.blueshark.music.helper.DeleteHelper
import com.blueshark.music.helper.MusicServiceRemote
import com.blueshark.music.helper.MusicServiceRemote.getCurrentSong
import com.blueshark.music.helper.MusicServiceRemote.getDuration
import com.blueshark.music.helper.MusicServiceRemote.getNextSong
import com.blueshark.music.helper.MusicServiceRemote.getOperation
import com.blueshark.music.helper.MusicServiceRemote.getPlayModel
import com.blueshark.music.helper.MusicServiceRemote.getProgress
import com.blueshark.music.helper.MusicServiceRemote.isPlaying
import com.blueshark.music.helper.MusicServiceRemote.setPlayModel
import com.blueshark.music.lyric.LrcView
import com.blueshark.music.lyric.LrcView.OnLrcClickListener
import com.blueshark.music.misc.handler.MsgHandler
import com.blueshark.music.misc.handler.OnHandleMessage
import com.blueshark.music.misc.isPortraitOrientation
import com.blueshark.music.service.Command
import com.blueshark.music.service.MusicService
import com.blueshark.music.service.MusicService.Companion.EXTRA_SONG
import com.blueshark.music.theme.DrawableGradient
import com.blueshark.music.theme.GradientDrawableMaker
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.theme.ThemeStore.materialPrimaryColor
import com.blueshark.music.theme.ThemeStore.textColorPrimary
import com.blueshark.music.ui.activity.LyricActivity
import com.blueshark.music.ui.activity.PlayerActivity
import com.blueshark.music.ui.activity.SearchActivity
import com.blueshark.music.ui.activity.SoundEffectActivity
import com.blueshark.music.ui.activity.base.BaseActivity
import com.blueshark.music.ui.adapter.PagerAdapter
import com.blueshark.music.ui.dialog.AddtoPlayListDialog
import com.blueshark.music.ui.dialog.PlayQueueDialog
import com.blueshark.music.ui.dialog.PlayQueueDialog.Companion.newInstance
import com.blueshark.music.ui.dialog.TimerDialog
import com.blueshark.music.ui.fragment.base.BaseMusicFragment
import com.blueshark.music.ui.fragment.player.CircleCoverFragment
import com.blueshark.music.ui.fragment.player.CoverFragment
import com.blueshark.music.ui.fragment.player.SongInfoFragment
import com.blueshark.music.util.*
import com.blueshark.music.util.SPUtil.SETTING_KEY
import com.ijkplay.AudioTaglib.PId3Info
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_lyric.*
import kotlinx.android.synthetic.main.activity_main.top_search
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.activity_player.player_container
import kotlinx.android.synthetic.main.activity_player.top_detail
import kotlinx.android.synthetic.main.activity_player.top_title
import kotlinx.android.synthetic.main.layout_player_control.*
import kotlinx.android.synthetic.main.layout_player_control.playbar_play_pause
import kotlinx.android.synthetic.main.layout_player_menu.*
import kotlinx.android.synthetic.main.layout_player_menu.favorite
import kotlinx.android.synthetic.main.layout_player_topbar.*
import kotlinx.android.synthetic.main.layout_player_volume.*
import timber.log.Timber
import tv.danmaku.ijk.media.player.IjkMediaMeta

/**
 * Created by Remix on 2015/12/1.
 */
/**
 * 播放界面
 */
class PlayerFragment : BaseMusicFragment() {
    private lateinit var binding: ActivityPlayerBinding

    private var valueAnimator: ValueAnimator? = null

    //上次选中的Fragment
    private var prevPosition = 0

    //第一次启动的标志变量
    private var firstStart = true

    //是否正在拖动进度条
    var isDragSeekBarFromUser = false

    //歌词控件
    private var lrcView: LrcView? = null

    //高亮与非高亮指示器
    private lateinit var highLightIndicator: GradientDrawable
    private lateinit var normalIndicator: GradientDrawable
    private val indicators: ArrayList<ImageView> = arrayListOf()

    //当前播放的歌曲
    private lateinit var song: Song

    //当前是否播放
    private var isPlaying = false

    //当前播放时间
    private var currentTime = 0

    //当前歌曲总时长
    private var duration = 0

    //Fragment
    lateinit var lyricFragment: LyricFragment
        private set
    private lateinit var coverFragment: CoverFragment
    private lateinit var tagFragment: SongInfoFragment

    private var distanceX = 0f
    private var distanceY = 0f
    private var lastY = 0f
    private var lastX = 0f

    private val thresholdX by lazy {
        DensityUtil.dip2px(App.context, 40f)
    }
    private val thresholdY by lazy {
        if (context?.isPortraitOrientation() == true) {
            DensityUtil.dip2px(App.context, 100f)
        } else {
            DensityUtil.dip2px(App.context, 40f)
        }
    }

    /**
     * 更新Handler
     */
    private val handler: MsgHandler by lazy {
        MsgHandler(context)
    }

    /**
     * 更新封面与背景的Handler
     */
    private val audioManager: AudioManager by lazy {
        context?.getSystemService(AUDIO_SERVICE) as AudioManager
    }

    //底部显示控制
    private var bottomConfig = 0
    private val volumeRunnable = Runnable {
        next_song.startAnimation(makeAnimation(next_song, true))
        volume_container.startAnimation(makeAnimation(volume_container, false))
    }
    private val receiver: Receiver = Receiver()

    private val background by lazy {
        //SPUtil.getValue(context, SETTING_KEY.NAME, SETTING_KEY.PLAYER_BACKGROUND, BACKGROUND_ADAPTIVE_COLOR)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = ActivityPlayerBinding.inflate(inflater)
        return binding.root
      //  return inflater.inflate(R.layout.activity_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        song = getCurrentSong()
        if (song == Song.EMPTY_SONG && arguments?.getParcelable<Song>(EXTRA_SONG) !=null) {
            song = arguments?.getParcelable<Song>(EXTRA_SONG)!!
        }
    }

    private fun setUpSearch() {
        val isPrimaryColorCloseToWhite = ThemeStore.isMDColorCloseToWhite
        top_search.setBackgroundResource(if (isPrimaryColorCloseToWhite) R.drawable.search_bg_white else R.drawable.search_bg_black)
        top_search.setOnClickListener() {
            startActivity(Intent(context, SearchActivity::class.java))
        }
    }

    private fun  initView(){
        setUpBottom()
        setUpFragments()
        setUpTop()
        setUpIndicator()
        setUpSeekBar()
        setUpViewColor()
        setUpSearch()
        Util.registerLocalReceiver(receiver, IntentFilter(ACTION_UPDATE_NEXT))

        arrayOf(binding.layoutPlayerControl.playbarNext, binding.layoutPlayerControl.playbarPrev, binding.layoutPlayerControl.playbarPlayContainer).forEach {
            it.setOnClickListener(onCtrlClick)
        }
        binding.topActionbar?.let {
            arrayOf(binding.layoutPlayerMenus.playbarModel, binding.layoutPlayerMenus.playbarPlayinglist, it.topHide, binding.layoutPlayerMenus.topMore, binding.layoutPlayerMenus.equalizer, binding.layoutPlayerMenus.speed, binding.layoutPlayerMenus.favorite).forEach {
                it.setOnClickListener(onOtherClick)
            }
        }
        arrayOf(binding.layoutPlayerVolume.volumeDown, binding.layoutPlayerVolume.volumeUp, binding.layoutPlayerVolume.nextSong).forEach {
            it.setOnClickListener(onVolumeClick)
        }
    }

    override fun onResume() {
        super.onResume()
        initView();
        if (context?.isPortraitOrientation() == true) {
            view_pager.currentItem = 0
        }
        //更新进度条
        ProgressThread().start()


    }

    override fun onServiceConnected(service: MusicService) {
        super.onServiceConnected(service)
  //      onMetaChanged()
  //      onPlayStateChange()
    }

    override fun onStart() {
        super.onStart()
   //     overridePendingTransition(R.anim.audio_in, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
 //       coverFragment.clearAnim()
    }

    /**
     * 上一首 下一首 播放、暂停
     */
    private val onCtrlClick = View.OnClickListener { v ->
        val intent = Intent(MusicService.ACTION_CMD)
        when (v.id) {
            R.id.playbar_prev -> intent.putExtra(MusicService.EXTRA_CONTROL, Command.PREV)
            R.id.playbar_next -> intent.putExtra(MusicService.EXTRA_CONTROL, Command.NEXT)
            R.id.playbar_play_container -> intent.putExtra(MusicService.EXTRA_CONTROL, Command.TOGGLE)
        }
        Util.sendLocalBroadcast(intent)
    }

    /**
     * 播放模式 播放列表 关闭 隐藏
     */
    private val onOtherClick = View.OnClickListener { v ->
        when (v.id) {
            R.id.playbar_model -> {
                var currentModel = getPlayModel()
                currentModel = if (currentModel == Constants.MODE_REPEAT) Constants.MODE_LOOP else ++currentModel
                setPlayModel(currentModel)
                playbar_model.setImageDrawable(Theme.tintDrawable(when (currentModel) {
                    Constants.MODE_LOOP -> R.drawable.play_btn_loop
                    Constants.MODE_SHUFFLE -> R.drawable.play_btn_shuffle
                    else -> R.drawable.play_btn_loop_one
                }, ThemeStore.playerBtnColor))
                val msg = if (currentModel == Constants.MODE_LOOP) getString(R.string.model_normal) else if (currentModel == Constants.MODE_SHUFFLE) getString(R.string.model_random) else getString(R.string.model_repeat)
                //刷新下一首
                if (currentModel != Constants.MODE_SHUFFLE) {
                    next_song.text = getString(R.string.next_song, getNextSong().title)
                }
                ToastUtil.show(context, msg)
            }

            R.id.playbar_playinglist -> newInstance().show(childFragmentManager, PlayQueueDialog::class.java.simpleName)

            R.id.top_hide -> startActivity(Intent(context, SoundEffectActivity::class.java))
            R.id.top_more -> {
            val popupMenu = context?.let { PopupMenu(it, v, Gravity.TOP) }
            if (popupMenu != null) {
                popupMenu.menuInflater.inflate(R.menu.menu_audio_item, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.menu_lyric -> {
                            val alreadyIgnore = (SPUtil
                                .getValue(
                                    App.context, SPUtil.LYRIC_KEY.NAME, song.id.toString(),
                                    SPUtil.LYRIC_KEY.LYRIC_DEFAULT) == SPUtil.LYRIC_KEY.LYRIC_IGNORE)

                            //   val  = ref.get()?.lyricFragment ?: return true
                            Theme.getBaseDialog(activity)
                                .items(
                                    getString(R.string.embedded_lyric),
                                    getString(R.string.local),
                                    getString(R.string.kugou),
                                    getString(R.string.netease),
                                    getString(R.string.qq),
                                    getString(R.string.select_lrc),
                                    getString(if (!alreadyIgnore) R.string.ignore_lrc else R.string.cancel_ignore_lrc),
                                    getString(R.string.lyric_adjust_font_size),
                                    getString(R.string.change_offset))
                                .itemsCallback { dialog, itemView, position, text ->
                                    when (position) {
                                        0, 1, 2, 3, 4 -> { //0内嵌 1本地 2酷狗 3网易 4qq
                                            SPUtil.putValue(App.context, SPUtil.LYRIC_KEY.NAME, song.id.toString(), position + 2)
                                            lyricFragment.updateLrc(song, true)
                                            Util.sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.CHANGE_LYRIC))
                                        }
                                        5 -> { //手动选择歌词
                                            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("lrc")
                                                addCategory(Intent.CATEGORY_OPENABLE)
                                            }
                                            activity?.let { it1 ->
                                                Util.startActivityForResultSafely(
                                                    it1,
                                                    intent,
                                                    PlayerActivity.REQUEST_SELECT_LYRIC
                                                )
                                            }
                                        }
                                        6 -> { //忽略或者取消忽略
                                            Theme.getBaseDialog(activity)
                                                .title(if (!alreadyIgnore) R.string.confirm_ignore_lrc else R.string.confirm_cancel_ignore_lrc)
                                                .negativeText(R.string.cancel)
                                                .positiveText(R.string.confirm)
                                                .onPositive { dialog1, which ->
                                                    if (!alreadyIgnore) {//忽略
                                                        SPUtil.putValue(activity, SPUtil.LYRIC_KEY.NAME, song.id.toString(),
                                                            SPUtil.LYRIC_KEY.LYRIC_IGNORE)
                                                        lyricFragment.updateLrc(song)
                                                    } else {//取消忽略
                                                        SPUtil.putValue(activity, SPUtil.LYRIC_KEY.NAME, song.id.toString(),
                                                            SPUtil.LYRIC_KEY.LYRIC_DEFAULT)
                                                        lyricFragment.updateLrc(song)
                                                    }
                                                    Util.sendLocalBroadcast(
                                                        MusicUtil.makeCmdIntent(
                                                            Command.CHANGE_LYRIC
                                                        )
                                                    )
                                                }
                                                .show()
                                        }
                                        7 -> { //字体大小调整
                                            Theme.getBaseDialog(activity)
                                                .items(R.array.lyric_font_size)
                                                .itemsCallback { dialog, itemView, position, text ->
                                                    lyricFragment.setLyricScalingFactor(position)
                                                }
                                                .show()
                                        }
                                        8 -> { //歌词时间轴调整
                                            showLyricOffsetView()
                                        }
                                    }

                                }
                                .show()
                        }
                        R.id.menu_edit -> {
                          //  AudioTag(activity = BaseActivity(), song).edit()
                        }
//      R.id.menu_detail -> {
//        audioTag.detail()
//      }
                        R.id.menu_timer -> {
                          //  val fm = activity?.supportFragmentManager ?: return true
                            activity?.let { it1 -> TimerDialog.newInstance().show(it1.supportFragmentManager, TimerDialog::class.java.simpleName) }
                        }
//      R.id.menu_eq -> {
//        EQHelper.startEqualizer(activity)
//      }
//      R.id.menu_collect -> {
//        DatabaseRepository.getInstance()
//            .insertToPlayList(listOf(song.id), getString(R.string.my_favorite))
//            .compose<Int>(applySingleScheduler<Int>())
//            .subscribe(
//                { count -> ToastUtil.show(activity, getString(R.string.add_song_playlist_success, 1, getString(R.string.my_favorite))) },
//                { throwable -> ToastUtil.show(activity, R.string.add_song_playlist_error) })
//      }
                        R.id.menu_add_to_playlist -> {
                            activity?.let { it1 ->
                                AddtoPlayListDialog.newInstance(listOf(song.id))
                                    .show(it1.supportFragmentManager, AddtoPlayListDialog::class.java.simpleName)
                            }
                        }
                        R.id.menu_delete -> {
                            val checked = arrayOf(SPUtil.getValue(App.context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false))

                            Theme.getBaseDialog(activity)
                                .content(R.string.confirm_delete_from_library)
                                .positiveText(R.string.confirm)
                                .negativeText(R.string.cancel)
                                .checkBoxPromptRes(R.string.delete_source, checked[0], CompoundButton.OnCheckedChangeListener { buttonView, isChecked -> checked[0] = isChecked })
                                .onAny { dialog, which ->
                                    if (which == DialogAction.POSITIVE) {
                                        DeleteHelper.deleteSong(activity= BaseActivity(), song.id, checked[0], false, "")
                                            .compose<Boolean>(RxUtil.applySingleScheduler<Boolean>())
                                            .subscribe({ success ->
                                                if (success) {
                                                    //移除的是正在播放的歌曲
                                                    if (song.id == getCurrentSong().id) {
                                                        Util.sendCMDLocalBroadcast(Command.NEXT)
                                                    }
                                                }
                                                ToastUtil.show(activity, if (success) R.string.delete_success else R.string.delete_error)
                                            }, { ToastUtil.show(activity, R.string.delete) })
                                    }
                                }
                                .show()
                        }
                        //            case R.id.menu_vol:
                        //                AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                        //                if(audioManager != null){
                        //                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                        //                }
//      R.id.menu_speed -> {
//        getBaseDialog(activity)
//            .title(R.string.speed)
//            .input(SPUtil.getValue(App.context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED, "1.0"),
//                "",
//                MaterialDialog.InputCallback { dialog, input ->
//                  var speed = 0f
//                  try {
//                    speed = java.lang.Float.parseFloat(input.toString())
//                  } catch (ignored: Exception) {
//
//                  }
//
//                  if (speed > 2f || speed < 0.5f) {
//                    ToastUtil.show(activity, R.string.speed_range_tip)
//                    return@InputCallback
//                  }
//                  SPUtil.putValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED,
//                      input.toString())
//                })
//            .show()
//      }
                    }
                   true
                }  //AudioPopupListenerNew(activity = BaseActivity(),this,lyricFragment, song)
                popupMenu.show()
            }

            }

            R.id.equalizer -> {
                //EQHelper.startEqualizer(context)
                startActivity(Intent(context, SoundEffectActivity::class.java))
            }
            R.id.favorite -> {
                binding.layoutPlayerMenus.favorite.setImageResource(if (MusicServiceRemote.service?.isLove == true) R.drawable.favorite_unchecked else R.drawable.favorite_checked)
                if (MusicServiceRemote.service?.isLove == true){
                    Theme.tintDrawable(favorite, R.drawable.favorite_unchecked, ThemeStore.playerBtnColor)
                }
                Util.sendCMDLocalBroadcast(Command.LOVE)
            }

            R.id.speed -> {
                Theme.getBaseDialog(context).title(R.string.speed).input(SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED, "1.0"), "", MaterialDialog.InputCallback { dialog, input ->
                            var speed = 0f
                            try {
                                speed = java.lang.Float.parseFloat(input.toString())
                            } catch (ignored: Exception) {

                            }

                            if (speed > 2f || speed < 0.5f) {
                                ToastUtil.show(context, R.string.speed_range_tip)
                                return@InputCallback
                            }
                            SPUtil.putValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED, input.toString())
                            binding.layoutPlayerMenus.speed.setText(input.toString())
                        }).show()
            }
        }
    }

    @SuppressLint("CheckResult")
    private val onVolumeClick = View.OnClickListener { v ->
        when (v.id) {
            R.id.volume_down -> Completable.fromAction {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                    }.subscribeOn(Schedulers.io()).subscribe()

            R.id.volume_up -> Completable.fromAction {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                    }.subscribeOn(Schedulers.io()).subscribe()

            R.id.next_song -> if (bottomConfig == BOTTOM_SHOW_BOTH) {
                next_song.startAnimation(makeAnimation(next_song, false))
                volume_container.startAnimation(makeAnimation(volume_container, true))
                handler.removeCallbacks(volumeRunnable)
                handler.postDelayed(volumeRunnable, DELAY_SHOW_NEXT_SONG.toLong())
            }
        }
        if (v.id != R.id.next_song) {
            Single.zip(Single.fromCallable { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }, Single.fromCallable { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) }, BiFunction { max: Int, current: Int -> longArrayOf(max.toLong(), current.toLong()) }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { longs: LongArray -> volume_seekbar.progress = (longs[1] * 1.0 / longs[0] * 100).toInt() }
        }
    }

    private fun makeAnimation(view: View, show: Boolean): AlphaAnimation {
        val alphaAnimation = AlphaAnimation(if (show) 0f else 1f, if (show) 1f else 0f)
        alphaAnimation.duration = 300
        alphaAnimation.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                View.VISIBLE.also { view.visibility = it }
            }

            override fun onAnimationEnd(animation: Animation) {
                (if (show) View.VISIBLE else View.INVISIBLE).also { view.visibility = it }
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        return alphaAnimation
    }

    /**
     * 初始化三个dot
     */
    private fun setUpIndicator() {

        val width = DensityUtil.dip2px(context, 8f)
        val height = DensityUtil.dip2px(context, 2f)
        highLightIndicator = GradientDrawableMaker().width(width).height(height).color(ThemeStore.accentColor).make()
        normalIndicator = GradientDrawableMaker().width(width).height(height).color(ThemeStore.accentColor).alpha(0.3f).make()
        getView()?.let { indicators.add(it.findViewById(R.id.guide_01)) }
        //indicators.add(findViewById(R.id.guide_02))
        getView()?.let { indicators.add(it.findViewById(R.id.guide_03)) }
        indicators[0].setImageDrawable(highLightIndicator)
        indicators[1].setImageDrawable(normalIndicator)
        //indicators[2].setImageDrawable(normalIndicator)
    }

    /**
     * 初始化seekbar
     */
    @SuppressLint("CheckResult")
    private fun setUpSeekBar() {

        //初始化已播放时间与剩余时间
        duration = song.duration.toInt()
        val temp = getProgress()
        currentTime = if (temp in 1 until duration) temp else 0
        if (duration > 0 && duration - currentTime > 0) {
            text_hasplay.text = Util.getTime(currentTime.toLong())
            text_remain.text = Util.getTime((duration - currentTime).toLong())
        }

        //初始化seekbar
        if (duration > 0 && duration < Int.MAX_VALUE) {
            seekbar.max = duration
        } else {
            seekbar.max = 1000
        }
        if (currentTime in 1 until duration) {
            seekbar.progress = currentTime
        } else {
            seekbar.progress = 0
        }
        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateProgressText(progress)
                }
                handler.sendEmptyMessage(UPDATE_TIME_ONLY)
                currentTime = progress
                lrcView?.seekTo(progress, true, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isDragSeekBarFromUser = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                //没有播放拖动进度条无效
//                if(!mIsPlay){
//                    seekBar.setProgress(0);
//                }
                MusicServiceRemote.setProgress(seekBar.progress)
                isDragSeekBarFromUser = false
            }
        })

        //音量的Seekbar
        Single.zip(Single.fromCallable { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }, Single.fromCallable { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) }, BiFunction { max: Int, current: Int -> intArrayOf(max, current) }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { ints: IntArray ->
                    val current = ints[1]
                    val max = ints[0]
                    volume_seekbar.progress = (current * 1.0 / max * 100).toInt()
                    volume_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                            if (bottomConfig == BOTTOM_SHOW_BOTH) {
                                handler.removeCallbacks(volumeRunnable)
                                handler.postDelayed(volumeRunnable, DELAY_SHOW_NEXT_SONG.toLong())
                            }
                            if (fromUser) {
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (seekBar.progress / 100f * max).toInt(), AudioManager.FLAG_PLAY_SOUND)
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar) {}
                    })
                }
        if (bottomConfig == BOTTOM_SHOW_BOTH) {
            handler.postDelayed(volumeRunnable, DELAY_SHOW_NEXT_SONG.toLong())
        }
    }

    /**
     * 更新顶部歌曲信息
     */
    private fun updateTopStatus(song: Song?) {

        if (song == null) {
            return
        }
        val title = song.title
        val artist = song.artist
        val album = song.album
        if (title == "") {
            top_title.text = getString(R.string.unknown_song)
        } else {
            top_title.text = title
        }
        when {
            artist == "" -> {
                top_detail.text = song.album
            }

            album == "" -> {
                top_detail.text = song.artist
            }

            else -> {
                top_detail.text = String.format("%s-%s", song.artist, song.album)
            }
        }

        speed.setText(SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED, "1.0"))
        favorite.setImageResource(if (MusicServiceRemote.service?.isLove == true) R.drawable.favorite_checked else R.drawable.favorite_unchecked)
        if (MusicServiceRemote.service?.isLove != true){
            Theme.tintDrawable(favorite, R.drawable.favorite_unchecked, ThemeStore.playerBtnColor)
        }
//        Single.fromCallable { MediaDecode().GetId3Info(song.data) }.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe({ pid3info -> updateTagInfo(pid3info) }, { updateTagInfo(null) })

        top_title.setOnClickListener({
            startActivity(Intent(context, LyricActivity::class.java))
        })

        top_detail.setOnClickListener({
            startActivity(Intent(context, LyricActivity::class.java))
        })

    }

    private fun updateTagInfo(pid3info: PId3Info?) {
        val meta: IjkMediaMeta? = MusicServiceRemote.getMediaPlayer()?.mediaInfo?.mMeta

        binding.tagInfo.setText("" + (if (pid3info!!.samplerate == 0) meta!!.mAudioStream!!.sampleRateInline else pid3info!!.samplerate.toString() + "Hz") + " | " + (if (pid3info?.bitsPerSample == 0) "1" else pid3info?.bitsPerSample.toString()) + "bit" + " | " + (if (pid3info?.bitrate == 0) meta!!.mBitrate / 1000 else pid3info?.bitrate) + "Kbps")

        tagFragment.setTag(song, meta, pid3info)
    }

    /**
     * 更新播放、暂停按钮
     */
    private fun updatePlayButton(isPlay: Boolean) {
        isPlaying = isPlay
        playbar_play_pause.updateState(isPlay, true)
    }

    /**
     * 初始化顶部信息
     */
    private fun setUpTop() {
        updateTopStatus(song)
    }

    /**
     * 初始化viewpager
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setUpFragments() {
        val fragmentManager = childFragmentManager
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        fragmentManager.executePendingTransactions()
        val fragments = fragmentManager.fragments

        for (fragment in fragments) {
            if (fragment is LyricFragment || fragment is CoverFragment || fragment is RecordFragment) {
                fragmentManager.beginTransaction().remove(fragment).commitNow()
            }
        }
        coverFragment = CircleCoverFragment()
        setUpCoverFragment()
        lyricFragment = LyricFragment()
        setUpLyricFragment()
        tagFragment = SongInfoFragment()

        if (context?.isPortraitOrientation() == true) {

            //Viewpager
            val adapter = PagerAdapter(childFragmentManager)
            //      adapter.addFragment(mRecordFragment);
            adapter.addFragment(coverFragment)
            //adapter.addFragment(lyricFragment)
            adapter.addFragment(tagFragment)
            view_pager.adapter = adapter
            view_pager.offscreenPageLimit = adapter.count - 1
            view_pager.currentItem = 0

            view_pager.addOnPageChangeListener(object : OnPageChangeListener {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
                override fun onPageSelected(position: Int) {
                    indicators[prevPosition].setImageDrawable(normalIndicator)
                    indicators[position].setImageDrawable(highLightIndicator)
                    prevPosition = position
                }

                override fun onPageScrollStateChanged(state: Int) {}
            })
        } else {
            fragmentManager.beginTransaction().replace(R.id.container_cover, coverFragment).replace(R.id.container_lyric, lyricFragment).replace(R.id.container_tag, tagFragment).commit()
        }

    }

    private fun setUpLyricFragment() {
        lyricFragment.setOnInflateFinishListener { view: View? ->
            lrcView = view as LrcView
            lrcView?.setOnLrcClickListener(object : OnLrcClickListener {
                override fun onClick() {}
                override fun onLongClick() {}
            })

            lrcView?.setOnSeekToListener(object : LrcView.OnSeekToListener {
                override fun onSeekTo(progress: Int) {
                    if (progress > 0 && progress < getDuration()) {
                        MusicServiceRemote.setProgress(progress)
                        currentTime = progress
                        handler.sendEmptyMessage(UPDATE_TIME_ALL)
                    }
                }

            })
            lrcView?.setHighLightColor(ThemeStore.textColorPrimary)
            lrcView?.setOtherColor(ThemeStore.textColorSecondary)
            lrcView?.setTimeLineColor(ThemeStore.textColorSecondary)
        }
    }

    private fun setUpCoverFragment() {
        coverFragment.coverCallback = object : CoverFragment.CoverCallback {
            override fun onBitmap(bitmap: Bitmap?) {
//        if (background == BACKGROUND_ADAPTIVE_COLOR) {
//          updateSwatch(bitmap)
//        }
            }
        }
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        val newSong = getCurrentSong()
        updateTopStatus(newSong)
        lyricFragment.updateLrc(newSong)
        song = newSong
        coverFragment.setImage(song, false, true)
    }

    override fun onMetaChanged() {
        super.onMetaChanged()
        song = getCurrentSong()
        //当操作不为播放或者暂停且正在运行时，更新所有控件
        val operation = getOperation()
        if (operation != Command.TOGGLE || firstStart) {
            //更新顶部信息
            updateTopStatus(song)
            //更新歌词
            handler.postDelayed({ lyricFragment.updateLrc(song) }, 50)
            //更新进度条
            val temp = getProgress()
            currentTime = if (temp in 1 until duration) temp else 0
            duration = song.duration.toInt()
            seekbar.max = duration
            //更新下一首歌曲
            next_song.text = getString(R.string.next_song, getNextSong().title)
            coverFragment.setImage(song, operation != Command.TOGGLE && !firstStart, operation != Command.TOGGLE)
            firstStart = false
        }
    }

    override fun onPlayStateChange() {
        super.onPlayStateChange()
        //更新按钮状态
        val isPlay = isPlaying()
        if (isPlaying != isPlay) {
            updatePlayButton(isPlay)
        }
    }

    //更新进度条线程
    private inner class ProgressThread : Thread() {
        override fun run() {
            while (!activity?.isFinishing!!) {
                try {
                    //音量
                    if (volume_seekbar.visibility == View.VISIBLE) {
                        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        activity!!.runOnUiThread { volume_seekbar.progress = (current * 1.0 / max * 100).toInt() }
                    }
                    if (!isPlaying()) {
                        sleep(500)
                        continue
                    }
                    val progress = getProgress()
                    if (progress in 1 until duration) {
                        currentTime = progress
                        handler.sendEmptyMessage(UPDATE_TIME_ALL)
                        sleep(500)
                    }
                } catch (ignore: Exception) {
                }
            }
        }
    }

    /**
     * 初始化底部区域
     */
    private fun setUpBottom() {
        bottomConfig = SPUtil.getValue(context, SETTING_KEY.NAME, SETTING_KEY.BOTTOM_OF_NOW_PLAYING_SCREEN, BOTTOM_SHOW_NONE)
        if (!context?.isPortraitOrientation()!!) { //横屏不显示底部
            bottomConfig = BOTTOM_SHOW_NONE
        }
        when (bottomConfig) {
            BOTTOM_SHOW_NEXT -> { //仅显示下一首
                volume_container.visibility = View.GONE
                next_song.visibility = View.VISIBLE
            }

            BOTTOM_SHOW_VOLUME -> { //仅显示音量控制
                volume_container.visibility = View.VISIBLE
                next_song.visibility = View.GONE
            }

            BOTTOM_SHOW_NONE -> { //关闭
                val volumeLayout = getView()?.findViewById<View>(R.id.layout_player_volume)
                if (volumeLayout != null) {
                    volumeLayout.visibility = View.INVISIBLE
                }
                val volumeLp = volumeLayout?.layoutParams as LinearLayout.LayoutParams
                volumeLp.weight = 0f
                if (volumeLayout != null) {
                    volumeLayout.layoutParams = volumeLp
                }
                val controlLayout = getView()?.findViewById<View>(R.id.layout_player_control)
                val controlLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
                controlLp.weight = 2f
                if (controlLayout != null) {
                    controlLayout.layoutParams = controlLp
                }
            }
        }
    }

    /**
     * 根据主题颜色修改按钮颜色
     */
    private fun setUpViewColor() {
        val materialPrimaryColor = if (ColorUtil.isColorCloseToBlack(materialPrimaryColor)) Color.WHITE else Color.BLACK
        val tintColor = ThemeStore.playerBtnColor
        updateSeekBarColor(materialPrimaryColor)
        //        mProgressSeekBar.setThumb(Theme.getShape(GradientDrawable.OVAL,ThemeStore.getAccentColor(),DensityUtil.dip2px(context,10),DensityUtil.dip2px(context,10)));
//        Drawable seekbarBackground = mProgressSeekBar.getBackground();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && seekbarBackground instanceof RippleDrawable) {
//            ((RippleDrawable)seekbarBackground).setColor(ColorStateList.valueOf( ColorUtil.adjustAlpha(ThemeStore.getAccentColor(),0.2f)));
//        }

        //修改控制按钮颜色
        Theme.tintDrawable(playbar_next, R.drawable.play_btn_next, textColorPrimary)
        Theme.tintDrawable(playbar_prev, R.drawable.play_btn_pre, textColorPrimary)
        playbar_play_pause.setBackgroundColor(textColorPrimary)

        Theme.tintDrawable(top_commit, R.drawable.ic_player_topbar_commit, tintColor)


        //歌曲名颜色
        top_title.setTextColor(ThemeStore.textColorPrimary)
        top_detail.setTextColor(ThemeStore.textColorPrimary)

        //修改顶部按钮颜色
        Theme.tintDrawable(top_hide, R.drawable.icon_player_back, tintColor)
        Theme.tintDrawable(top_more, R.drawable.icon_player_more, tintColor)
        //播放模式与播放队列
        val playMode = SPUtil.getValue(context, SETTING_KEY.NAME, SETTING_KEY.PLAY_MODEL, Constants.MODE_LOOP)
        Theme.tintDrawable(playbar_model, if (playMode == Constants.MODE_LOOP) R.drawable.play_btn_loop else if (playMode == Constants.MODE_SHUFFLE) R.drawable.play_btn_shuffle else R.drawable.play_btn_loop_one, tintColor)
        Theme.tintDrawable(playbar_playinglist, R.drawable.play_btn_normal_list, tintColor)
        Theme.tintDrawable(equalizer, R.drawable.icon_equalizer, tintColor)
        Theme.tintDrawable(speed, R.drawable.bg_quick, tintColor)
        speed.setTextColor(tintColor)

        //音量控制
        volume_down.drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
        volume_up.drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
        //        Theme.tintDrawable(mVolumeDown,R.drawable.ic_volume_down_black_24dp,tintColor);
//        Theme.tintDrawable(mVolumeUp,R.drawable.ic_volume_up_black_24dp,tintColor);
        //下一首背景
//    next_song.background = GradientDrawableMaker()
//        .color(ThemeStore.playerNextSongBgColor)
//        .corner(DensityUtil.dip2px(2f).toFloat())
//        .width(DensityUtil.dip2px(288f))
//        .height(DensityUtil.dip2px(38f))
//        .make()
//    next_song.setTextColor(ThemeStore.playerNextSongTextColor)
    }

    private fun updateSeekBarColor(color: Int) {
        setProgressDrawable(seekbar, color)
        setProgressDrawable(volume_seekbar, color)

        //修改thumb
        val inset = DensityUtil.dip2px(context, 6f)
        val width = DensityUtil.dip2px(context, 2f)
        val height = DensityUtil.dip2px(context, 6f)
        seekbar.thumb = InsetDrawable(GradientDrawableMaker().width(width).height(height).color(color).make(), inset, inset, inset, inset)
        volume_seekbar.thumb = InsetDrawable(GradientDrawableMaker().width(width).height(height).color(color).make(), inset, inset, inset, inset)
    }

    private fun setProgressDrawable(seekBar: SeekBar, color: Int) {
        val progressDrawable = seekBar.progressDrawable as LayerDrawable
        //修改progress颜色
        (progressDrawable.getDrawable(0) as GradientDrawable).setColor(ThemeStore.playerProgressColor)
        progressDrawable.getDrawable(1).setColorFilter(color, PorterDuff.Mode.SRC_IN)
        seekBar.progressDrawable = progressDrawable
    }

    @SuppressLint("CheckResult")
    private fun updateSwatch(bitmap: Bitmap?) {
        Single.fromCallable {
                    bitmap
                }.map { result: Bitmap ->
                    val palette = Palette.from(result).generate()
                    if (palette.mutedSwatch != null) {
                        return@map palette.mutedSwatch
                    }
                    val swatches = ArrayList<Swatch>(palette.swatches)
                    swatches.sortWith(Comparator { o1, o2 -> o1.population.compareTo(o2.population) })

                    return@map if (swatches.isNotEmpty()) swatches[0] else Swatch(Color.GRAY, 100);
                }.onErrorReturnItem(Swatch(Color.GRAY, 100)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ swatch: Swatch? ->
                    if (swatch == null) {
                        return@subscribe
                    }

                    updateViewsColorBySwatch(swatch)
                    startBGColorAnimation(swatch)

                }) { t: Throwable? -> Timber.v(t) }
    }

    private fun updateViewsColorBySwatch(swatch: Swatch) {
        playbar_next.setColorFilter(swatch.rgb, PorterDuff.Mode.SRC_IN)
        playbar_prev.setColorFilter(swatch.rgb, PorterDuff.Mode.SRC_IN)
        playbar_play_pause.setBackgroundColor(swatch.rgb)

        volume_down.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)
        volume_up.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)

        playbar_model.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)
        playbar_playinglist.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)

        normalIndicator.setColor(ColorUtil.adjustAlpha(swatch.rgb, 0.3f))
        highLightIndicator.setColor(swatch.rgb)

        updateSeekBarColor(swatch.rgb)
        next_song.setBackgroundColor(ColorUtil.adjustAlpha(swatch.rgb, 0.1f))

        //top_title.setTextColor(swatch.titleTextColor)
        //top_detail.setTextColor(swatch.bodyTextColor)

        top_more.setColorFilter(swatch.titleTextColor, PorterDuff.Mode.SRC_IN)
        top_hide.setColorFilter(swatch.titleTextColor, PorterDuff.Mode.SRC_IN)
    }

    private fun startBGColorAnimation(swatch: Swatch) {
        valueAnimator?.cancel()

        val surfaceColor = Theme.resolveColor(context, R.attr.colorSurface, if (ThemeStore.isLightTheme) Color.WHITE else Color.BLACK)
        valueAnimator = ValueAnimator.ofObject(ArgbEvaluator(), surfaceColor, swatch.rgb)

        valueAnimator?.addUpdateListener { animation ->
            val drawable = DrawableGradient(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(animation.animatedValue as Int, surfaceColor), 0)
            player_container.background = drawable
        }
        valueAnimator?.setDuration(1000)?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.remove()
        Util.unregisterLocalReceiver(receiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_LYRIC && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                SPUtil.putValue(context, SPUtil.LYRIC_KEY.NAME, song.id.toString(), SPUtil.LYRIC_KEY.LYRIC_MANUAL)
                lyricFragment.updateLrc(uri)
                Util.sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.CHANGE_LYRIC))
            }
        }
    }

    private fun updateProgressText(current: Int) {
        if (current > 0 && duration - current > 0) {
            text_hasplay.text = Util.getTime(current.toLong())
            text_remain.text = Util.getTime((duration - current).toLong())
        }
    }

    private fun updateProgressByHandler() {
        updateProgressText(currentTime)
    }

    private fun updateSeekBarByHandler() {
        seekbar.progress = currentTime
    }

    @OnHandleMessage
    fun handleInternal(msg: Message) {
//        if(msg.what == UPDATE_BG){
//            int colorFrom = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.3f);
//            int colorTo = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.05f);
//            mContainer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{colorFrom, colorTo}));
//        }
        if (msg.what == UPDATE_TIME_ONLY && !isDragSeekBarFromUser) {
            updateProgressByHandler()
        }
        if (msg.what == UPDATE_TIME_ALL && !isDragSeekBarFromUser) {
            updateProgressByHandler()
            updateSeekBarByHandler()
        }
    }

    fun showLyricOffsetView() {
        //todo
        if (view_pager.currentItem != 1) {
            view_pager.setCurrentItem(1, true)
        }
        lyricFragment.showLyricOffsetView()
    }

    private inner class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //更新下一首歌曲
            next_song.text = getString(R.string.next_song, getNextSong().title)
        }
    }

    companion object {
        private const val TAG = "PlayerFragment"
        private const val UPDATE_BG = 1
        private const val UPDATE_TIME_ONLY = 2
        private const val UPDATE_TIME_ALL = 3

        const val BOTTOM_SHOW_NEXT = 0
        const val BOTTOM_SHOW_VOLUME = 1
        const val BOTTOM_SHOW_BOTH = 2
        const val BOTTOM_SHOW_NONE = 3

        const val BACKGROUND_THEME = 0
        const val BACKGROUND_ADAPTIVE_COLOR = 1
        const val BACKGROUND_CUSTOM_IMAGE = 2

        private const val FRAGMENT_COUNT = 2
        private const val DELAY_SHOW_NEXT_SONG = 3000

        const val ACTION_UPDATE_NEXT = "com.blueshark.music.update.next_song"

        const val REQUEST_SELECT_LYRIC = 0x104
    }
}