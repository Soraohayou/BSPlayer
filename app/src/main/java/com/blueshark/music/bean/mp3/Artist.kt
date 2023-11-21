package com.blueshark.music.bean.mp3

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by Remix on 2017/10/22.
 */

@Parcelize
data class Artist(val artistID: Long,
                  val artist: String,
                  var count: Int) : Parcelable, BSPlayerModel {

  override fun getKey(): String {
    return artistID.toString()
  }
}
