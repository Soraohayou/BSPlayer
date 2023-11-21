package com.blueshark.music.db.room.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Created by remix on 2019/1/12
 */
@Entity(indices = [Index(value = ["audio_id"], unique = true)])
data class PlayQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val audio_id: Long
) {

  companion object {
    const val TABLE_NAME = "PlayQueue"
  }
}