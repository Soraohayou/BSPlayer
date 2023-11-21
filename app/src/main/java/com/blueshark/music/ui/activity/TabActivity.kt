package com.blueshark.music.ui.activity

import android.app.LocalActivityManager
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.blueshark.music.R
import com.blueshark.music.databinding.ActivityTabBinding
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.ui.activity.base.BaseActivity
import kotlinx.android.synthetic.main.activity_setting.root
import kotlinx.android.synthetic.main.activity_tab.frame
import kotlinx.android.synthetic.main.activity_tab.view1
import kotlinx.android.synthetic.main.activity_tab.view2
import kotlinx.android.synthetic.main.activity_tab.view3
import kotlinx.android.synthetic.main.activity_tab.view4
import kotlinx.android.synthetic.main.activity_tab.view5
import kotlinx.android.synthetic.main.activity_tab.view6

class TabActivity  : BaseActivity() {

    private lateinit var binding: ActivityTabBinding

    private lateinit var localActivityManager:LocalActivityManager;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab)

        binding = ActivityTabBinding.inflate(layoutInflater)
        setContentView(binding.root)


        localActivityManager = LocalActivityManager(this,true)
        localActivityManager.dispatchCreate(savedInstanceState)

        binding.layout1.setOnClickListener({
            view1.setImageResource(R.drawable.home_select);
            view2.setTextColor(Color.WHITE)
            view3.setImageResource(R.drawable.play_disselect);
            view4.setTextColor(resources.getColor(R.color.home_disselect))
            view5.setImageResource(R.drawable.me_disselect);
            view6.setTextColor(resources.getColor(R.color.home_disselect))
        })

        binding.layout2.setOnClickListener({
            view1.setImageResource(R.drawable.home_disselect);
            view2.setTextColor(resources.getColor(R.color.home_disselect))
            view3.setImageResource(R.drawable.play_select);
            view4.setTextColor(Color.WHITE)
            view5.setImageResource(R.drawable.me_disselect);
            view6.setTextColor(resources.getColor(R.color.home_disselect))
        })

        binding.layout3.setOnClickListener({
            view1.setImageResource(R.drawable.home_disselect);
            view2.setTextColor(resources.getColor(R.color.home_disselect))
            view3.setImageResource(R.drawable.play_disselect);
            view4.setTextColor(resources.getColor(R.color.home_disselect))
            view5.setImageResource(R.drawable.me_select);
            view6.setTextColor(Color.WHITE)
        })

        binding.root.setBackgroundColor(ThemeStore.materialPrimaryColor)

    }

    override fun onPause() {
        super.onPause()
        localActivityManager.dispatchPause(isFinishing());
    }

    override fun onResume() {
        super.onResume()
        localActivityManager.dispatchResume();
        var intent = Intent(this, launchmain.Companion::class.java);
        var v = localActivityManager.startActivity("one", intent).getDecorView();
        frame.removeAllViews();
        frame.addView(v);
    }

}