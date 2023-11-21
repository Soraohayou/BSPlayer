package com.blueshark.music.request.network

import io.reactivex.Single
import com.blueshark.music.bean.github.Release
import com.blueshark.music.bean.kugou.KLrcResponse
import com.blueshark.music.bean.kugou.KSearchResponse
import com.blueshark.music.bean.lastfm.LastFmAlbum
import com.blueshark.music.bean.lastfm.LastFmArtist
import com.blueshark.music.bean.netease.NAlbumSearchResponse
import com.blueshark.music.bean.netease.NArtistSearchResponse
import com.blueshark.music.bean.netease.NLrcResponse
import com.blueshark.music.bean.netease.NSongSearchResponse
import com.blueshark.music.bean.netease.NDetailResponse
import com.blueshark.music.bean.qq.QLrcResponse
import com.blueshark.music.bean.qq.QSearchResponse
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by Remix on 2017/11/20.
 */
object HttpClient {
  private val neteaseApi: ApiService
  private val kuGouApi: ApiService
  private val qqApi: ApiService
  private val lastfmApi: ApiService
  private val githubApi: ApiService

  //New Api
  fun searchLastFMAlbum(
    albumName: String?,
    artistName: String?,
    lang: String?
  ): Single<LastFmAlbum> {
    return lastfmApi.searchLastFMAlbum(albumName, artistName, lang)
  }

  fun searchLastFMArtist(artistName: String?, language: String?): Single<LastFmArtist> {
    return lastfmApi.searchLastFMArtist(artistName, language)
  }

  fun searchNeteaseSong(key: String?, offset: Int, limit: Int): Single<NSongSearchResponse> {
    return neteaseApi.searchNeteaseSong(key, offset, limit, 1)
  }

  fun searchNeteaseAlbum(key: String?, offset: Int, limit: Int): Single<NAlbumSearchResponse> {
    return neteaseApi.searchNeteaseAlbum(key, offset, limit, 10)
  }

  fun searchNeteaseArtist(key: String?, offset: Int, limit: Int): Single<NArtistSearchResponse> {
    return neteaseApi.searchNeteaseArtist(key, offset, limit, 100)
  }

  fun searchNeteaseLyric(id: Int): Single<NLrcResponse> {
    return neteaseApi.searchNeteaseLyric("pc", id, -1, -1, -1)
  }

  fun searchNeteaseDetail(id: Int, ids: String?): Single<NDetailResponse> {
    return neteaseApi.searchNeteaseDetail(id,"[$id]" )
  }

  fun searchKuGou(keyword: String?, duration: Long): Single<KSearchResponse> {
    return kuGouApi.searchKuGou(1, "yes", "pc", keyword, duration, "")
  }

  fun searchKuGouLyric(id: Int, accessKey: String?): Single<KLrcResponse> {
    return kuGouApi.searchKuGouLyric(1, "pc", "lrc", "utf8", id, accessKey)
  }

  fun searchQQ(key: String?): Single<QSearchResponse> {
    return qqApi.searchQQ(1, key, "json")
  }

  fun searchQQLyric(songmid: String?): Single<QLrcResponse> {
    return qqApi.searchQQLyric(songmid, 5381, "json", 1)
  }


  private const val NETEASE_BASE_URL = "http://music.163.com/api/"
  private const val KUGOU_BASE_URL = "http://lyrics.kugou.com/"
  private const val QQ_BASE_URL = "https://c.y.qq.com/"
  private const val LASTFM_BASE_URL = "http://ws.audioscrobbler.com/2.0/"
  private const val GITHUB_BASE_URL = "https://api.github.com/"

  init {
    val retrofitBuilder = Retrofit.Builder()
    val okHttpClient = OkHttpHelper.okHttpClient
    neteaseApi = retrofitBuilder
      .baseUrl(NETEASE_BASE_URL)
      .client(okHttpClient)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .addConverterFactory(GsonConverterFactory.create())
      .build().create(ApiService::class.java)
    kuGouApi = retrofitBuilder
      .baseUrl(KUGOU_BASE_URL)
      .client(okHttpClient)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .addConverterFactory(GsonConverterFactory.create())
      .build().create(ApiService::class.java)
    qqApi = retrofitBuilder
      .baseUrl(QQ_BASE_URL)
      .client(okHttpClient)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .addConverterFactory(GsonConverterFactory.create())
      .build().create(ApiService::class.java)
    lastfmApi = retrofitBuilder
      .baseUrl(LASTFM_BASE_URL)
      .client(okHttpClient)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .addConverterFactory(GsonConverterFactory.create())
      .build().create(ApiService::class.java)
    githubApi = retrofitBuilder
      .baseUrl(GITHUB_BASE_URL)
      .client(okHttpClient)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .addConverterFactory(GsonConverterFactory.create())
      .build().create(ApiService::class.java)
  }
}