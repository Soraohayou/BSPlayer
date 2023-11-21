package com.blueshark.music.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.PopupWindow
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.blueshark.music.App
import com.blueshark.music.R
import com.blueshark.music.bean.misc.Library
import com.blueshark.music.db.room.DatabaseRepository
import com.blueshark.music.db.room.model.PlayList
import com.blueshark.music.misc.handler.MsgHandler
import com.blueshark.music.service.MusicService
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.ui.activity.LancherMain
import com.blueshark.music.ui.activity.MeActivity
import com.blueshark.music.ui.activity.SearchActivity
import com.blueshark.music.ui.activity.SongChooseActivity
import com.blueshark.music.ui.activity.TypeActivty
import com.blueshark.music.ui.adapter.DrawerAdapter
import com.blueshark.music.ui.adapter.MainPagerAdapter
import com.blueshark.music.ui.fragment.base.BaseMusicFragment
import com.blueshark.music.ui.misc.MultipleChoice
import com.blueshark.music.util.DensityUtil
import com.blueshark.music.util.RxUtil
import com.blueshark.music.util.SPUtil
import com.blueshark.music.util.ToastUtil
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringSystem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_home.btn_add
import kotlinx.android.synthetic.main.fragment_home.layout1
import kotlinx.android.synthetic.main.fragment_home.main_tabs_album
import kotlinx.android.synthetic.main.fragment_home.main_tabs_artist
import kotlinx.android.synthetic.main.fragment_home.main_tabs_folder
import kotlinx.android.synthetic.main.fragment_home.main_tabs_music
import kotlinx.android.synthetic.main.fragment_home.main_tabs_playlist
import kotlinx.android.synthetic.main.fragment_home.top_search
import kotlinx.android.synthetic.main.fragment_home.view_pager
import kotlinx.android.synthetic.main.toolbar.toolbar

/**
 *
 */
class HomeFragment : BaseMusicFragment(), View.OnClickListener {

    private val drawerAdapter by lazy {
        DrawerAdapter(R.layout.item_drawer)
    }
    private val pagerAdapter by lazy {
        MainPagerAdapter(childFragmentManager)
    }

    private val handler by lazy {
        MsgHandler(this)
    }

    private var popupWindow: PopupWindow? = null
    private var popContentView: View? = null

    var adapter: BaseAdapter? = null

    //当前选中的fragment
    private var currentFragment: BaseMusicFragment? = null

    private var menuLayoutId = R.menu.menu_main

    /**
     * 判断安卓版本，请求安装权限或者直接安装
     *
     * @param activity
     * @param path
     */
    private var installPath: String? = null


    private var forceDialog: MaterialDialog? = null

