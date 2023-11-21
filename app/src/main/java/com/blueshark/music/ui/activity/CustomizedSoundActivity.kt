package com.blueshark.music.ui.activity

import android.graphics.Color
import android.os.Bundle
import com.blueshark.music.databinding.ActivityCustomizeSoundBinding
import com.blueshark.music.helper.EQHelper
import kotlinx.android.synthetic.main.activity_customize_sound.*

class CustomizedSoundActivity : ToolbarActivity() {

    private lateinit var binding: ActivityCustomizeSoundBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCustomizeSoundBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpToolbar("人耳定制声音")

        text1.setTextColor(Color.parseColor("#727272"))
        text2.setTextColor(Color.parseColor("#727272"))
        text3.setTextColor(Color.parseColor("#727272"))
        text4.setTextColor(Color.parseColor("#727272"))
        sound_1.setOnClickListener(null)
        sound_2.setOnClickListener(null)
        sound_3.setOnClickListener(null)
        customer.setOnClickListener(null)

        setting_switch.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                text1.setTextColor(Color.WHITE)
                text2.setTextColor(Color.WHITE)
                text3.setTextColor(Color.WHITE)
                text4.setTextColor(Color.WHITE)
                sound_1.setOnClickListener(null)
                sound_2.setOnClickListener(null)
                sound_3.setOnClickListener(null)
                customer.setOnClickListener({
                    EQHelper.startEqualizer(this@CustomizedSoundActivity)
                }
                )

            } else {
                text1.setTextColor(Color.parseColor("#727272"))
                text2.setTextColor(Color.parseColor("#727272"))
                text3.setTextColor(Color.parseColor("#727272"))
                text4.setTextColor(Color.parseColor("#727272"))
                sound_1.setOnClickListener(null)
                sound_2.setOnClickListener(null)
                sound_3.setOnClickListener(null)
                customer.setOnClickListener(null)
            }
        }
    }

}