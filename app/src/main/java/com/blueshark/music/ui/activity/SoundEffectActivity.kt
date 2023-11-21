package com.blueshark.music.ui.activity

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.blueshark.music.R
import com.blueshark.music.databinding.ActivitySettingBinding
import com.blueshark.music.databinding.ActivitySoundEffectBinding
import com.blueshark.music.helper.EQHelper
import com.blueshark.music.ui.dialog.color.ColorChooserDialog
import kotlinx.android.synthetic.main.activity_sound_effect.*

class SoundEffectActivity : ToolbarActivity(), ColorChooserDialog.ColorCallback,
    SharedPreferences.OnSharedPreferenceChangeListener  {

    private lateinit var binding: ActivitySoundEffectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_effect)

        binding = ActivitySoundEffectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpToolbar(getString(R.string.deep_space_sound_effect))

        initView()

    }

    fun initView(){
        sound_1.setOnClickListener({
            EQHelper.startEqualizer(this@SoundEffectActivity)
        })

        sound_3.setOnClickListener({
            startActivity(Intent(this@SoundEffectActivity, CustomizedSoundActivity::class.java))
        })

        more.setOnClickListener({
            startActivity(Intent(this@SoundEffectActivity, SoundMoreActivity::class.java))
        })

    }

    override fun onColorSelection(dialog: ColorChooserDialog, selectedColor: Int) {

    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {

    }
}