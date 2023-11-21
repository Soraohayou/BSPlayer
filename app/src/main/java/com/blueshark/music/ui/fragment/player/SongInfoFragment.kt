package com.blueshark.music.ui.fragment.player

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.ui.fragment.base.BaseMusicFragment
import com.blueshark.music.util.Constants
import com.blueshark.music.util.Util
import com.ijkplay.AudioTaglib.PId3Info
import kotlinx.android.synthetic.main.fragment_tag.*
import tv.danmaku.ijk.media.player.IjkMediaMeta

/**
 * Created by Remix on 2015/12/2.
 */
/**
 * 歌曲信息Fragment
 */
class SongInfoFragment : BaseMusicFragment() {

  var reRunnable: Runnable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pageName = SongInfoFragment::class.java.simpleName
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_tag, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    reRunnable?.run()
    reRunnable = null
  }

  fun setTag(song: Song, meta: IjkMediaMeta?, pid3info: PId3Info?) {
    if (!isAdded || (context as Activity).isFinishing) {
      reRunnable = Runnable {
        setTag(song, meta, pid3info)
      }
      return
    }
    val unknown = context!!.getString(R.string.unknown)
    var builder: StringBuilder = java.lang.StringBuilder()
    builder.append("专辑：" + if (TextUtils.isEmpty(pid3info?.ablum)) unknown else pid3info?.ablum )
            .append("\n年代：" + if (pid3info?.year == 0) unknown else pid3info?.year)
            .append("\n风格：" + if (TextUtils.isEmpty(pid3info?.genre)) unknown else pid3info?.genre)
            .append("\n大小：" + getString(R.string.cache_size, 1.0f * song.size / Constants.MB))
            .append("\n时长：" + Util.getTime(song.duration))
            .append("\n格式：" + meta?.mAudioStream?.mCodecName?.split("_")?.get(0)?.toUpperCase())
            .append("\n采样率：" + if(pid3info!!.samplerate == 0) meta!!.mAudioStream!!.sampleRateInline else pid3info!!.samplerate.toString() + "Hz")
            .append("\n位深：" + (if (pid3info?.bitsPerSample == 0) "1" else pid3info?.bitsPerSample.toString()) + "bit")
            .append("\n比特率：" + (if (pid3info?.bitrate == 0) meta!!.mBitrate/1000 else pid3info?.bitrate) + "Kbps")
            .append("\n声道：" + if (pid3info?.channels == 0) getChannels(meta?.mAudioStream?.mChannelLayout) else pid3info?.channels)
            .append("\n位置：" + song.data)
    title?.setText(builder.toString())
      title.setTextColor(ThemeStore.textColorPrimary);
  }

  fun getChannels(layout:  Long? ): Int {
    if (layout == null) return 0;

    var num = 0;
    var tmp = layout
    while(tmp != 0L) {
      var v: Long = tmp % 2
      if (v == 1L) num++;
      tmp /= 2
    }
    return num
  }

}