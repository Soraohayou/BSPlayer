package com.blueshark.music.appshortcuts

import android.annotation.TargetApi
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import com.blueshark.music.BuildConfig
import com.blueshark.music.appshortcuts.shortcuttype.BaseShortcutType.Companion.ID_PREFIX
import com.blueshark.music.appshortcuts.shortcuttype.LastAddedShortcutType
import com.blueshark.music.appshortcuts.shortcuttype.MyLoveShortcutType
import com.blueshark.music.appshortcuts.shortcuttype.ShuffleShortcutType

/**
 * Created by Remix on 2017/11/1.
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
class DynamicShortcutManager(private val context: Context) : ContextWrapper(context.applicationContext) {
  private var shortcutManger: ShortcutManager? = null

  private val defaultShortcut: List<ShortcutInfo>
    get() = listOf(ShuffleShortcutType(context).shortcutInfo,
        MyLoveShortcutType(context).shortcutInfo,
        LastAddedShortcutType(context).shortcutInfo)

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
      shortcutManger = getSystemService(ShortcutManager::class.java)
  }

  fun setUpShortcut() {
    shortcutManger?.removeDynamicShortcuts(listOf(ID_PREFIX + "continue_play"))
    val shortcuts = shortcutManger?.dynamicShortcuts
    if (BuildConfig.DEBUG) {
      //debug模式下有leakCanary
      if (shortcuts?.size == 0 || shortcuts?.get(0)?.id == "com.squareup.leakcanary.dynamic_shortcut") {
        shortcutManger?.addDynamicShortcuts(defaultShortcut)
      }
    } else {
      if (shortcuts?.size == 0) {
        shortcutManger?.addDynamicShortcuts(defaultShortcut)
      }
    }
  }

//  fun updateContinueShortcut(service: MusicService) {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
//      shortcutManger?.updateShortcuts(listOf(ContinuePlayShortcutType(service).shortcutInfo))
//  }
}
