package com.anddle.anddlemusic;

import android.graphics.Bitmap;
import android.net.Uri;

//定义一个类描述音乐信息，
public class MusicItem {
    //存放音乐名字
    String name;
    //存放音乐的Uri地址
    Uri songUri;
    //存放音乐封面的Uri地址
    Uri albumUri;
    //存放封面图片
    Bitmap thumb;
    //存储音乐时长，单位毫秒
    long duration;
    long playedTime;

    MusicItem(Uri songUri, Uri albumUri, String strName, long duration, long playedTime) {
        this.name = strName;
        this.songUri = songUri;
        this.duration = duration;
        this.playedTime = playedTime;
        this.albumUri = albumUri;
    }

    @Override
    //重写MusicItem的equals（）方法
    public boolean equals(Object o) {
        MusicItem another = (MusicItem) o;
        //音乐的Uri相同则说明两首音乐相同
        return another.songUri.equals(this.songUri);
    }
}