package com.anddle.anddlemusic;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;


//自定义一个的ContentProvider，用来封装数据库，记录当前播放列表提供给播放服务使用
public class PlayListContentProvider extends ContentProvider {
    //uri的scheme字段固定使用content
    private static final String SCHEME = "content://";
    //path为音乐路径
    private static final String PATH_SONGS = "/songs";
    //定义为程序的包
    public static final String AUTHORITY = "com.anddle.anddlemusicprovider";
//操作数据库要用到的路径类似于域名
    public static final Uri CONTENT_SONGS_URI = Uri.parse(SCHEME + AUTHORITY + PATH_SONGS);
    //声明成员变量mDBHelper，用来操作数据库
    private DBHelper mDBHelper;

    public PlayListContentProvider() {

    }

    @Override
    public boolean onCreate() {
        //ContentProvider被创建时，获取DBHelper对象

        mDBHelper = new DBHelper(getContext());

        return true;
    }

    @Override
    //需要为每一种类型的Uri返回一种数据类型--Mime type
    //现在简化成返回值为空
    public String getType(Uri uri) {

        return null;
    }

    @Override
    //实现query函数
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        //通过DBHelper获取写数据库的方法
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        //查询数据库中的数据
        Cursor cursor = db.query(DBHelper.PLAYLIST_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

        return cursor;
    }

    @Override
    //实现update函数
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        //通过DBHelper获取写数据库的方法
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        //更新数据库的指定项
        int count = db.update(DBHelper.PLAYLIST_TABLE_NAME, values, selection, selectionArgs);

        return count;
    }

    @Override
    //实现delete函数
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        //通过DBHelper获取写数据库的方法
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        //清空PLAYLIST_TABLE表，并将删除的数据条数返回
        int count = db.delete(DBHelper.PLAYLIST_TABLE_NAME, selection, selectionArgs);
        return count;
    }

    @Override
    //实现insert函数
    public Uri insert(Uri uri, ContentValues values) {

        Uri result = null;
        //从ContentValues中取出数据，保存后返回保存数据的Uri地址
        //通过DBHelper获取写数据库的方法
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        //将数据ContentValues插入到数据库中
        long id = db.insert(DBHelper.PLAYLIST_TABLE_NAME, null, values);
        if(id > 0) {
            //根据返回到id值组合成该数据项对应的uri地址
            result = ContentUris.withAppendedId(CONTENT_SONGS_URI, id);
        }

        return result;

    }

}
