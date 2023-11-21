package com.blueshark.music.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.activity_custom_sort.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import com.blueshark.music.R
import com.blueshark.music.bean.mp3.Song
import com.blueshark.music.databinding.ActivityCustomSortBinding
import com.blueshark.music.db.room.DatabaseRepository
import com.blueshark.music.db.room.model.PlayList
import com.blueshark.music.glide.UriFetcher
import com.blueshark.music.misc.interfaces.OnItemClickListener
import com.blueshark.music.theme.Theme
import com.blueshark.music.theme.ThemeStore
import com.blueshark.music.theme.TintHelper
import com.blueshark.music.ui.adapter.CustomSortAdapter
import com.blueshark.music.util.ColorUtil.isColorLight
import com.blueshark.music.util.ToastUtil
import com.blueshark.music.util.Util
import java.util.*
import kotlin.collections.ArrayList

class CustomSortActivity : ToolbarActivity() {
  private lateinit var binding: ActivityCustomSortBinding

  private val adapter: CustomSortAdapter by lazy {
    CustomSortAdapter(R.layout.item_custom_sort)
  }
  private val mdDialog: MaterialDialog by lazy {
    Theme.getBaseDialog(this)
        .title(R.string.saveing)
        .content(R.string.please_wait)
        .progress(true, 0)
        .progressIndeterminateStyle(false).build()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCustomSortBinding.inflate(layoutInflater)
    setContentView(binding.root)

    if (!intent.hasExtra(EXTRA_LIST) || !intent.hasExtra(EXTRA_PLAYLIST)) {
      ToastUtil.show(this, R.string.illegal_arg)
      finish()
      return
    }

    val songs = intent.getSerializableExtra(EXTRA_LIST) as ArrayList<Song>
    val playList = intent.getParcelableExtra<PlayList>(EXTRA_PLAYLIST)!!

    setUpToolbar(playList.name)

    adapter.setDataList(songs)
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemLongClick(view: View?, position: Int) {
        Util.vibrate(this@CustomSortActivity, 100)
      }

      override fun onItemClick(view: View?, position: Int) {

      }

    }

    ItemTouchHelper(object : ItemTouchHelper.Callback() {
      override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.DOWN or ItemTouchHelper.UP or ItemTouchHelper.RIGHT
        return makeMovementFlags(dragFlag, 0)
      }

      override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        Collections.swap(adapter.dataList, if (viewHolder.adapterPosition >= 0) viewHolder.adapterPosition else 0,
            if (target.adapterPosition >= 0) target.adapterPosition else 0)
        adapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
      }

      override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

      }


    }).attachToRecyclerView(custom_sort_recyclerView)

    custom_sort_recyclerView.setHasFixedSize(true)
    custom_sort_recyclerView.layoutManager = LinearLayoutManager(this)
    custom_sort_recyclerView.itemAnimator = DefaultItemAnimator()
    custom_sort_recyclerView.adapter = adapter


    val accentColor = ThemeStore.accentColor
    custom_sort_recyclerView.setBubbleColor(accentColor)
    custom_sort_recyclerView.setHandleColor(accentColor)
    custom_sort_recyclerView.setBubbleTextColor(resources.getColor(if (isColorLight(accentColor))
      R.color.light_text_color_primary
    else
      R.color.dark_text_color_primary))

    TintHelper.setTintAuto(binding.customSortSave, accentColor, true)

    binding.customSortSave.setOnClickListener {
      doAsync {
        uiThread {
          mdDialog.show()
        }

        Thread.sleep(500)
        val result = DatabaseRepository.getInstance()
            .updatePlayListAudios(playList.id, adapter.dataList.map { it.id })
            .blockingGet()

        UriFetcher.updatePlayListVersion()
        UriFetcher.clearAllCache()

        uiThread {
          ToastUtil.show(this@CustomSortActivity, if (result > 0) R.string.save_success else R.string.save_error)
          mdDialog.dismiss()
          finish()
        }
      }
    }
  }

  companion object {
    @JvmStatic
    fun start(context: Context, playList: PlayList, list: List<Song>) {
      context.startActivity(Intent(context, CustomSortActivity::class.java)
          .putExtra(EXTRA_PLAYLIST, playList)
          .putExtra(EXTRA_LIST, ArrayList(list)))
    }

    private const val EXTRA_PLAYLIST = "playlist"
    private const val EXTRA_LIST = "list"
  }

}

