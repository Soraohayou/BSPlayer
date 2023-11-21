package com.blueshark.music.db.room

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import android.content.Context
import android.content.Intent
import com.blueshark.music.db.room.AppDatabase.Companion.VERSION
import com.blueshark.music.db.room.dao.HistoryDao
import com.blueshark.music.db.room.dao.PlayListDao
import com.blueshark.music.db.room.dao.PlayQueueDao
import com.blueshark.music.db.room.model.History
import com.blueshark.music.db.room.model.PlayList
import com.blueshark.music.db.room.model.PlayQueue
import com.blueshark.music.service.MusicService
import com.blueshark.music.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PLAYLIST
import com.blueshark.music.util.Util.sendLocalBroadcast
import timber.log.Timber

/**
 * Created by remix on 2019/1/12
 */
@Database(entities = [
  PlayList::class,
  PlayQueue::class,
  History::class
], version = VERSION, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

  abstract fun playListDao(): PlayListDao

  abstract fun playQueueDao(): PlayQueueDao

  abstract fun historyDao(): HistoryDao

  companion object {
    const val VERSION = 3

    @Volatile
    private var INSTANCE: AppDatabase? = null

    @JvmStatic
    fun getInstance(context: Context): AppDatabase =
        INSTANCE ?: synchronized(this) {
          INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

    private fun buildDatabase(context: Context): AppDatabase {
      val migration1to3 = object : Migration(1, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
        }

      }
      val database = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "bsplayer.db")
          .addMigrations(migration1to3)
          .build()
      database.invalidationTracker.addObserver(object : InvalidationTracker.Observer(PlayList.TABLE_NAME, PlayQueue.TABLE_NAME) {
        override fun onInvalidated(tables: MutableSet<String>) {
          Timber.v("onInvalidated: $tables")
          if (tables.contains(PlayList.TABLE_NAME)) {
            sendLocalBroadcast(Intent(MusicService.PLAYLIST_CHANGE)
                .putExtra(EXTRA_PLAYLIST, PlayList.TABLE_NAME))
          } else if (tables.contains(PlayQueue.TABLE_NAME)) {
            sendLocalBroadcast(Intent(MusicService.PLAYLIST_CHANGE)
                .putExtra(EXTRA_PLAYLIST, PlayQueue.TABLE_NAME))
          }
        }
      })
      return database
    }

  }
}
