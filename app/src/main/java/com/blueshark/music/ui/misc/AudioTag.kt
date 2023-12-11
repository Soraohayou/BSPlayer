package com.blueshark.music.ui.misc

import android.annotation.SuppressLint
import android.app.RecoverableSecurityException
import android.content.ContextWrapper
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.afollestad.materialdialogs.GravityEnum
import com.google.android.material.textfield.TextInputLayout
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_song_edit.view.*
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.tag.FieldKey
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.databinding.DialogSongDetailBinding
import com.blueshark.music.helper.MusicServiceRemote.getCurrentSong
import com.blueshark.music.misc.cache.DiskCache
import com.blueshark.music.theme.TextInputLayoutUtil
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.theme.TintHelper
import com.blueshark.music.ui.activity.base.BaseActivity
import com.blueshark.music.util.Constants.MB
import com.blueshark.music.util.ToastUtil
import com.blueshark.music.util.Util
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*


class AudioTag(val activity: BaseActivity, song: Song?) : ContextWrapper(activity) {
    private val song: Song = song ?: getCurrentSong()

    private var title: String = ""
    private var album: String = ""
    private var artist: String = ""
    private var year: String = ""
    private var genre: String = ""
    private var track: String = ""

    fun detail() {
        var disposable: Disposable? = null

        val detailDialog = Theme.getBaseDialog(this)
            .title(R.string.song_detail)
            .customView(R.layout.dialog_song_detail, true)
            .positiveText(R.string.close)
            .onPositive { _, _ -> disposable?.dispose() }
            .build()

        val binding = DialogSongDetailBinding.bind(detailDialog.customView!!)

        binding.songDetailPath.text = song.data
        binding.songDetailName.text = song.showName
        binding.songDetailSize.text = getString(R.string.cache_size, 1.0f * song.size / MB)
        binding.songDetailDuration.text = Util.getTime(song.duration)

        arrayOf(
            binding.songDetailPath,
            binding.songDetailName,
            binding.songDetailSize,
            binding.songDetailMime,
            binding.songDetailDuration,
            binding.songDetailBitRate,
            binding.songDetailSampleRate
        ).forEach {
            TintHelper.setTint(it, ThemeStore.accentColor, false)
        }

        disposable = Single.fromCallable { AudioFileIO.read(File(song.data)).audioHeader }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(
                { audioHeader ->
                    binding.songDetailMime.text = audioHeader.format
                    @SuppressLint("SetTextI18n")
                    binding.songDetailBitRate.text = "${audioHeader.bitRate} kb/s"
                    @SuppressLint("SetTextI18n")
                    binding.songDetailSampleRate.text = "${audioHeader.sampleRate} Hz"
                }, {
                    ToastUtil.show(this, getString(R.string.init_failed, it))
                })

        detailDialog.show()
    }

    fun edit() {
        val editDialog = Theme.getBaseDialog(this)
            .title(R.string.song_edit)
            .backgroundColor(Color.parseColor("#1C232D"))
            .titleGravity(GravityEnum.CENTER)
            .customView(R.layout.dialog_song_edit, true)
            .negativeText(R.string.cancel)
            .positiveText(R.string.confirm)
            .onPositive { dialog, which ->
                dialog.customView?.let { root ->
                    title = root.song_layout.editText?.text.toString()
                    artist = root.artist_layout.editText?.text.toString()
                    album = root.album_layout.editText?.text.toString()
                    genre = root.genre_layout.editText?.text.toString()
                    year = root.year_layout.editText?.text.toString()
                    track = root.track_layout.editText?.text.toString()
                    if (TextUtils.isEmpty(title)) {
                        ToastUtil.show(this, R.string.song_not_empty)
                        return@onPositive
                    }
                    saveTag()
                }
            }
            .build()

        editDialog.customView?.let { root ->
            val textInputTintColor = ThemeStore.accentColor
            val editTintColor = ThemeStore.accentColor
            TextInputLayoutUtil.setAccent(root.song_layout, textInputTintColor)
            TintHelper.setTintAuto(root.song_layout.editText!!, editTintColor, false)
            root.song_layout.editText?.addTextChangedListener(
                TextInputEditWatcher(
                    root.song_layout,
                    getString(R.string.song_not_empty)
                )
            )
            root.song_layout.editText?.setText(song.title)

            TextInputLayoutUtil.setAccent(root.album_layout, textInputTintColor)
            TintHelper.setTintAuto(root.album_layout.editText!!, editTintColor, false)
            root.album_layout.editText?.setText(song.album)

            TextInputLayoutUtil.setAccent(root.artist_layout, textInputTintColor)
            TintHelper.setTintAuto(root.artist_layout.editText!!, editTintColor, false)
            root.artist_layout.editText?.setText(song.artist)

            TextInputLayoutUtil.setAccent(root.year_layout, textInputTintColor)
            TintHelper.setTintAuto(root.year_layout.editText!!, editTintColor, false)
            root.year_layout.editText?.setText(song.year)

            TextInputLayoutUtil.setAccent(root.track_layout, textInputTintColor)
            TintHelper.setTintAuto(root.track_layout.editText!!, editTintColor, false)
            root.track_layout.editText?.setText(song.track)

            TextInputLayoutUtil.setAccent(root.genre_layout, textInputTintColor)
            TintHelper.setTintAuto(root.genre_layout.editText!!, editTintColor, false)
            root.genre_layout.editText?.setText(song.genre)
        }

        editDialog.show()
    }

