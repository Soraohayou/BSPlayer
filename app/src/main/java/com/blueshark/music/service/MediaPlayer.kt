package com.blueshark.music.service

import tv.danmaku.ijk.media.player.MediaPlayerProxy
import tv.danmaku.ijk.media.player.IjkMediaPlayer

class MediaPlayer : MediaPlayerProxy(IjkMediaPlayer()) {
    fun setOption(category: Int, name: String?, value: Long) {
        (internalMediaPlayer as IjkMediaPlayer).setOption(category, name, value)
    }
    fun setSpeed(speed: Float) {
        (internalMediaPlayer as IjkMediaPlayer).setSpeed(speed)
    }
}