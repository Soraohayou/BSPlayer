package com.blueshark.music.ui.misc

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.blueshark.music.R
import com.blueshark.music.theme.Theme
import com.blueshark.music.ui.activity.base.BaseActivity
import com.blueshark.music.ui.dialog.FolderChooserDialog
import com.blueshark.music.util.SPUtil
import java.io.File

class FolderChooser(val activity: BaseActivity,
                    val tag: String?,
                    private val newFolderLabel: Int?,
                    private val preferenceName: String?,
                    private val preferenceKey: String?,
                    val callback: FolderCallback?
) : FolderChooserDialog.FolderCallback {

  interface FolderCallback {
    fun onFolderSelection(chooser: FolderChooser, folder: File)
  }

  private data class Choice(
      val volume: String?,
      val directory: String
  )

  private var selectedVolume: String? = null

  fun show() {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
//        !PermissionUtil.hasManageExternalStorage()) {
//      Theme
//          .getBaseDialog(activity)
//          .content(R.string.manage_external_storage_permission_tip)
//          .positiveText(R.string.confirm)
//          .negativeText(R.string.cancel)
//          .onPositive { _, _ -> PermissionUtil.requestManageExternalStorage(activity) }
//          .show()
//      return
//    }

    val builder = FolderChooserDialog
        .Builder(activity)
        .chooseButton(R.string.choose_folder)
        .allowNewFolder(newFolderLabel != null, newFolderLabel ?: 0)
        .tag(tag)
        .callback(this)

    var lastChoice: Choice? = null
    preferenceName?.let { preferenceName ->
      preferenceKey?.let { preferenceKey ->
        try {
          lastChoice = Gson().fromJson(
              SPUtil.getValue(activity, preferenceName, preferenceKey, ""),
              Choice::class.java
          )
        } catch (throwable: Throwable) {
        }
      }
    }

    val volumes: ArrayList<String> = ArrayList()
    val storageManager = activity.getSystemService(Context.STORAGE_SERVICE) as StorageManager?
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      storageManager?.storageVolumes?.forEach { storageVolume ->
        storageVolume?.directory?.let { directory ->
          volumes.add(directory.absolutePath)
        }
      }
    } else {
      storageManager?.storageVolumes?.forEach { storageVolume ->
        val path = getPath(storageVolume)
        if (path != null && path != "/dev/null") {
          volumes.add(path)
        }
      }
    }

    if (volumes.size > 1) {
      Theme
          .getBaseDialog(activity)
          .items(volumes)
          .itemsCallback { _, _, _, text ->
            selectedVolume = text.toString()
            builder.initialPath(
                if (text == lastChoice?.volume) {
                  lastChoice!!.directory
                } else {
                  text.toString()
                }
            )
            builder.show()
          }
          .show()
    } else {
      var initialPath: String? = null
      lastChoice?.let {
        if (it.volume == null) {
          initialPath = it.directory
        }
      }
      if (initialPath != null) {
        builder.initialPath(initialPath)
      }
      builder.show()
    }
  }

  override fun onFolderSelection(dialog: FolderChooserDialog, folder: File) {
    preferenceName?.let { preferenceName ->
      preferenceKey?.let { preferenceKey ->
        SPUtil.deleteValue(activity, preferenceName, preferenceKey)
        if (folder.isDirectory && folder.startsWith(selectedVolume ?: folder.absolutePath)) {
          SPUtil.putValue(
              activity,
              preferenceName,
              preferenceKey,
              GsonBuilder()
                  .serializeNulls()
                  .create()
                  .toJson(Choice(selectedVolume, folder.absolutePath))
          )
        }
      }
    }

    callback?.onFolderSelection(this, folder)
  }


  fun getPath(vol: StorageVolume?): String? {
    return try {
      val cls = Class.forName("android.os.storage.StorageVolume")
      val method = cls.getMethod("getPathFile")
      val path = method.invoke(vol) as File
      path.absolutePath
    } catch (e: Exception) {
      null
    }
  }
}