    fun saveTag() {
        fun saveTag(file: File) {
            val audioFile = AudioFileIO.read(file)
            val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java)
            fieldKeyValueMap[FieldKey.ALBUM] = album
            fieldKeyValueMap[FieldKey.TITLE] = title
            fieldKeyValueMap[FieldKey.YEAR] = year
            fieldKeyValueMap[FieldKey.GENRE] = genre
            fieldKeyValueMap[FieldKey.ARTIST] = artist
            fieldKeyValueMap[FieldKey.TRACK] = track
            val tag = audioFile.tagOrCreateAndSetDefault
            for ((key, value) in fieldKeyValueMap) {
                try {
                    tag.setField(key, value)
                } catch (e: Exception) {
                    Timber.v("setField($key, $value) failed: $e")
                }
            }
            audioFile.commit()
        }

        try {
            try {
                saveTag(File(song.data))
                ToastUtil.show(this, R.string.save_success)
            } catch (e: CannotWriteException) {
                val cacheDir = DiskCache.getDiskCacheDir(this, CACHE_DIR_NAME)
                val tmpFile =
                    File(
                        cacheDir,
                        song.data.substring(song.data.lastIndexOf(File.separatorChar) + 1)
                    )
                try {
                    var songFD =
                        activity.contentResolver.openFileDescriptor(
                            song.contentUri,
                            "w"
                        )!! // test if we can write
                    songFD.close()

                    cacheDir.mkdirs()
                    tmpFile.createNewFile()

                    songFD = activity.contentResolver.openFileDescriptor(song.contentUri, "r")!!
                    var inputStream = FileInputStream(songFD.fileDescriptor)
                    var outputStream = FileOutputStream(tmpFile)
                    inputStream.copyTo(outputStream, inputStream.available())
                    songFD.close()
                    outputStream.close()

                    saveTag(tmpFile)

                    songFD = activity.contentResolver.openFileDescriptor(song.contentUri, "w")!!
                    inputStream = FileInputStream(tmpFile)
                    outputStream = FileOutputStream(songFD.fileDescriptor)
                    inputStream.copyTo(outputStream, inputStream.available())
                    inputStream.close()
                    songFD.close()

                    ToastUtil.show(this, R.string.save_success)
                } catch (securityException: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException is RecoverableSecurityException) {
                        activity.audioTag = this
                        startIntentSenderForResult(
                            activity,
                            securityException.userAction.actionIntent.intentSender,
                            REQUEST_WRITE_PERMISSION,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                        return
                    }
                    throw securityException
                } catch (e: Exception) {
                    throw e
                } finally {
                    tmpFile.delete()
                }
            }
            MediaScannerConnection.scanFile(
                activity,
                arrayOf(song.data), null
            ) { _, uri ->
                activity.contentResolver.notifyChange(uri, null)
            }
        } catch (e: Exception) {
            Timber.v("Fail to save tag")
            e.printStackTrace()
            ToastUtil.show(activity, R.string.save_error_arg, e.toString())
        }
    }

    companion object {
        private const val CACHE_DIR_NAME = "tag"
        const val REQUEST_WRITE_PERMISSION = 0x100
    }
}


private class TextInputEditWatcher internal constructor(
    private val mInputLayout: TextInputLayout,
    private val mError: String
) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (s == null || TextUtils.isEmpty(s.toString())) {
            mInputLayout.error = mError
        } else {
            mInputLayout.error = ""
        }
    }
}

