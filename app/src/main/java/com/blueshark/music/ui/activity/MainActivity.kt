package com.blueshark.music.ui.activity


import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.blueshark.music.App
import com.blueshark.music.R
import com.blueshark.music.bean.misc.CustomCover
import com.blueshark.music.bean.misc.Library
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.db.room.DatabaseRepository
import com.blueshark.music.db.room.model.PlayList
import com.blueshark.music.glide.GlideApp
import com.blueshark.music.glide.UriFetcher
import com.blueshark.music.helper.MusicServiceRemote
import com.blueshark.music.helper.SortOrder
import com.blueshark.music.misc.MediaScanner
import com.blueshark.music.misc.cache.DiskCache
import com.blueshark.music.misc.handler.MsgHandler
import com.blueshark.music.misc.handler.OnHandleMessage
import com.blueshark.music.misc.interfaces.OnItemClickListener
import com.blueshark.music.misc.menu.LibraryListener.Companion.EXTRA_COVER
import com.blueshark.music.misc.receiver.ExitReceiver
import com.blueshark.music.service.Command
import com.blueshark.music.service.MusicService
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.theme.ThemeStore.highLightTextColor
import com.blueshark.music.ui.adapter.DrawerAdapter
import com.blueshark.music.ui.adapter.MainPagerAdapter
import com.blueshark.music.ui.adapter.SongAdapter
import com.blueshark.music.ui.fragment.*
import com.blueshark.music.ui.misc.FolderChooser
import com.blueshark.music.ui.misc.MultipleChoice
import com.blueshark.music.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView
import com.blueshark.music.util.*
import com.blueshark.music.util.RxUtil.applySingleScheduler
import com.blueshark.music.util.Util.hashKeyForDisk
import com.bumptech.glide.signature.ObjectKey
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringSystem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.soundcloud.android.crop.Crop
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_timer.view.*
import kotlinx.android.synthetic.main.navigation_header.*
import timber.log.Timber
import java.io.File
import java.util.*


/**
 *
 */
class launchmain : MenuActivity(), View.OnClickListener {

    private val drawerAdapter by lazy {
        DrawerAdapter(R.layout.item_drawer)
    }
    private val pagerAdapter by lazy {
        MainPagerAdapter(supportFragmentManager)
    }

    private val handler by lazy {
        MsgHandler(this)
    }

    private var popupWindow: PopupWindow? = null
    private var popContentView: View? = null

    var adapter: BaseAdapter? = null

    //当前选中的fragment
    private var currentFragment: LibraryFragment<*, *>? = null

    private var menuLayoutId = R.menu.menu_main

    /**
     * 判断安卓版本，请求安装权限或者直接安装
     *
     * @param activity
     * @param path
     */
    private var installPath: String? = null


    private var forceDialog: MaterialDialog? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (hasNewIntent) {
            handler.postDelayed({ this.parseIntent() }, 500)
            handler.post {
                onMetaChanged()
            }
            hasNewIntent = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //初始化控件
        setUpToolbar()
        setUpPager()
        setUpTab()
        setUpSearch()
        btn_add.setOnClickListener(this)
        //初始化测滑菜单
        setUpDrawerLayout()
        setUpViewColor()

        //清除多选显示状态
        MultipleChoice.isActiveSomeWhere = false
    }

    override fun setStatusBarColor() {
        StatusBarUtil.setColorNoTranslucentForDrawerLayout(this, findViewById(R.id.drawer), ThemeStore.statusBarColor)
    }

    /**
     * 初始化toolbar
     */
    private fun setUpToolbar() {
        super.setUpToolbar("")
        toolbar?.setNavigationIcon(R.drawable.ic_menu_white_24dp)

        toolbar?.setNavigationOnClickListener { v ->
            run {
                startActivityForResult(Intent(this@launchmain, MeActivity::class.java), REQUEST_SETTING)
                //  目前为打开我的界面,后续需要改成推荐歌曲文本，我的界面移至fragment
                // drawer.openDrawer(navigation_view)
            }
        }
    }

