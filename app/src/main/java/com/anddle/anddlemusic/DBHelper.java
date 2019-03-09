package com.anddle.anddlemusic;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    //数据库名为playlist.db
    private final static String DB_NAME = "playlist.db";
    private final static int DB_VERSION = 1;
    //数据库中的表playlist_table
    public final static String PLAYLIST_TABLE_NAME = "playlist_table";

    public final static String ID = "id";
    public final static String NAME = "name";
    public final static String LAST_PLAY_TIME = "last_play_time";
    public final static String SONG_URI = "song_uri";
    public final static String ALBUM_URI = "album_uri";
    public final static String DURATION = "duration";

    public  DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    //实现对数据库的创建
    public void onCreate(SQLiteDatabase db) {
        //创建播放列表的存储列表

        String PLAYLIST_TABLE_CMD = "CREATE TABLE " + PLAYLIST_TABLE_NAME
                + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + NAME +" VARCHAR(256),"
                + LAST_PLAY_TIME +" LONG,"
                + SONG_URI +" VARCHAR(128),"
                + ALBUM_URI +" VARCHAR(128),"
                + DURATION + " LONG"
                + ");" ;
        db.execSQL(PLAYLIST_TABLE_CMD);

    }

    @Override
    //告知开发者数据库版本有变化，可以重新组织已存储的数据
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    //如果遇到数据库更新则删除以前的表并重新创建一张
        db.execSQL("DROP TABLE IF EXISTS "+ PLAYLIST_TABLE_NAME);
        onCreate(db);
    }


}