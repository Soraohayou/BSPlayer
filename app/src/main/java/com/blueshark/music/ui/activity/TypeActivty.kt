package com.blueshark.music.ui.activity

import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import com.blueshark.music.R
import com.blueshark.music.bean.misc.Library
import com.blueshark.music.databinding.ActivityTabBinding
import com.blueshark.music.databinding.ActivityTypeBinding
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.ui.fragment.AlbumFragment
import com.blueshark.music.ui.fragment.ArtistFragment
import com.blueshark.music.ui.fragment.FolderFragment
import com.blueshark.music.ui.fragment.PlayListFragment
import com.blueshark.music.ui.fragment.SongFragment
import com.blueshark.music.util.SPUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_setting.root

class TypeActivty : MenuActivity() {

    private var menuLayoutId = R.menu.menu_tab

    private lateinit var binding: ActivityTypeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_type)

        binding = ActivityTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpToolbar(getString(intent.getIntExtra("tab_name", 0)))

        val libraryJson = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY, "")
        val libraries = if (TextUtils.isEmpty(libraryJson)) ArrayList()
        else Gson().fromJson<ArrayList<Library>>(libraryJson, object : TypeToken<List<Library>>() {}.type)
        if (libraries.isEmpty()) {
            val defaultLibraries = Library.getDefaultLibrary()
            libraries.addAll(defaultLibraries)
            SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY, Gson().toJson(defaultLibraries, object : TypeToken<List<Library>>() {}.type))
        }

        val beginTransaction = supportFragmentManager.beginTransaction();
        val tag = intent.getIntExtra("tab_target", 0);
        val fragment: Fragment = if (tag == Library.TAG_SONG) SongFragment() else if (tag == Library.TAG_ALBUM) AlbumFragment() else if (tag == Library.TAG_ARTIST) ArtistFragment() else if (tag == Library.TAG_PLAYLIST) PlayListFragment() else FolderFragment()
        beginTransaction.replace(R.id.content, fragment);
        beginTransaction.commit();

    }

    override fun getMenuLayoutId(): Int {
        return menuLayoutId
    }

}