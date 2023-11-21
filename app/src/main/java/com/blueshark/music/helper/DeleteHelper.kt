package com.blueshark.music.helper

import io.reactivex.Single
import com.blueshark.music.db.room.DatabaseRepository
import com.blueshark.music.ui.activity.base.BaseActivity
import com.blueshark.music.util.RxUtil.applySingleScheduler
import com.blueshark.music.util.MediaStoreUtil

object DeleteHelper {
  /**
   * 在曲库列表删除歌曲
   */
  @JvmStatic
  fun deleteSongs(activity: BaseActivity, songIds: List<Long>, deleteSource: Boolean, playlistId: Long = -1, deletePlaylist: Boolean = false):
      Single<Boolean> {
    return Single
        .fromCallable {
          return@fromCallable if (deletePlaylist) {
            if (deleteSource) {
              MediaStoreUtil.delete(activity, MediaStoreUtil.getSongsByIds(songIds), deleteSource)
            } else {
              DatabaseRepository.getInstance()
                  .deletePlayList(playlistId).blockingGet()
            }
          } else {
            MediaStoreUtil.delete(activity, MediaStoreUtil.getSongsByIds(songIds), deleteSource)
          }

        }
        .map {
          it > 0
        }

  }

  /**
   * 在列表内(如专辑、艺术家列表内删除歌曲)
   */
  @JvmStatic
  fun deleteSong(activity: BaseActivity, songId: Long, deleteSource: Boolean, deleteFromPlayList: Boolean = false, playListName: String = ""):
      Single<Boolean> {
    return Single
        .fromCallable {
          if (!deleteFromPlayList)
            MediaStoreUtil
                .delete(activity, listOf(MediaStoreUtil.getSongById(songId)), deleteSource)
          else
            DatabaseRepository.getInstance()
                .deleteFromPlayList(arrayListOf(songId), playListName)
                .blockingGet()
        }
        .map {
          it > 0
        }
        .compose(applySingleScheduler())
  }
}