    /**
     * 新建播放列表
     */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_add -> {
                if (MultipleChoice.isActiveSomeWhere) {
                    return
                }

                DatabaseRepository.getInstance().getAllPlaylist().compose<List<PlayList>>(applySingleScheduler<List<PlayList>>()).subscribe { playLists ->
                    Theme.getBaseDialog(this).title(R.string.new_playlist).positiveText(R.string.create).negativeText(R.string.cancel).inputRange(1, 25).input("", getString(R.string.local_list) + playLists.size) { dialog, input ->
                        if (!TextUtils.isEmpty(input)) {
                            DatabaseRepository.getInstance().insertPlayList(input.toString()).compose(applySingleScheduler()).subscribe({ id ->
                                //跳转到添加歌曲界面
                                SongChooseActivity.start(this@launchmain, id, input.toString())
                            }, { throwable ->
                                ToastUtil.show(this, R.string.create_playlist_fail, throwable.toString())
                            })
                        }
                    }.show()
                }
            }

            else -> {
            }
        }
    }

    //初始化ViewPager
    private fun setUpPager() {
        val libraryJson = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY, "")
        val libraries = if (TextUtils.isEmpty(libraryJson)) ArrayList()
        else Gson().fromJson<ArrayList<Library>>(libraryJson, object : TypeToken<List<Library>>() {}.type)
        if (libraries.isEmpty()) {
            val defaultLibraries = Library.getDefaultLibrary()
            libraries.addAll(defaultLibraries)
            SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY, Gson().toJson(defaultLibraries, object : TypeToken<List<Library>>() {}.type))
        }

        pagerAdapter.list = listOf(libraries[0]);
        menuLayoutId = parseMenuId(pagerAdapter.list[0].tag)
        //有且仅有一个tab
        if (libraries.size == 1) {
            if (libraries[0].isPlayList()) {
                showViewWithAnim(btn_add, true)
            }
            tabs.visibility = View.GONE
        } else {
            tabs.visibility = View.VISIBLE
        }

        view_pager.adapter = pagerAdapter
        view_pager.offscreenPageLimit = pagerAdapter.count - 1
        view_pager.currentItem = 0
        view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                val library = pagerAdapter.list[position]
                showViewWithAnim(btn_add, library.isPlayList())

                menuLayoutId = parseMenuId(pagerAdapter.list[position].tag)
                currentFragment = pagerAdapter.getFragment(position) as LibraryFragment<*, *>

                invalidateOptionsMenu()
            }


            override fun onPageScrollStateChanged(state: Int) {}
        })
        currentFragment = pagerAdapter.getFragment(0) as LibraryFragment<*, *>
    }

    fun parseMenuId(tag: Int): Int {
        return when (tag) {
            Library.TAG_SONG -> R.menu.menu_main
            Library.TAG_ALBUM -> R.menu.menu_album
            Library.TAG_ARTIST -> R.menu.menu_artist
            Library.TAG_PLAYLIST -> R.menu.menu_playlist
            Library.TAG_FOLDER -> R.menu.menu_folder
            else -> R.menu.menu_main_simple
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if (currentFragment is FolderFragment) {
            return true
        }
        var sortOrder = ""
        when (currentFragment) {
            is SongFragment -> sortOrder = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, SortOrder.SONG_A_Z)

            is AlbumFragment -> sortOrder = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER, SortOrder.ALBUM_A_Z)

            is ArtistFragment -> sortOrder = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER, SortOrder.ARTIST_A_Z)

            is PlayListFragment -> sortOrder = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER, SortOrder.PLAYLIST_DATE)
        }

        if (TextUtils.isEmpty(sortOrder)) {
            return true
        }
        setUpMenuItem(menu, sortOrder)
        return true
    }


    override fun getMenuLayoutId(): Int {
        return menuLayoutId
    }

    override fun saveSortOrder(sortOrder: String) {
        when (currentFragment) {
            is SongFragment -> SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, sortOrder)

            is AlbumFragment -> SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER, sortOrder)

            is ArtistFragment -> SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER, sortOrder)

            is PlayListFragment -> SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER, sortOrder)
        }
        currentFragment?.onMediaStoreChanged()
    }

    private fun showViewWithAnim(view: View, show: Boolean) {
        if (show) {
            if (view.visibility != View.VISIBLE) {
                view.visibility = View.VISIBLE
                SpringSystem.create().createSpring().addListener(object : SimpleSpringListener() {
                    override fun onSpringUpdate(spring: Spring?) {
                        spring?.apply {
                            view.scaleX = currentValue.toFloat()
                            view.scaleY = currentValue.toFloat()
                        }

                    }
                }).endValue = 1.0
            }
        } else {
            view.visibility = View.GONE
        }

    }

    //初始化custontab
    private fun setUpTab() {

        //添加tab选项卡
        layout1.setBackgroundColor(ThemeStore.materialPrimaryColor)
        // TODO 这里可能需要判断一下字体的颜色
        //val isPrimaryColorCloseToWhite = ThemeStore.isMDColorCloseToWhite
        //tabs.setBackgroundColor(if (isPrimaryColorCloseToWhite) Color.BLACK else Color.WHITE)
        /*tabs.addTab(tabs.newTab().setText(R.string.tab_song))
        tabs.addTab(tabs.newTab().setText(R.string.tab_album))
        tabs.addTab(tabs.newTab().setText(R.string.tab_artist))
        tabs.addTab(tabs.newTab().setText(R.string.tab_playlist))
        tabs.addTab(tabs.newTab().setText(R.string.tab_folder))
        //viewpager与tablayout关联
        tabs.setupWithViewPager(view_pager)*/

        //        tabs.setSelectedTabIndicatorColor(ColorUtil.getColor(isLightColor ? R.color.black : R.color.white));
        /*tabs.setSelectedTabIndicatorHeight(DensityUtil.dip2px(this, 3f))
        tabs.setTabTextColors(ColorUtil.getColor(
            if (isPrimaryColorCloseToWhite)
              R.color.dark_normal_tab_text_color
            else
              R.color.light_normal_tab_text_color),
            ColorUtil.getColor(if (isPrimaryColorCloseToWhite) R.color.black else R.color.white))

        setTabClickListener()
        tabs.post {
          for (i in 0..tabs.tabCount) {
            ((tabs.getTabAt(i)?.view?.getChildAt(1)) as TextView?)?.apply {
              if (layout != null && layout.lineCount > 1) {
                maxLines = 1
              }
            }
          }
        }*/

        main_tabs_music.setOnClickListener {
            val intent = Intent(this@launchmain, TypeActivty::class.java);
            intent.putExtra("tab_name", R.string.tab_song);
            intent.putExtra("tab_target", Library.TAG_SONG)
            startActivity(intent)
        }

        main_tabs_album.setOnClickListener {
            val intent = Intent(this@launchmain, TypeActivty::class.java);
            intent.putExtra("tab_name", R.string.tab_album);
            intent.putExtra("tab_target", Library.TAG_ALBUM)
            startActivity(intent)
        }

        main_tabs_artist.setOnClickListener {
            val intent = Intent(this@launchmain, TypeActivty::class.java);
            intent.putExtra("tab_name", R.string.tab_artist);
            intent.putExtra("tab_target", Library.TAG_ARTIST)
            startActivity(intent)
        }

        main_tabs_playlist.setOnClickListener {
            val intent = Intent(this@launchmain, TypeActivty::class.java);
            intent.putExtra("tab_name", R.string.tab_playlist);
            intent.putExtra("tab_target", Library.TAG_PLAYLIST)
            startActivity(intent)
        }

        main_tabs_folder.setOnClickListener {
            val intent = Intent(this@launchmain, TypeActivty::class.java);
            intent.putExtra("tab_name", R.string.tab_folder);
            intent.putExtra("tab_target", Library.TAG_FOLDER)
            startActivity(intent)
        }

    }

    private fun setUpSearch() {
        top_search.setOnClickListener() {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    private fun setTabClickListener() {/*for (i in 0 until tabs.tabCount) {
          val tab = tabs.getTabAt(i) ?: return
          tab.view.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick(v: View) {
              // 只有第一个标签可能是"歌曲"
              if (currentFragment is SongFragment) {
                // 滚动到当前的歌曲
                val fragments = supportFragmentManager.fragments
                for (fragment in fragments) {
                  if (fragment is SongFragment) {
                    fragment.scrollToCurrent()
                  }
                }
              }
            }
          })
        }*/
    }

    private fun setUpDrawerLayout() {
        drawerAdapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                when (position) {
                    //歌曲库
                    0 -> drawer.closeDrawer(navigation_view)
                    1 -> startActivity(Intent(this@launchmain, HistoryActivity::class.java))
                    //最近添加
//          2 -> startActivity(Intent(this@launchmain, RecentlyActivity::class.java))
                    2 -> {
                        drawer.closeDrawer(navigation_view)
                        FolderChooser(this@launchmain, "Scan", null, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MANUAL_SCAN_FOLDER, object : FolderChooser.FolderCallback {
                            override fun onFolderSelection(chooser: FolderChooser, folder: File) {
                                MediaScanner(this@launchmain).scanFiles(folder)
                            }
                        }).show()
                    }
                    //设置
                    3 -> startActivityForResult(Intent(this@launchmain, SettingActivity::class.java), REQUEST_SETTING)
                    //退出
                    4 -> {
                        Timber.v("发送Exit广播")
                        sendBroadcast(Intent(Constants.ACTION_EXIT).setComponent(ComponentName(this@launchmain, ExitReceiver::class.java)))
                    }
                }
                drawerAdapter.setSelectIndex(position)
            }

            override fun onItemLongClick(view: View, position: Int) {}
        }
        recyclerview.adapter = drawerAdapter
        recyclerview.layoutManager = LinearLayoutManager(this)

        drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {
                drawerAdapter.setSelectIndex(0)
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    /**
     * 初始化控件相关颜色
     */
    private fun setUpViewColor() {

        val isPrimaryColorCloseToWhite = ColorUtil.isColorCloseToWhite(ThemeStore.materialPrimaryColor)
        Log.i("TAG", "setUpViewColor: ${isPrimaryColorCloseToWhite}" )

        text1.setTextColor(if (isPrimaryColorCloseToWhite) Color.BLACK else Color.WHITE)
        text2.setTextColor(if (isPrimaryColorCloseToWhite) Color.BLACK else Color.WHITE)
        text3.setTextColor(if (isPrimaryColorCloseToWhite) Color.BLACK else Color.WHITE)
        text4.setTextColor(if (isPrimaryColorCloseToWhite) Color.BLACK else Color.WHITE)
        text5.setTextColor(if (isPrimaryColorCloseToWhite) Color.BLACK else Color.WHITE)

        top_searchbar.setBackgroundColor(ThemeStore.materialPrimaryColor)
        top_search.setBackgroundResource(if (isPrimaryColorCloseToWhite) R.drawable.search_bg_white else R.drawable.search_bg_black)

        drawer.setBackgroundColor(ThemeStore.materialPrimaryColor);

        //正在播放文字的背景
        val bg = GradientDrawable()
        val primaryColor = ThemeStore.materialPrimaryColor

        bg.setColor(ColorUtil.darkenColor(primaryColor))
        bg.cornerRadius = DensityUtil.dip2px(this, 4f).toFloat()
        tv_header.background = bg
        tv_header.setTextColor(ThemeStore.materialPrimaryColorReverse)
        //抽屉
        header.setBackgroundColor(primaryColor)
        navigation_view.setBackgroundColor(ThemeStore.drawerDefaultColor)

        //这种图片不知道该怎么着色 暂时先这样处理
        btn_add.background = Theme.tintDrawable(R.drawable.bg_playlist_add, ThemeStore.accentColor)
        btn_add.setImageResource(R.drawable.icon_playlist_add)
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        onMetaChanged()
        //    mRefreshHandler.sendEmptyMessage(MSG_UPDATE_ADAPTER);
    }

    @SuppressLint("CheckResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SETTING -> {
                if (data == null) {
                    return
                }
                handler.sendEmptyMessage(MSG_RECREATE_ACTIVITY)
                /*if (data.getBooleanExtra(EXTRA_RECREATE, false)) { //设置后需要重启activity
                    handler.sendEmptyMessage(MSG_RECREATE_ACTIVITY)
                } else if (data.getBooleanExtra(EXTRA_REFRESH_ADAPTER, false)) { //刷新adapter
                    UriFetcher.updateAllVersion()
                    UriFetcher.clearAllCache()
                    GlideApp.get(this).clearMemory()
                    handler.sendEmptyMessage(MSG_UPDATE_ADAPTER)
                } else if (data.getBooleanExtra(EXTRA_REFRESH_LIBRARY, false)) { //刷新Library
                    val libraries = data.getSerializableExtra(EXTRA_LIBRARY) as List<Library>?
                    if (libraries != null && libraries.isNotEmpty()) {
                        pagerAdapter.list = libraries
                        pagerAdapter.notifyDataSetChanged()
                        view_pager.offscreenPageLimit = libraries.size - 1
                        menuLayoutId = parseMenuId(pagerAdapter.list[view_pager.currentItem].tag)
                        currentFragment = pagerAdapter.getFragment(view_pager.currentItem) as LibraryFragment<*, *>
                        invalidateOptionsMenu()
                        //如果只有一个Library,隐藏标签栏
                        if (libraries.size == 1) {
                            tabs.visibility = View.GONE
                        } else {
                            tabs.visibility = View.VISIBLE
                        }
                    }
                }*/
            }

            Crop.REQUEST_CROP, Crop.REQUEST_PICK -> {
                val intent = intent

                val customCover = intent.getParcelableExtra<CustomCover>(EXTRA_COVER) ?: return
                val errorTxt = getString(when (customCover.type) {
                    Constants.ALBUM -> R.string.set_album_cover_error
                    Constants.ARTIST -> R.string.set_artist_cover_error
                    else -> R.string.set_playlist_cover_error
                })
                val id = customCover.model.getKey().toLong() //专辑、艺术家、播放列表封面

                if (resultCode != Activity.RESULT_OK) {
                    ToastUtil.show(this, errorTxt)
                    return
                }

                if (requestCode == Crop.REQUEST_PICK) {
                    //选择图片
                    val cacheDir = DiskCache.getDiskCacheDir(this, "thumbnail/" + when (customCover.type) {
                        Constants.ALBUM -> "album"
                        Constants.ARTIST -> "artist"
                        else -> "playlist"
                    })
                    if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                        ToastUtil.show(this, errorTxt)
                        return
                    }
                    val destination = Uri.fromFile(File(cacheDir, hashKeyForDisk(id.toString() + "") + ".jpg"))
                    Crop.of(data?.data, destination).asSquare().start(this)
                } else {
                    //图片裁剪
                    //裁剪后的图片路径
                    if (data == null) {
                        return
                    }
                    if (Crop.getOutput(data) == null) {
                        return
                    }

                    val path = Crop.getOutput(data).encodedPath
                    if (TextUtils.isEmpty(path) || id == -1L) {
                        ToastUtil.show(this, errorTxt)
                        return
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        when (customCover.type) {
                            Constants.ALBUM -> UriFetcher.updateAlbumVersion()
                            Constants.ARTIST -> UriFetcher.updateArtistVersion()
                            else -> UriFetcher.updatePlayListVersion()
                        }
                        UriFetcher.clearAllCache()
                        GlideApp.get(this).clearMemory()
                        onMediaStoreChanged()
                        handler.sendEmptyMessage(MSG_UPDATE_ADAPTER)
                    }, 500)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(navigation_view)) {
            drawer.closeDrawer(navigation_view)
        } else {
            var closed = false
            for (fragment in supportFragmentManager.fragments) {
                if (fragment is LibraryFragment<*, *>) {
                    val choice = fragment.multiChoice
                    if (choice.isActive) {
                        closed = true
                        choice.close()
                        break
                    }
                }
            }
            if (!closed) {
                super.onBackPressed()
            }
            //            Intent intent = new Intent();
            //            intent.setAction(Intent.ACTION_MAIN);
            //            intent.addCategory(Intent.CATEGORY_HOME);
            //            startActivity(intent);
        }
    }

    override fun onMetaChanged() {
        super.onMetaChanged()
        val currentSong = MusicServiceRemote.getCurrentSong()
        if (currentSong != Song.EMPTY_SONG) {
            tv_header.text = getString(R.string.play_now, currentSong.title)
            GlideApp.with(this).load(currentSong).centerCrop().signature(ObjectKey(UriFetcher.albumVersion)).placeholder(Theme.resolveDrawable(this, R.attr.default_album)).error(Theme.resolveDrawable(this, R.attr.default_album)).into(iv_header)
        }
    }

    override fun onPlayStateChange() {
        super.onPlayStateChange()
        iv_header.setBackgroundResource(if (MusicServiceRemote.isPlaying() && ThemeStore.isLightTheme) R.drawable.drawer_bg_album_shadow
        else R.color.transparent)
    }

    override fun onServiceConnected(service: MusicService) {
        super.onServiceConnected(service)
        handler.postDelayed({ this.parseIntent() }, 500)
        handler.post {
            onMetaChanged()
        }
    }

    @OnHandleMessage
    fun handleInternal(msg: Message) {
        when (msg.what) {
            MSG_RECREATE_ACTIVITY -> recreate()
            MSG_RESET_MULTI -> for (temp in supportFragmentManager.fragments) {
                if (temp is LibraryFragment<*, *>) {
                    temp.adapter.notifyDataSetChanged()
                }
            }

            MSG_UPDATE_ADAPTER -> //刷新适配器
                for (temp in supportFragmentManager.fragments) {
                    if (temp is LibraryFragment<*, *>) {
                        temp.adapter.notifyDataSetChanged()
                    }
                }
        }
    }

    /**
     * 解析外部打开Intent
     */
    private fun parseIntent() {
        if (intent == null) {
            return
        }
        val intent = intent
        val uri = intent.data
        if (uri != null && uri.toString().isNotEmpty()) {
            MusicUtil.playFromUri(uri)
            setIntent(Intent())
        }
    }

    fun toPlayerActivity() {
        val bottomActionBarFragment = supportFragmentManager.findFragmentByTag("BottomActionBarFragment") as BottomActionBarFragment?
        bottomActionBarFragment?.startPlayerActivity()
    }

    /**
     * 显示popupWindow
     */
    private fun showPopwindow() {


    }

    companion object {
        const val EXTRA_RECREATE = "extra_needRecreate"
        const val EXTRA_REFRESH_ADAPTER = "extra_needRefreshAdapter"
        const val EXTRA_REFRESH_LIBRARY = "extra_needRefreshLibrary"
        const val EXTRA_LIBRARY = "extra_library"

        //设置界面
        private const val REQUEST_SETTING = 1

        //安装权限
        private const val REQUEST_INSTALL_PACKAGES = 2

        private val IMAGE_SIZE = DensityUtil.dip2px(App.context, 108f)

    }
}

