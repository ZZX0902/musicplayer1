package com.anddle.anddlemusic;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//播放服务：为主界面MusicListActivity提供音乐播放操作播放列表的接口
//从PlayListContentProvider获取播放列表的操作，拥有MediaPlayer对象，真正的控制播放器
public class MusicService extends Service {
        //对外的接口函数，可以被Activity中的MusicServiceIBinder调用
    public interface OnStateChangeListenr {
        //用来通知播放进度
        void onPlayProgressChange(MusicItem item);
        //用来通知当前处于播放状态
        void onPlay(MusicItem item);
        //用来通知当前处于暂停或停止状态
        void onPause(MusicItem item);
    }
    //定义循环发送的消息
    private final int MSG_PROGRESS_UPDATE = 0;
    //处理定义消息的Handler，用来显示播放音乐的进度条
    public static final String ACTION_PLAY_MUSIC_PRE = "com.anddle.anddlemusic.playpre";
    public static final String ACTION_PLAY_MUSIC_NEXT = "com.anddle.anddlemusic.playnext";
    public static final String ACTION_PLAY_MUSIC_TOGGLE = "com.anddle.anddlemusic.playtoggle";
    public static final String ACTION_PLAY_MUSIC_UPDATE = "com.anddle.anddlemusic.playupdate";
    //创建存储监听器的存储列表
    private List<OnStateChangeListenr> mListenerList = new ArrayList<OnStateChangeListenr>();

