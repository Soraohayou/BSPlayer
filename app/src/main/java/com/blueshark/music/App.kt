package com.blueshark.music

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Process
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
//import com.tencent.bugly.crashreport.CrashReport
//import com.tencent.bugly.crashreport.CrashReport.UserStrategy
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import com.blueshark.music.appshortcuts.DynamicShortcutManager
import com.blueshark.music.helper.LanguageHelper.onConfigurationChanged
import com.blueshark.music.helper.LanguageHelper.saveSystemCurrentLanguage
import com.blueshark.music.helper.LanguageHelper.setApplicationLanguage
import com.blueshark.music.helper.LanguageHelper.setLocal
import com.blueshark.music.misc.cache.DiskCache
import com.blueshark.music.misc.manager.BSPlayerActivityManager
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.util.SPUtil
import com.blueshark.music.util.SPUtil.SETTING_KEY
import com.blueshark.music.util.Util
import timber.log.Timber

/**
 * Created by Remix on 16-3-16.
 */
class App : MultiDexApplication() {

  override fun attachBaseContext(base: Context) {
    saveSystemCurrentLanguage()
    super.attachBaseContext(setLocal(base))
    MultiDex.install(this)
  }

  override fun onCreate() {
    super.onCreate()
    context = this
    checkMigration()
    setUp()

    // AppShortcut
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      DynamicShortcutManager(this).setUpShortcut()
    }

    // 处理 RxJava2 取消订阅后，抛出的异常无法捕获，导致程序崩溃
    RxJavaPlugins.setErrorHandler { throwable: Throwable? ->
      Timber.v(throwable)
//      CrashReport.postCatchedException(throwable)
    }
    registerActivityLifecycleCallbacks(BSPlayerActivityManager())
  }

  private fun checkMigration() {
    if (!SPUtil.getValue(context, SPUtil.LYRIC_KEY.NAME, SPUtil.LYRIC_KEY.LYRIC_RESET_ON_16000, false)) {
      SPUtil.deleteFile(this, SPUtil.LYRIC_KEY.NAME)
      SPUtil.putValue(context, SPUtil.LYRIC_KEY.NAME, SPUtil.LYRIC_KEY.LYRIC_RESET_ON_16000, true)
      SPUtil.putValue(context, SPUtil.LYRIC_KEY.NAME, SPUtil.LYRIC_KEY.PRIORITY_LYRIC, SPUtil.LYRIC_KEY.DEFAULT_PRIORITY)
      try {
        DiskCache.getLrcDiskCache().delete()
      } catch (e: Exception) {
        Timber.v(e)
      }
    }
  }

  private fun setUp() {
    DiskCache.init(this, "lyric")
    setApplicationLanguage(this)
    Completable
        .fromAction {
          ThemeStore.sImmersiveMode = SPUtil
              .getValue(context, SETTING_KEY.NAME, SETTING_KEY.IMMERSIVE_MODE, true)
          ThemeStore.sColoredNavigation = SPUtil.getValue(context, SETTING_KEY.NAME,
              SETTING_KEY.COLOR_NAVIGATION, false)
        }
        .subscribe()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    onConfigurationChanged(applicationContext)
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    Timber.v("onTrimMemory, %s", level)
    Completable
        .fromAction {
          when (level) {
            TRIM_MEMORY_UI_HIDDEN -> {
            }
            TRIM_MEMORY_RUNNING_MODERATE, TRIM_MEMORY_RUNNING_LOW, TRIM_MEMORY_RUNNING_CRITICAL -> {
            }
            TRIM_MEMORY_BACKGROUND, TRIM_MEMORY_MODERATE, TRIM_MEMORY_COMPLETE -> {
            }
            else -> {
            }
          }
        }
        .subscribeOn(AndroidSchedulers.mainThread())
        .subscribe()
  }

  companion object {
    @JvmStatic
    lateinit var context: App
      private set
  }
}