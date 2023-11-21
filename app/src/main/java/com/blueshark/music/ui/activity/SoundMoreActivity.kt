package com.blueshark.music.ui.activity

import android.os.Bundle
import android.view.View
import com.blueshark.music.databinding.ActivityCustomizeSoundBinding
import com.blueshark.music.databinding.ActivitySoundMoreBinding
import kotlinx.android.synthetic.main.activity_customize_sound.*
import kotlinx.android.synthetic.main.activity_customize_sound.sound_1
import kotlinx.android.synthetic.main.activity_sound_more.*

class SoundMoreActivity : ToolbarActivity() {

    private lateinit var binding: ActivitySoundMoreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySoundMoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpToolbar("更多")

        sound_7.setOnClickListener({
            image1.visibility = View.VISIBLE
            image2.visibility = View.GONE
            image3.visibility = View.GONE
            image4.visibility = View.GONE
            image5.visibility = View.GONE
            image6.visibility = View.GONE
        })

        sound_8.setOnClickListener({
            image1.visibility = View.GONE
            image2.visibility = View.VISIBLE
            image3.visibility = View.GONE
            image4.visibility = View.GONE
            image5.visibility = View.GONE
            image6.visibility = View.GONE
        })

        sound_9.setOnClickListener({
            image1.visibility = View.GONE
            image2.visibility = View.GONE
            image3.visibility = View.VISIBLE
            image4.visibility = View.GONE
            image5.visibility = View.GONE
            image6.visibility = View.GONE
        })

        sound_4.setOnClickListener({
            image1.visibility = View.GONE
            image2.visibility = View.GONE
            image3.visibility = View.GONE
            image4.visibility = View.VISIBLE
            image5.visibility = View.GONE
            image6.visibility = View.GONE
        })

        sound_5.setOnClickListener({
            image1.visibility = View.GONE
            image2.visibility = View.GONE
            image3.visibility = View.GONE
            image4.visibility = View.GONE
            image5.visibility = View.VISIBLE
            image6.visibility = View.GONE
        })

        sound_6.setOnClickListener({
            image1.visibility = View.GONE
            image2.visibility = View.GONE
            image3.visibility = View.GONE
            image4.visibility = View.GONE
            image5.visibility = View.GONE
            image6.visibility = View.VISIBLE
        })

    }

}