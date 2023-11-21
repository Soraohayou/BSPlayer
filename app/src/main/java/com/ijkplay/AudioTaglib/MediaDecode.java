package com.ijkplay.AudioTaglib;


public class MediaDecode {
    
	public MediaDecode(){
		
	}
	
	public native PId3Info GetId3Info(String filepath);
	public native int getMp3FileAlbumPic(String filepath,String picpath);
	public native int getWmaFileAlbumPic(String filepath,String picpath);
	public native int getM4aFileAlbumPic(String filepath,String picpath);
	public native int getWavFileAlbumPic(String filepath,String picpath);
	public native int getAiffFileAlbumPic(String filepath,String picpath);
	public native int getFlacFileAlbumPic(String filepath,String picpath);
	public native int getOggFileAlbumPic(String filepath,String picpath);
	public native int getApeFileAlbumPic(String filepath,String picpath);
    public native int getDsfFileAlbumPic(String filepath,String picpath);
	static {
        System.loadLibrary("jni_taglib");
    }

}
