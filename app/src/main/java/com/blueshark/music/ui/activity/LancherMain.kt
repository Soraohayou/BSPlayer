package com.blueshark.music.ui.activity

import android.os.Bundle
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.blueshark.music.R
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.ui.activity.MenuActivity
import com.blueshark.music.ui.adapter.PagerAdapter
import com.blueshark.music.ui.fragment.AlbumFragment
import com.blueshark.music.ui.fragment.ArtistFragment
import com.blueshark.music.ui.fragment.BottomActionBarFragment
import com.blueshark.music.ui.fragment.HomeFragment
import com.blueshark.music.ui.fragment.MyFragment
import com.blueshark.music.ui.fragment.PlayerFragment
import com.blueshark.music.ui.fragment.SongFragment
import com.blueshark.music.util.StatusBarUtil
import kotlinx.android.synthetic.main.lancher_main.bottom_action_bar
import kotlinx.android.synthetic.main.lancher_main.view_pager

class LancherMain : MenuActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lancher_main)

        var adapter = PagerAdapter(supportFragmentManager)
        adapter.addFragment(HomeFragment())
        adapter.addFragment(PlayerFragment())
        adapter.addFragment(MyFragment())

        view_pager.adapter = adapter
        // view_pager.adapter =
        view_pager.setCurrentItem(0)

//        bottom_action_bar.setOnNavigationItemSelectedListener {
//            when(it.itemId){
//                R.id.navigation_home->view_pager.setCurrentItem(0)
//                R.id.navigation_dashboard->view_pager.setCurrentItem(1)
//                R.id.navigation_notifications->view_pager.setCurrentItem(2)
//            }
//            true
//        }

        bottom_action_bar.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> view_pager.setCurrentItem(0)
                R.id.navigation_dashboard -> view_pager.setCurrentItem(1)
                R.id.navigation_notifications -> view_pager.setCurrentItem(2)
            }
            true
        }

        view_pager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, positionOffsetPixels: Int
            ) {
            }
            override fun onPageSelected(position: Int) {
                bottom_action_bar.menu.getItem(position).isChecked = true
            }
            override fun onPageScrollStateChanged(state: Int) {

            }
        })

    }

    fun toPlayerActivity() {
        val bottomActionBarFragment =
            supportFragmentManager.findFragmentByTag("BottomActionBarFragment") as BottomActionBarFragment?
        bottomActionBarFragment?.startPlayerActivity()
    }

    override fun setStatusBarColor() {
        StatusBarUtil.setColor(this, ThemeStore.statusBarColor)
//        StatusBarUtil.setColorNoTranslucentForDrawerLayout(
//            this,
//            findViewById(R.id.drawer),
//            ThemeStore.statusBarColor
//        )
    }


}