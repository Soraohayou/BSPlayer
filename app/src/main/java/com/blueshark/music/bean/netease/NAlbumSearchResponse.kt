package com.blueshark.music.bean.netease

data class NAlbumSearchResponse(val code: Int,
                                val result: ResultBean? = null) {

  data class ResultBean(val albumCount: Int,
                        val albums: List<AlbumsBean?>? = null) {

    data class AlbumsBean(val picUrl: String?)
  }
}