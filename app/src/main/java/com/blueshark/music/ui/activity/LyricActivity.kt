package com.blueshark.music.ui.activity

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.palette.graphics.Palette
import com.afollestad.materialdialogs.MaterialDialog
import com.blueshark.music.App
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.databinding.ActivityLyricBinding
import com.blueshark.music.helper.EQHelper
import com.blueshark.music.helper.MusicServiceRemote
import com.blueshark.music.lyric.LrcView
import com.blueshark.music.misc.handler.MsgHandler
import com.blueshark.music.misc.handler.OnHandleMessage
import com.blueshark.music.misc.isPortraitOrientation
import com.blueshark.music.service.Command
import com.blueshark.music.service.MusicService
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.ui.activity.base.BaseMusicActivity
import com.blueshark.music.ui.dialog.PlayQueueDialog
import com.blueshark.music.ui.fragment.LyricFragment
import com.blueshark.music.ui.fragment.RecordFragment
import com.blueshark.music.ui.fragment.player.CircleCoverFragment
import com.blueshark.music.ui.fragment.player.CoverFragment
import com.blueshark.music.util.ColorUtil
import com.blueshark.music.util.Constants
import com.blueshark.music.util.DensityUtil
import com.blueshark.music.util.MusicUtil
import com.blueshark.music.util.SPUtil
import com.blueshark.music.util.StatusBarUtil
import com.blueshark.music.util.ToastUtil
import com.blueshark.music.util.Util
import com.google.gson.Gson
import com.ijkplay.AudioTaglib.MediaDecode
import com.ijkplay.AudioTaglib.PId3Info
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_player.top_detail
import kotlinx.android.synthetic.main.activity_player.top_title
import kotlinx.android.synthetic.main.activity_player.view_pager
import kotlinx.android.synthetic.main.layout_player_control.playbar_play_pause
import kotlinx.android.synthetic.main.layout_player_menu.favorite
import kotlinx.android.synthetic.main.layout_player_menu.playbar_model
import kotlinx.android.synthetic.main.layout_player_volume.next_song
import kotlinx.android.synthetic.main.layout_player_volume.volume_container
import kotlinx.android.synthetic.main.layout_player_volume.volume_seekbar
import timber.log.Timber
import kotlin.math.abs

class LyricActivity : BaseMusicActivity() {

    private lateinit var binding: ActivityLyricBinding

    private var valueAnimator: ValueAnimator? = null

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

    //第一次启动的标志变量
    private var firstStart = true


    //Fragment
    private lateinit var coverFragment: CoverFragment
    lateinit var lyricFragment: LyricFragment
        private set

    private var distanceX = 0f
    private var distanceY = 0f
    private var lastY = 0f
    private var lastX = 0f

    private val thresholdX by lazy {
        DensityUtil.dip2px(App.context, 40f)
    }
    private val thresholdY by lazy {
        if (this.isPortraitOrientation()) {
            DensityUtil.dip2px(App.context, 100f)
        } else {
            DensityUtil.dip2px(App.context, 40f)
        }
    }

    /**
     * 更新Handler
     */
    private val handler: MsgHandler by lazy {
        MsgHandler(this)
    }

    /**
     * 更新封面与背景的Handler
     */
    private val audioManager: AudioManager by lazy {
        getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager
    }

    //底部显示控制
    private var bottomConfig = 0
    private val volumeRunnable = Runnable {
        next_song.startAnimation(makeAnimation(next_song, true))
        volume_container.startAnimation(makeAnimation(volume_container, false))
    }
    private val receiver: Receiver = Receiver()

    private val background by lazy {
        //SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.PLAYER_BACKGROUND, BACKGROUND_ADAPTIVE_COLOR)
    }