    var hasNewIntent:Boolean = false;

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //初始化控件
        setUpToolbar()
        setUpPager()
        setUpTab()
        setUpSearch()
        btn_add.setOnClickListener(this)
        //初始化测滑菜单
        //清除多选显示状态
        MultipleChoice.isActiveSomeWhere = false
    }

    /**
     * 初始化toolbar
     */
    private fun setUpToolbar() {
        (activity as LancherMain).setUpToolbar("")
        toolbar?.setNavigationIcon(R.drawable.ic_menu_white_24dp)

        toolbar?.setNavigationOnClickListener { v ->
            run {
                startActivityForResult(Intent(activity, MeActivity::class.java), REQUEST_SETTING)
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

                DatabaseRepository.getInstance()
                    .getAllPlaylist().compose<List<PlayList>>(RxUtil.applySingleScheduler<List<PlayList>>()).subscribe { playLists ->
                    Theme.getBaseDialog(activity)
                        .title(R.string.new_playlist).positiveText(R.string.create).negativeText(R.string.cancel).inputRange(1, 25).input("", getString(
                        R.string.local_list
                    ) + playLists.size) { dialog, input ->
                        if (!TextUtils.isEmpty(input)) {
                            DatabaseRepository.getInstance()
                                .insertPlayList(input.toString()).compose(RxUtil.applySingleScheduler()).subscribe({ id ->
                                //跳转到添加歌曲界面
                                    activity?.let { SongChooseActivity.start(it, id, input.toString()) }
                            }, { throwable ->
                                ToastUtil.show(
                                    activity,
                                    R.string.create_playlist_fail,
                                    throwable.toString()
                                )
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
        val libraryJson =
            SPUtil.getValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY, "")
        val libraries = if (TextUtils.isEmpty(libraryJson)) ArrayList()
        else Gson().fromJson<ArrayList<Library>>(libraryJson, object : TypeToken<List<Library>>() {}.type)
        if (libraries.isEmpty()) {
            val defaultLibraries = Library.getDefaultLibrary()
            libraries.addAll(defaultLibraries)
            SPUtil.putValue(
                activity,
                SPUtil.SETTING_KEY.NAME,
                SPUtil.SETTING_KEY.LIBRARY,
                Gson().toJson(defaultLibraries, object : TypeToken<List<Library>>() {}.type)
            )
        }

        pagerAdapter.list = libraries;
        menuLayoutId = parseMenuId(pagerAdapter.list[0].tag)
        //有且仅有一个tab
//        if (libraries.size == 1) {
//            if (libraries[0].isPlayList()) {
//                showViewWithAnim(btn_add, true)
//            }
//            tabs.visibility = View.GONE
//        } else {
//            tabs.visibility = View.VISIBLE
//        }

        view_pager.adapter = pagerAdapter
        view_pager.offscreenPageLimit = pagerAdapter.count - 1
        view_pager.currentItem = 0
        view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                val library = pagerAdapter.list[position]
                showViewWithAnim(btn_add, library.isPlayList())
                
                    menuLayoutId = parseMenuId(pagerAdapter.list[position].tag)
                    currentFragment = pagerAdapter.getFragment(position) as BaseMusicFragment

            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        currentFragment = pagerAdapter.getFragment(0) as BaseMusicFragment
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
            val intent = Intent(activity, TypeActivty::class.java);
            intent.putExtra("tab_name", R.string.tab_song);
            intent.putExtra("tab_target", Library.TAG_SONG)
            startActivity(intent)
        }

        main_tabs_album.setOnClickListener {
            val intent = Intent(activity, TypeActivty::class.java);
            intent.putExtra("tab_name", R.string.tab_album);
            intent.putExtra("tab_target", Library.TAG_ALBUM)
            startActivity(intent)
        }

        main_tabs_artist.setOnClickListener {
            val intent = Intent(activity, TypeActivty::class.java);
            intent.putExtra("tab_name", R.string.tab_artist);
            intent.putExtra("tab_target", Library.TAG_ARTIST)
            startActivity(intent)
        }

        main_tabs_playlist.setOnClickListener {
            val intent = Intent(activity, TypeActivty::class.java);
            intent.putExtra("tab_name", R.string.tab_playlist);
            intent.putExtra("tab_target", Library.TAG_PLAYLIST)
            startActivity(intent)
        }

        main_tabs_folder.setOnClickListener {
            val intent = Intent(activity, TypeActivty::class.java);
            intent.putExtra("tab_name", R.string.tab_folder);
            intent.putExtra("tab_target", Library.TAG_FOLDER)
            startActivity(intent)
        }

    }

    private fun setUpSearch() {
        top_search.setOnClickListener() {
            startActivity(Intent(activity, SearchActivity::class.java))
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

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        onMetaChanged()
        //    mRefreshHandler.sendEmptyMessage(MSG_UPDATE_ADAPTER);
    }

    override fun onServiceConnected(service: MusicService) {
        super.onServiceConnected(service)
//        handler.postDelayed({ this.parseIntent() }, 500)
//        handler.post {
//            onMetaChanged()
//        }
    }


    fun toPlayerActivity() {
        val bottomActionBarFragment = childFragmentManager.findFragmentByTag("BottomActionBarFragment") as BottomActionBarFragment?
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