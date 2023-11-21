package com.blueshark.music.appwidgets.small

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.blueshark.music.R
import com.blueshark.music.appwidgets.AppWidgetSkin
import com.blueshark.music.appwidgets.BaseAppwidget
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.service.MusicService
import com.blueshark.music.util.Util

class AppWidgetSmallTransparent : BaseAppwidget() {

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    super.onUpdate(context,appWidgetManager,appWidgetIds)
    defaultAppWidget(context, appWidgetIds)
    val intent = Intent(MusicService.ACTION_WIDGET_UPDATE)
    intent.putExtra(EXTRA_WIDGET_NAME, this.javaClass.simpleName)
    intent.putExtra(EXTRA_WIDGET_IDS, appWidgetIds)
    Util.sendLocalBroadcast(intent)
  }

  private fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
    val remoteViews = RemoteViews(context.packageName, R.layout.app_widget_small_transparent)
    buildAction(context, remoteViews)
    pushUpdate(context, appWidgetIds, remoteViews)
  }

  override fun updateWidget(service: MusicService, appWidgetIds: IntArray?, reloadCover: Boolean) {
    val song = service.currentSong
    if(song == Song.EMPTY_SONG){
      return
    }
    if(!hasInstances(service)){
      return
    }
    val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_small_transparent)
    buildAction(service, remoteViews)
    skin = AppWidgetSkin.TRANSPARENT
    updateRemoteViews(service, remoteViews, song)
    //设置封面
    updateCover(service, remoteViews, appWidgetIds, reloadCover)
  }

  override fun partiallyUpdateWidget(service: MusicService) {
    val song = service.currentSong
    if(song == Song.EMPTY_SONG){
      return
    }
    if(!hasInstances(service)){
      return
    }
    val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_small_transparent)
    buildAction(service, remoteViews)
    skin = AppWidgetSkin.TRANSPARENT
    updateRemoteViews(service, remoteViews, song)

    val appIds = AppWidgetManager.getInstance(service).getAppWidgetIds(ComponentName(service, javaClass))
    pushPartiallyUpdate(service,appIds,remoteViews)
  }

  companion object {
    @Volatile
    private var INSTANCE: AppWidgetSmallTransparent? = null

    @JvmStatic
    fun getInstance(): AppWidgetSmallTransparent =
        INSTANCE ?: synchronized(this) {
          INSTANCE ?: AppWidgetSmallTransparent()
        }
  }
}