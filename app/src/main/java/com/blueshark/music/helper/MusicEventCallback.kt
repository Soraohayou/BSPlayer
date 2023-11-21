package com.blueshark.music.helper

import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.service.MusicService

interface MusicEventCallback {
  fun onMediaStoreChanged()

  fun onPermissionChanged(has: Boolean)

  fun onPlayListChanged(name: String)

  fun onServiceConnected(service: MusicService)

  fun onMetaChanged()

  fun onPlayStateChange()

  fun onServiceDisConnected()

  fun onTagChanged(oldSong: Song, newSong: Song)
}
