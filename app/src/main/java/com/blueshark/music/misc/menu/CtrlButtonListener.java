package com.blueshark.music.misc.menu;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import com.blueshark.music.R;
import com.blueshark.music.service.Command;
import com.blueshark.music.service.MusicService;
import com.blueshark.music.util.Util;

/**
 * Created by Remix on 2015/12/3.
 */

/**
 * 播放控制
 */
public class CtrlButtonListener implements View.OnClickListener {

  private Context context;

  public CtrlButtonListener(Context context) {
    this.context = context;
  }

  @Override
  public void onClick(View v) {
    Intent intent = new Intent(MusicService.ACTION_CMD);
    switch (v.getId()) {
      case R.id.lockscreen_prev:
      case R.id.playbar_prev:
        intent.putExtra("Control", Command.PREV);
        break;
      case R.id.lockscreen_next:
      case R.id.playbar_next:
        intent.putExtra("Control", Command.NEXT);
        break;
      case R.id.lockscreen_play:
      case R.id.playbar_play:
        intent.putExtra("Control", Command.TOGGLE);
        break;
    }
    Util.sendLocalBroadcast(intent);
  }
}
