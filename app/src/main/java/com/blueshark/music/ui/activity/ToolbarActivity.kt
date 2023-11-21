package com.blueshark.music.ui.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar
import com.blueshark.music.R
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.theme.ThemeStore.materialPrimaryColor
import com.blueshark.music.theme.ToolbarContentTintHelper
import com.blueshark.music.ui.activity.base.BaseMusicActivity
import com.blueshark.music.util.SPUtil

/**
 * Created by taeja on 16-3-15.
 */
@SuppressLint("Registered")
open class ToolbarActivity : BaseMusicActivity() {

    protected var toolbar: Toolbar? = null
    protected var toolbar_title: TextView? = null

    protected fun setUpToolbar(title: String?, @DrawableRes iconRes: Int) {
        if (toolbar == null) {
            toolbar = findViewById(R.id.toolbar)
        }

        if (toolbar_title == null) {
            toolbar_title = findViewById(R.id.toolbar_title)
        }

        toolbar?.title = ""
        toolbar_title?.text = title
        setSupportActionBar(toolbar)

        if (materialPrimaryColor == 0) {
            materialPrimaryColor = Color.BLACK
            SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DARK_THEME, ThemeStore.ALWAYS_ON)
            recreate()
        }

        toolbar?.setBackgroundColor(materialPrimaryColor)
        toolbar?.setNavigationIcon(iconRes)
        toolbar?.setNavigationOnClickListener { v: View? -> onClickNavigation() }
    }

    fun setUpToolbar(title: String?) {
        setUpToolbar(title, R.drawable.bg_back)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val toolbar = toolbar
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(this, toolbar, menu, getToolbarBackgroundColor(toolbar))
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(this, toolbar)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        this.toolbar = toolbar
        super.setSupportActionBar(toolbar)
    }

    protected open fun onClickNavigation() {
        finish()
    }

    companion object {
        fun getToolbarBackgroundColor(toolbar: Toolbar?): Int {
            return if (toolbar != null && toolbar.background is ColorDrawable) (toolbar.background as ColorDrawable).color else 0
        }
    }
}