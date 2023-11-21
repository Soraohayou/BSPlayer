package com.blueshark.music.bean.mp3

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by Remix on 2018/1/9.
 */
@Parcelize
data class Folder(val name: String?,
                  val count: Int,
                  val path: String) : Parcelable, BSPlayerModel {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Folder

    if (path != other.path) return false

    return true
  }

  override fun hashCode(): Int {
    return path.hashCode()
  }

  override fun getKey(): String {
    return path
  }
}
