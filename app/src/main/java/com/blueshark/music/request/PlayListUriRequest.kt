//package com.blueshark.music.request
//
//import android.net.Uri
//import com.facebook.drawee.view.SimpleDraweeView
//import io.reactivex.Observable
//import io.reactivex.ObservableSource
//import io.reactivex.disposables.Disposable
//import io.reactivex.functions.Function
//import io.reactivex.observers.DisposableObserver
//import com.blueshark.music.App
//import com.blueshark.music.bean.mp3.Song
//import com.blueshark.music.db.room.DatabaseRepository
//import com.blueshark.music.util.RxUtil
//import com.blueshark.music.util.ImageUriUtil.getSearchRequestWithAlbumType
//
///**
// * Created by Remix on 2017/11/30.
// */
//
//@Deprecated("replace with glide")
//open class PlayListUriRequest(image: SimpleDraweeView, request: UriRequest, config: RequestConfig) : LibraryUriRequest(image, request, config) {
//
//  override fun onError(throwable: Throwable?) {
//    super.onError(throwable)
////    mImageRef.get()?.setImageURI(Uri.EMPTY)
//  }
//
//  override fun load(): Disposable {
//    val coverObservables = DatabaseRepository.getInstance()
//        .getPlayList(request.id)
//        .flatMap { playList ->
//          DatabaseRepository.getInstance()
//              .getPlayListSongs(App.context, playList, true)
//        }
//        .flatMapObservable(Function<List<Song>, ObservableSource<Song>> { songs ->
//          Observable.create { emitter ->
//            for (song in songs) {
//              emitter.onNext(song)
//            }
//            emitter.onComplete()
//          }
//        })
//        .concatMapDelayError { song ->
//          getCoverObservable(getSearchRequestWithAlbumType(song))
//        }
//
//    return Observable.concat(getCustomThumbObservable(request), coverObservables)
//        .firstOrError()
//        .toObservable()
//        .compose(RxUtil.applyScheduler())
//        .subscribeWith(object : DisposableObserver<String>() {
//          override fun onStart() {
//            ref.get()?.setImageURI(Uri.EMPTY)
//          }
//
//          override fun onNext(s: String) {
//            onSuccess(s)
//          }
//
//          override fun onError(e: Throwable) {
//            this@PlayListUriRequest.onError(e)
//          }
//
//          override fun onComplete() {
//
//          }
//        })
//  }
//
//}