    override fun setUpTheme() {
//    if (ThemeStore.isLightTheme()) {
//      super.setUpTheme();
//    } else {
//      setTheme(R.style.AudioHolderStyle_Night);
//    }
        val superThemeRes = ThemeStore.themeRes
        val themeRes: Int
        themeRes = when (superThemeRes) {
            R.style.Theme_BSPlayer_Black -> R.style.PlayerActivityStyle_Black
            R.style.Theme_BSPlayer_Dark -> R.style.PlayerActivityStyle_Dark
            else -> R.style.PlayerActivityStyle
        }
        setTheme(themeRes)
    }

    override fun setNavigationBarColor() {
        super.setNavigationBarColor()
        //导航栏变色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && ThemeStore.sColoredNavigation) {
            val navigationColor = ThemeStore.getBackgroundColorMain(this)
            window.navigationBarColor = navigationColor
            Theme.setLightNavigationbarAuto(this, ColorUtil.isColorLight(navigationColor))
        }
    }

    override fun setStatusBarMode() {
        StatusBarUtil.setStatusBarMode(this, ThemeStore.getBackgroundColorMain(this))
    }

//  override fun setStatusBarColor() {
//    when (background) {
//      BACKGROUND_THEME -> {
//        StatusBarUtil.setColorNoTranslucent(this, ThemeStore.getBackgroundColorMain(this))
//      }
//      BACKGROUND_ADAPTIVE_COLOR -> {
//        StatusBarUtil.setTransparent(this)
//      }
//      BACKGROUND_CUSTOM_IMAGE -> {
//        StatusBarUtil.setTransparent(this)
//
//        val file = File(DiskCache.getDiskCacheDir(this, "thumbnail/player"), "player.jpg");
//        if (file.exists()) {
//          Single
//              .fromCallable {
//                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
//                val blurBitmap = StackBlurManager(bitmap).processNatively(10)
//
//                blurBitmap
//              }
//              .compose(RxUtil.applySingleScheduler())
//              .onErrorReturn {
//                return@onErrorReturn BitmapFactory.decodeResource(resources, R.drawable.album_empty_bg_day)
//              }
//              .subscribe({
//                //player_container.background = BitmapDrawable(resources, it)
//                updateSwatch(it)
//              }, {
//
//              })
//        }
//      }
//    }
//  }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLyricBinding.inflate(layoutInflater)
        setContentView(binding.root)

        song = MusicServiceRemote.getCurrentSong()
        if (song == Song.EMPTY_SONG && intent.hasExtra(MusicService.EXTRA_SONG)) {
            song = intent.getParcelableExtra(MusicService.EXTRA_SONG)!!
        }

        setUpBottom()
        setUpFragments()
        setUpTop()
        setUpIndicator()
        setUpSeekBar()
        setUpViewColor()
        Util.registerLocalReceiver(receiver, IntentFilter(ACTION_UPDATE_NEXT))

        arrayOf(
            binding.playbarPlayPause
        ).forEach {
            it.setOnClickListener(onCtrlClick)
        }
        arrayOf(
            binding.favorite
        ).forEach {
            it.setOnClickListener(onOtherClick)
        }

    }

    public override fun onResume() {
        super.onResume()
    }

    override fun onServiceConnected(service: MusicService) {
        super.onServiceConnected(service)
        onMetaChanged()
        onPlayStateChange()
    }

    override fun onStart() {
        super.onStart()
        overridePendingTransition(R.anim.audio_in, 0)
    }

    override fun finish() {
        super.finish()
        coverFragment.clearAnim()
        overridePendingTransition(0, R.anim.audio_out)
    }

    /**
     * 上一首 下一首 播放、暂停
     */
    private val onCtrlClick = View.OnClickListener { v ->
        val intent = Intent(MusicService.ACTION_CMD)
        when (v.id) {
            R.id.playbar_prev -> intent.putExtra(MusicService.EXTRA_CONTROL, Command.PREV)
            R.id.playbar_next -> intent.putExtra(MusicService.EXTRA_CONTROL, Command.NEXT)
            R.id.playbar_play_container -> intent.putExtra(
                MusicService.EXTRA_CONTROL,
                Command.TOGGLE
            )
        }
        Util.sendLocalBroadcast(intent)
    }

    /**
     * 播放模式 播放列表 关闭 隐藏
     */
    private val onOtherClick = View.OnClickListener { v ->
        when (v.id) {
            R.id.playbar_model -> {
                var currentModel = MusicServiceRemote.getPlayModel()
                currentModel =
                    if (currentModel == Constants.MODE_REPEAT) Constants.MODE_LOOP else ++currentModel
                MusicServiceRemote.setPlayModel(currentModel)
                playbar_model.setImageDrawable(
                    Theme.tintDrawable(
                        when (currentModel) {
                            Constants.MODE_LOOP -> R.drawable.play_btn_loop
                            Constants.MODE_SHUFFLE -> R.drawable.play_btn_shuffle
                            else -> R.drawable.play_btn_loop_one
                        }, ThemeStore.playerBtnColor
                    )
                )
                val msg =
                    if (currentModel == Constants.MODE_LOOP) getString(R.string.model_normal) else if (currentModel == Constants.MODE_SHUFFLE) getString(
                        R.string.model_random
                    ) else getString(R.string.model_repeat)
                //刷新下一首
                if (currentModel != Constants.MODE_SHUFFLE) {
                    next_song.text =
                        getString(R.string.next_song, MusicServiceRemote.getNextSong().title)
                }
                ToastUtil.show(this, msg)
            }

            R.id.playbar_playinglist -> PlayQueueDialog.newInstance()
                .show(supportFragmentManager, PlayQueueDialog::class.java.simpleName)

            R.id.top_hide -> onBackPressed()
            R.id.top_more -> {
                @SuppressLint("RestrictedApi") val popupMenu = PopupMenu(this, v, Gravity.TOP)
                //popupMenu.menuInflater.inflate(R.menu.menu_audio_item, popupMenu.menu)
                //popupMenu.setOnMenuItemClickListener(AudioPopupListener(this@LyricActivity, song))

//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//          popupMenu.menu.removeItem(R.id.menu_speed)
//        }
                popupMenu.show()
            }

            R.id.equalizer -> EQHelper.startEqualizer(this)
            R.id.favorite -> {
                binding.favorite.setImageResource(if (MusicServiceRemote.service?.isLove == true) R.drawable.favorite_unchecked else R.drawable.favorite_checked)
                Util.sendCMDLocalBroadcast(Command.LOVE)
            }

            R.id.speed -> {
                Theme.getBaseDialog(this)
                    .title(R.string.speed)
                    .input(
                        SPUtil.getValue(
                            this,
                            SPUtil.SETTING_KEY.NAME,
                            SPUtil.SETTING_KEY.SPEED,
                            "1.0"
                        ),
                        "",
                        MaterialDialog.InputCallback { dialog, input ->
                            var speed = 0f
                            try {
                                speed = java.lang.Float.parseFloat(input.toString())
                            } catch (ignored: Exception) {

                            }

                            if (speed > 2f || speed < 0.5f) {
                                ToastUtil.show(this, R.string.speed_range_tip)
                                return@InputCallback
                            }
                            SPUtil.putValue(
                                this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED,
                                input.toString()
                            )
                        })
                    .show()
            }
        }
    }

    @SuppressLint("CheckResult")
    private val onVolumeClick = View.OnClickListener { v ->
        when (v.id) {
            R.id.volume_down -> Completable
                .fromAction {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_PLAY_SOUND
                    )
                }
                .subscribeOn(Schedulers.io())
                .subscribe()

            R.id.volume_up -> Completable
                .fromAction {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_PLAY_SOUND
                    )
                }
                .subscribeOn(Schedulers.io())
                .subscribe()

            R.id.next_song -> if (bottomConfig == BOTTOM_SHOW_BOTH) {
                next_song.startAnimation(makeAnimation(next_song, false))
                volume_container.startAnimation(makeAnimation(volume_container, true))
                handler.removeCallbacks(volumeRunnable)
                handler.postDelayed(volumeRunnable, DELAY_SHOW_NEXT_SONG.toLong())
            }
        }
        if (v.id != R.id.next_song) {
            Single.zip(
                Single.fromCallable { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) },
                Single.fromCallable { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) },
                BiFunction { max: Int, current: Int ->
                    longArrayOf(
                        max.toLong(),
                        current.toLong()
                    )
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { longs: LongArray ->
                    volume_seekbar.progress = (longs[1] * 1.0 / longs[0] * 100).toInt()
                }
        }
    }

    private fun makeAnimation(view: View, show: Boolean): AlphaAnimation {
        val alphaAnimation = AlphaAnimation(if (show) 0f else 1f, if (show) 1f else 0f)
        alphaAnimation.duration = 300
        alphaAnimation.setAnimationListener(object : Animation.AnimationListener {
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

    }

    /**
     * 初始化seekbar
     */
    @SuppressLint("CheckResult")
    private fun setUpSeekBar() {

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
        favorite.setImageResource(if (MusicServiceRemote.service?.isLove == true) R.drawable.favorite_checked else R.drawable.favorite_unchecked)


        Single.fromCallable { MediaDecode().GetId3Info(song.data) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(
                { pid3info -> updateTagInfo(pid3info) },
                { updateTagInfo(null) }
            )
    }

    private fun updateTagInfo(pid3info: PId3Info?) {

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
        //updateTopStatus(song)
    }

    /**
     * 初始化viewpager
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setUpFragments() {
        val fragmentManager = supportFragmentManager
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        fragmentManager.executePendingTransactions()
        val fragments = fragmentManager.fragments

        for (fragment in fragments) {
            if (fragment is LyricFragment ||
                fragment is CoverFragment ||
                fragment is RecordFragment
            ) {
                fragmentManager.beginTransaction().remove(fragment).commitNow()
            }
        }
        coverFragment = CircleCoverFragment()
        setUpCoverFragment()
        lyricFragment = LyricFragment()
        setUpLyricFragment()
        fragmentManager
            .beginTransaction()
            .replace(R.id.container_cover, coverFragment)
            .replace(R.id.container_lyric, lyricFragment)
            .commit()
        //歌词界面常亮
        if (SPUtil.getValue(
                this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCREEN_ALWAYS_ON,
                false
            )
        ) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        updateTopStatus(song);
        coverFragment.setImage(song,
            true,
            true)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (touchOnCoverFragment(event)) {
                    lastX = event.x
                    lastY = event.y
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (lastX > 0 && lastY > 0 && touchOnCoverFragment(event)) {
                    distanceX += abs(event.x - lastX)
                    distanceY += (event.y - lastY)
                    if (distanceY > thresholdY && distanceX < thresholdX) {
                        onBackPressed()
                    }

                    lastX = event.x
                    lastY = event.y
                }
            }

            else -> {
                lastX = 0f
                lastY = 0f
                distanceY = 0f
                distanceX = 0f
            }
        }

        return super.dispatchTouchEvent(event)
    }

    private fun touchOnCoverFragment(event: MotionEvent): Boolean {
        val rect = Rect()
        return binding.containerCover?.getLocalVisibleRect(rect) && rect.contains(
            event.x.toInt(),
            event.y.toInt()
        )
    }

    private fun setUpLyricFragment() {
        lyricFragment.setOnInflateFinishListener { view: View? ->
            lrcView = view as LrcView
            lrcView?.setOnLrcClickListener(object : LrcView.OnLrcClickListener {
                override fun onClick() {}
                override fun onLongClick() {}
            })

            lrcView?.setOnSeekToListener(object : LrcView.OnSeekToListener {
                override fun onSeekTo(progress: Int) {
                    if (progress > 0 && progress < MusicServiceRemote.getDuration()) {
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
        val newSong = MusicServiceRemote.getCurrentSong()
        updateTopStatus(newSong)
        lyricFragment.updateLrc(newSong)
        song = newSong
        coverFragment.setImage(song, false, true)
    }

    override fun onMetaChanged() {
        super.onMetaChanged()
        song = MusicServiceRemote.getCurrentSong()
        //当操作不为播放或者暂停且正在运行时，更新所有控件
        val operation = MusicServiceRemote.getOperation()
        if (operation != Command.TOGGLE || firstStart) {
            //更新歌词
            handler.postDelayed({ lyricFragment.updateLrc(song) }, 50)
            //更新下一首歌曲
            firstStart = false
        }
        coverFragment.setImage(song,
            operation != Command.TOGGLE && !firstStart,
            operation != Command.TOGGLE)
    }

    override fun onPlayStateChange() {
        super.onPlayStateChange()
        //更新按钮状态
        val isPlay = MusicServiceRemote.isPlaying()
        if (isPlaying != isPlay) {
            updatePlayButton(isPlay)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 初始化底部区域
     */
    private fun setUpBottom() {

    }

    /**
     * 根据主题颜色修改按钮颜色
     */
    private fun setUpViewColor() {
//        val accentColor = ThemeStore.accentColor
//        val tintColor = ThemeStore.playerBtnColor
//        updateSeekBarColor(accentColor)
//        //        mProgressSeekBar.setThumb(Theme.getShape(GradientDrawable.OVAL,ThemeStore.getAccentColor(),DensityUtil.dip2px(context,10),DensityUtil.dip2px(context,10)));
////        Drawable seekbarBackground = mProgressSeekBar.getBackground();
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && seekbarBackground instanceof RippleDrawable) {
////            ((RippleDrawable)seekbarBackground).setColor(ColorStateList.valueOf( ColorUtil.adjustAlpha(ThemeStore.getAccentColor(),0.2f)));
////        }
//
//        //修改控制按钮颜色
//        Theme.tintDrawable(playbar_next, R.drawable.play_btn_next, accentColor)
//        Theme.tintDrawable(playbar_prev, R.drawable.play_btn_pre, accentColor)
//        playbar_play_pause.setBackgroundColor(accentColor)
//
//        //歌曲名颜色
//        //top_title.setTextColor(ThemeStore.playerTitleColor)
//
//        //修改顶部按钮颜色
//        Theme.tintDrawable(top_hide, R.drawable.icon_player_back, tintColor)
//        Theme.tintDrawable(top_more, R.drawable.icon_player_more, tintColor)
//        //播放模式与播放队列
//        val playMode = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL,
//            Constants.MODE_LOOP)
//        Theme.tintDrawable(playbar_model, if (playMode == Constants.MODE_LOOP) R.drawable.play_btn_loop else if (playMode == Constants.MODE_SHUFFLE) R.drawable.play_btn_shuffle else R.drawable.play_btn_loop_one, tintColor)
//        Theme.tintDrawable(playbar_playinglist, R.drawable.play_btn_normal_list, tintColor)
//
//        //音量控制
//        volume_down.drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
//        volume_up.drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
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
        Single
            .fromCallable {
                bitmap
            }
            .map { result: Bitmap ->
                val palette = Palette.from(result).generate()
                if (palette.mutedSwatch != null) {
                    return@map palette.mutedSwatch
                }
                val swatches = ArrayList<Palette.Swatch>(palette.swatches)
                swatches.sortWith(Comparator { o1, o2 -> o1.population.compareTo(o2.population) })

                return@map if (swatches.isNotEmpty()) swatches[0] else Palette.Swatch(
                    Color.GRAY,
                    100
                );
            }
            .onErrorReturnItem(Palette.Swatch(Color.GRAY, 100))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ swatch: Palette.Swatch? ->
                if (swatch == null) {
                    return@subscribe
                }

                updateViewsColorBySwatch(swatch)
                startBGColorAnimation(swatch)

            }) { t: Throwable? -> Timber.v(t) }
    }

    private fun updateViewsColorBySwatch(swatch: Palette.Swatch) {
//        playbar_next.setColorFilter(swatch.rgb, PorterDuff.Mode.SRC_IN)
//        playbar_prev.setColorFilter(swatch.rgb, PorterDuff.Mode.SRC_IN)
//        playbar_play_pause.setBackgroundColor(swatch.rgb)
//
//        volume_down.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)
//        volume_up.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)
//
//        playbar_model.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)
//        playbar_playinglist.setColorFilter(ColorUtil.adjustAlpha(swatch.rgb, 0.5f), PorterDuff.Mode.SRC_IN)
//
//        normalIndicator.setColor(ColorUtil.adjustAlpha(swatch.rgb, 0.3f))
//        highLightIndicator.setColor(swatch.rgb)
//
//        updateSeekBarColor(swatch.rgb)
//        next_song.setBackgroundColor(ColorUtil.adjustAlpha(swatch.rgb, 0.1f))
//
//        //top_title.setTextColor(swatch.titleTextColor)
//        //top_detail.setTextColor(swatch.bodyTextColor)
//
//        top_more.setColorFilter(swatch.titleTextColor, PorterDuff.Mode.SRC_IN)
//        top_hide.setColorFilter(swatch.titleTextColor, PorterDuff.Mode.SRC_IN)
    }

    private fun startBGColorAnimation(swatch: Palette.Swatch) {
//        valueAnimator?.cancel()
//
//        val surfaceColor = Theme.resolveColor(this, R.attr.colorSurface, if (ThemeStore.isLightTheme) Color.WHITE else Color.BLACK)
//        valueAnimator = ValueAnimator.ofObject(ArgbEvaluator(), surfaceColor, swatch.rgb)
//
//        valueAnimator?.addUpdateListener { animation ->
//            val drawable = DrawableGradient(
//                GradientDrawable.Orientation.TOP_BOTTOM,
//                intArrayOf(animation.animatedValue as Int,
//                    surfaceColor
//                ), 0)
//            player_container.background = drawable
//        }
//        valueAnimator?.setDuration(1000)?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handler.remove()
        Util.unregisterLocalReceiver(receiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_LYRIC && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                SPUtil.putValue(
                    this,
                    SPUtil.LYRIC_KEY.NAME,
                    song.id.toString(),
                    SPUtil.LYRIC_KEY.LYRIC_MANUAL
                )
                lyricFragment.updateLrc(uri)
                Util.sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.CHANGE_LYRIC))
            }
        }
    }

    private fun updateProgressText(current: Int) {
//        if (current > 0 && duration - current > 0) {
//            text_hasplay.text = Util.getTime(current.toLong())
//            text_remain.text = Util.getTime((duration - current).toLong())
//        }
    }

    private fun updateProgressByHandler() {
        //updateProgressText(currentTime)
    }

    private fun updateSeekBarByHandler() {
        //seekbar.progress = currentTime
    }

    @OnHandleMessage
    fun handleInternal(msg: Message) {
//        if(msg.what == UPDATE_BG){
//            int colorFrom = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.3f);
//            int colorTo = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.05f);
//            mContainer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{colorFrom, colorTo}));
//        }
//        if (msg.what == UPDATE_TIME_ONLY && !isDragSeekBarFromUser) {
//            updateProgressByHandler()
//        }
//        if (msg.what == UPDATE_TIME_ALL && !isDragSeekBarFromUser) {
//            updateProgressByHandler()
//            updateSeekBarByHandler()
//        }
    }

    private inner class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //更新下一首歌曲
            //next_song.text = getString(R.string.next_song, MusicServiceRemote.getNextSong().title)
        }
    }

    companion object {
        private const val TAG = "LyricActivity"
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