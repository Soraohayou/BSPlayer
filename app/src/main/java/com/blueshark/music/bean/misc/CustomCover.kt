package com.blueshark.music.bean.misc

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import com.blueshark.music.bean.mp3.BSPlayerModel

@Parcelize
data class CustomCover(
    val model: BSPlayerModel,
    val type: Int) : Parcelable