    //播放列表要保存的数组，实现列表的保存
    private List<MusicItem> mPlayList;
    //创建一个MusicItem类的数据类型用来存放当前要播放的音乐
    private MusicItem mCurrentMusicItem;
    //声明一个MediaPlayer实例
    private MediaPlayer mMusicPlayer;
    //添加修改删除都需要通过ContentResolver获取访问ContentProvider的入口，来添加修改删除要操作的数据
    private ContentResolver mResolver;
    //用来判断当前是否为播放暂停状态
    private boolean mPaused;
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PROGRESS_UPDATE: {
                    //将音乐的时长和当前播放的进度保存到MusicItem中
                    mCurrentMusicItem.playedTime = mMusicPlayer.getCurrentPosition();
                    mCurrentMusicItem.duration = mMusicPlayer.getDuration();
                    //通知监听者当前的播放进度
                    for(OnStateChangeListenr l : mListenerList) {
                        l.onPlayProgressChange(mCurrentMusicItem);
                    }
                    //将当前的播放进度保存到数据库中
                    updateMusicItem(mCurrentMusicItem);
                    //间隔一秒发送一次更新播放进度的消息
                    sendEmptyMessageDelayed(MSG_PROGRESS_UPDATE, 1000);
                }
                break;
            }
        }
    };





    @Override
    //更新状态Intent传参来判断播放哪一首音乐
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (ACTION_PLAY_MUSIC_PRE.equals(action)) {
                    playPreInner();
                } else if (ACTION_PLAY_MUSIC_NEXT.equals(action)) {
                    playNextInner();
                } else if (ACTION_PLAY_MUSIC_TOGGLE.equals(action)) {
                    if (isPlayingInner()) {
                        pauseInner();
                    } else {
                        playInner();
                    }
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //创建一个MediaPlayer实例
        mMusicPlayer = new MediaPlayer();
        //获取ContentProvider的解析器，避免以后每次使用时都要重新获取
        mResolver = getContentResolver();
        //保存播放列表
        mPlayList = new ArrayList<MusicItem>();
        mPaused = false;
        mMusicPlayer.setOnCompletionListener(mOnCompletionListener);


        //加载数据库中的现有列表
        initPlayList();

        if(mCurrentMusicItem != null) {

            prepareToPlay(mCurrentMusicItem);
        }



    }
    //将要播放的音乐载入MediaPlayer，但并不播放
    private void prepareToPlay(MusicItem item) {
        try {
            //重置播放状态
            mMusicPlayer.reset();
            //设置播放的音乐状态
            mMusicPlayer.setDataSource(MusicService.this, item.songUri);
            //准备播放音乐
            mMusicPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mMusicPlayer.isPlaying()) {
            mMusicPlayer.stop();
        }
        //释放MediaPlayer实例
        mMusicPlayer.release();


        //停止更新
        mHandler.removeMessages(MSG_PROGRESS_UPDATE);
        //当MusicService销毁时，清空监听器列表
        mListenerList.clear();
        for(MusicItem item : mPlayList) {
            if(item.thumb != null) {
                item.thumb.recycle();
            }
        }

        mPlayList.clear();
    }
    //创建一个自定义的Binder用它连接Service和组件，完成组件与Service之间的调用
    public class MusicServiceIBinder extends Binder {
                //真正实现功能的方法
         //一次添加一首音乐
        public void addPlayList(MusicItem item) { addPlayListInner(item, true); }
         //一次添加多首音乐
        public void addPlayList(List<MusicItem> items) {
            addPlayListInner(items);
        }


        public void play() {
            playInner();
        }

        public void playNext() {
            playNextInner();
        }

        public void playPre() {
            playPreInner();
        }

        public void pause() {
            pauseInner();
        }

        public void seekTo(int pos) {
            seekToInner(pos);
        }
        //需要监听MusicService时需要用registerOnStateChangeListener和unregisterOnStateChangeListener注册
        //MusicService把注册的监听器存储到数组列表中，需要时取出来使用
        public void registerOnStateChangeListener(OnStateChangeListenr l) {
            //将监听器添加到列表
            registerOnStateChangeListenerInner(l);

        }

        public void unregisterOnStateChangeListener(OnStateChangeListenr l) {
            //将监听器从列表中移除
            unregisterOnStateChangeListenerInner(l);
        }

        public MusicItem getCurrentMusic() {
            return getCurrentMusicInner();
        }

        public boolean isPlaying() {
            return isPlayingInner();
        }

        public List<MusicItem> getPlayList() {
            return mPlayList;
        }

    }
    //创建Binder实例
    private final IBinder mBinder = new MusicServiceIBinder();

    @Override

    public IBinder onBind(Intent intent) {
   //当组件bindService（）之后，将这个Binder返回给组件使用
        return mBinder;
    }

    //添加监听器
    private void registerOnStateChangeListenerInner(OnStateChangeListenr l) {
        mListenerList.add(l);
    }
    //消除监听器
    private void unregisterOnStateChangeListenerInner(OnStateChangeListenr l) {
        mListenerList.remove(l);
    }

    private MusicItem getCurrentMusicInner() {
        //返回当前正加载好的音乐
        return mCurrentMusicItem;
    }

    private boolean isPlayingInner() {
        //返回当前的播放器是否正在播放音乐
        return mMusicPlayer.isPlaying();
    }
    //获取播放列表，再启动ContentProvider时将数据库中的现有列表加载到mPlayList中
    private void initPlayList() {
        mPlayList.clear();
        //通过ContentResolver获取ContentProvider的入口，使用Uri修改数据要修改的数据放入ContentValues中
        Cursor cursor = mResolver.query(
                PlayListContentProvider.CONTENT_SONGS_URI,
                null,
                null,
                null,
                null);

        while(cursor.moveToNext())
        {
            String songUri = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.SONG_URI));
            String albumUri = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.ALBUM_URI));
            String name = cursor.getString(cursor.getColumnIndex(DBHelper.NAME));
            long playedTime = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LAST_PLAY_TIME));
            long duration = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.DURATION));

            MusicItem item = new MusicItem(Uri.parse(songUri), Uri.parse(albumUri), name, duration, playedTime/*, isLastPlaying*/);
            mPlayList.add(item);
        }

        cursor.close();

        if( mPlayList.size() > 0) {
            //再次启动时将第一首音乐作为默认待播放的音乐
            mCurrentMusicItem = mPlayList.get(0);
        }
    }
    private void seekToInner(int pos) {
        //将音乐拖动到指定的时间
        mMusicPlayer.seekTo(pos);
    }
    //播放音乐，根据reload标志判断是否需要重新加载音乐
    private void playMusicItem(MusicItem item, boolean reload) {
        //如果传入空值则什么也不做
            if(item == null) {
                return;
            }

            if(reload) {
                //需要重新加载音乐
                prepareToPlay(item);
            }
            //开始播放音乐，如果之前状态是暂停则继续播放音乐
            mMusicPlayer.start();
            //将音乐设置到指定时间开始播放单位是毫秒
            seekToInner((int)item.playedTime);
            //将播放状态通过监听器通知监听者
            for(OnStateChangeListenr l : mListenerList) {
                l.onPlay(item);
            }
            //设置为非暂停状态
            mPaused = false;
            //移除现有的更新消息，重新启动更新
            mHandler.removeMessages(MSG_PROGRESS_UPDATE);
            mHandler.sendEmptyMessage(MSG_PROGRESS_UPDATE);


    }
    private void pauseInner() {
        //设置为暂停播放状态
        mPaused = true;
        //暂停当前正在播放的音乐
        mMusicPlayer.pause();
        //将播放状态的改变通知给监听者
        for(OnStateChangeListenr l : mListenerList) {
            l.onPause(mCurrentMusicItem);
        }
        //停止更新
        mHandler.removeMessages(MSG_PROGRESS_UPDATE);

    }
    private void playInner() {
        //如果之前没有选定要播放的音乐，选列表中的第一首音乐开始播放
        if(mCurrentMusicItem == null && mPlayList.size() > 0) {
            mCurrentMusicItem = mPlayList.get(0);
        }
        //如果是从暂停状态恢复播放音乐那么不需要重新加载音乐否则重新加载音乐
        if(mPaused) {
            playMusicItem(mCurrentMusicItem, false);
        }
        else {
            playMusicItem(mCurrentMusicItem, true);
        }

    }

    private void playPreInner() {
        int currentIndex = mPlayList.indexOf(mCurrentMusicItem);
        if(currentIndex - 1 >= 0 ) {
            //获取当前播放音乐的上一首，如果前面有要播放的音乐把那首音乐设置成要播放的音乐，并重新加载该音乐，开始播放
            mCurrentMusicItem = mPlayList.get(currentIndex - 1);
            playMusicItem(mCurrentMusicItem, true);
        }
    }
    //播放列表中当前音乐的下一首
    private void playNextInner() {
        int currentIndex = mPlayList.indexOf(mCurrentMusicItem);
        if(currentIndex < mPlayList.size() -1 ) {
            //获取当前播放音乐的下一首，如果后面有要播放的音乐把那首音乐设置成要播放的音乐，并重新加载该音乐，开始播放
            mCurrentMusicItem = mPlayList.get(currentIndex + 1);
            playMusicItem(mCurrentMusicItem, true);
        }
    }


    //一次添加一首音乐的实现
    private void addPlayListInner(MusicItem item, boolean needPlay) {
        //判断列表中是否已经存储过该音乐
        if(mPlayList.contains(item)) {
            return;
        }
        //添加到播放列表的第一个位置
        mPlayList.add(0, item);
        //将音乐信息保存到ContentProvider中
        insertMusicItemToContentProvider(item);

        if(needPlay) {
            //添加完成后开始播放
            mCurrentMusicItem = mPlayList.get(0);
            playInner();
        }
    }
    //一次添加多首音乐的实现
    private void addPlayListInner(List<MusicItem> items) {
        //通过ContentResolver获取ContentProvider的入口，使用Uri删除数据，
        //清空数据库的playlist_table表
        mResolver.delete(PlayListContentProvider.CONTENT_SONGS_URI, null, null);
        //清空缓存的播放列表
        mPlayList.clear();
        //将每首音乐添加到播放列表的缓存和数据库中
        for (MusicItem item : items) {
            //调用添加一首音乐的函数，
            addPlayListInner(item, false);
        }
        //添加完成后，开始播放
        mCurrentMusicItem = mPlayList.get(0);
        playInner();
    }


    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            //将当前播放的音乐记录时间重置为0，更新到数据库，下次播放从头开始
            mCurrentMusicItem.playedTime = 0;
            updateMusicItem(mCurrentMusicItem);
            //播放下一首音乐
            playNextInner();
        }
    };
    //访问ContentProvider，保存一条数据
    private void insertMusicItemToContentProvider(MusicItem item) {

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.NAME, item.name);
        cv.put(DBHelper.DURATION, item.duration);
        cv.put(DBHelper.LAST_PLAY_TIME, item.playedTime);
        cv.put(DBHelper.SONG_URI, item.songUri.toString());
        cv.put(DBHelper.ALBUM_URI, item.albumUri.toString());
        Uri uri = mResolver.insert(PlayListContentProvider.CONTENT_SONGS_URI, cv);
    }
    //将播放时间更新到ContentProvider中
    private void updateMusicItem(MusicItem item) {

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.DURATION, item.duration);
        cv.put(DBHelper.LAST_PLAY_TIME, item.playedTime);

        String strUri = item.songUri.toString();
        mResolver.update(PlayListContentProvider.CONTENT_SONGS_URI, cv, DBHelper.SONG_URI + "=\"" + strUri + "\"", null);
    }



}
