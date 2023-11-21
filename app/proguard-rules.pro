# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in d:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#指定代码的压缩级别
-optimizationpasses 5
#包明不混合大小写
-dontusemixedcaseclassnames
#不去忽略非公共的库类
-dontskipnonpubliclibraryclasses
#优化  不优化输入的类文件
-dontoptimize
#预校验
-dontpreverify
#混淆时是否记录日志
-verbose
#混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
#保持哪些类不被混淆
#保护注解
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
#崩溃信息
-keepattributes SourceFile,LineNumberTable

#basedata
-keepclasseswithmembers class * implements java.io.Serializable{
    <fields>;
    <methods>;
}
-keep class **.bean.**{*;}
# 保留Parcelable序列化类不被混淆
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclasseswithmembers class * implements android.os.Parcelable{
    <fields>;
    <methods>;
}
-keep class * extends com.blueshark.music.ui.adapter.holder.BaseViewHolder{*;}

-keep class **.R$* {*;}
-keep public class com.blueshark.music.R$*{
public static final int *;
}
-keepclassmembers class ** {
    @com.blueshark.music.misc.handler.OnHandleMessage public *;
}

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends java.lang.annotation.Annotation
-keep public class * extends android.os.Handler
-keep class !android.support.v7.internal.view.menu.**,android.support.** {*;}

-keep public class **.R$*{
   public static final int *;
}

# Keep native methods
-keepclassmembers class * {
    native <methods>;
}

-dontwarn okio.**
-dontwarn com.squareup.okhttp.**
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-dontwarn com.android.volley.toolbox.**
-dontwarn com.facebook.infer.**


#retrofit2
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
 long producerIndex;
 long consumerIndex;
}

#rxpermission
-keep class com.tbruyelle.rxpermissions.**{*;}

#lambda
-dontwarn java.lang.invoke.*
-dontwarn **$$Lambda$*

# 如果你需要兼容6.0系统，请不要混淆org.apache.http.legacy.jar
-dontwarn android.net.compatibility.**
-dontwarn android.net.http.**
-dontwarn com.android.internal.http.multipart.**
-dontwarn org.apache.commons.**
-dontwarn org.apache.http.**
-keep class android.net.compatibility.**{*;}
-keep class android.net.http.**{*;}
-keep class com.android.internal.http.multipart.**{*;}
-keep class org.apache.commons.**{*;}
-keep class org.apache.http.**{*;}

#kotlin
-dontwarn kotlin.**

#coroutine
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# kotlinx
-dontwarn kotlinx.**
-keepnames class kotlinx.** { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

#ijkplayer
-keep class tv.danmaku.ijk.** { *; }

#taglib
-keep class com.ijkplay.AudioTaglib.** { *; }

#jaudiotagger
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
