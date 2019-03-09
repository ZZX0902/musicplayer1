package com.anddle.anddlemusic;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

//展示音乐列表，当前进度，与播放服务建立联系获取播放列表和播放控制接口
public class MusicListActivity extends AppCompatActivity {

    static public String TAG = "MusicListActivity";
    //设置ListView创建列表
    private List<MusicItem> mMusicList;
    private ListView mMusicListView;
    private Button mPlayBtn;
    private Button mPreBtn;
    private Button mNextBtn;
    private TextView mMusicTitle;
    private TextView mPlayedTime;
    private TextView mDurationTime;
    private SeekBar mMusicSeekBar;
    //创建一个MusicUpdateTask对象
    private MusicUpdateTask mMusicUpdateTask;

    @Override
    //在主界面所在的MusicListActivity创建时启动MusicUpdateTask的运行
    protected void onCreate(Bundle savedInstanceState) {//解析
        //创建Adapter并设置给ListView
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list);

        mMusicList = new ArrayList<MusicItem>();
        mMusicListView = (ListView) findViewById(R.id.music_list);
        MusicItemAdapter adapter = new MusicItemAdapter(this, R.layout.music_item, mMusicList);
        mMusicListView.setAdapter(adapter);
        //设置监听器，监听音乐列表的点击事件
        mMusicListView.setOnItemClickListener(mOnMusicItemClickListener);
        //设置多选modal模式
        mMusicListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        //设置多选模式变化的监听器
        mMusicListView.setMultiChoiceModeListener(mMultiChoiceListener);
        //播放和暂停使用的按钮
        mPlayBtn = (Button) findViewById(R.id.play_btn);
        //上一首
        mPreBtn = (Button) findViewById(R.id.pre_btn);
        //下一首
        mNextBtn = (Button) findViewById(R.id.next_btn);
        //音乐名称
        mMusicTitle = (TextView) findViewById(R.id.music_title);
        //播放时长
        mDurationTime = (TextView) findViewById(R.id.duration_time);
        //当前播放时间
        mPlayedTime = (TextView) findViewById(R.id.played_time);
        //进度条
        mMusicSeekBar = (SeekBar) findViewById(R.id.seek_music);
        mMusicSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        mMusicUpdateTask = new MusicUpdateTask();
        mMusicUpdateTask.execute();
        //明确指定那个Service启动
        Intent i = new Intent(this, MusicService.class);
        //启动MusicService
        startService(i);
        //实现绑定操作
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    //在主界面所在的MusicListActivity销毁时取消MusicUpdateTask的运行
    protected void onDestroy() {
        super.onDestroy();

        if(mMusicUpdateTask != null && mMusicUpdateTask.getStatus() == AsyncTask.Status.RUNNING) {
            mMusicUpdateTask.cancel(true);
        }
        mMusicUpdateTask = null;
        //注销监听函数
        mMusicService.unregisterOnStateChangeListener(mStateChangeListenr);
        //当MusicListActivity退出时，将MusicService解除绑定
        unbindService(mServiceConnection);
        //手动回收使用的图片资源
        for(MusicItem item : mMusicList) {
            if( item.thumb != null ) {
                item.thumb.recycle();
                item.thumb = null;
            }
        }
        mMusicList.clear();
    }
    //进度条拖动的监听器
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //停止拖动时，根据进度条的位置来设定播放的位置
            if(mMusicService != null) {
                mMusicService.seekTo(seekBar.getProgress());
            }
        }
    };

    //修改监听器，实现单击音乐添加到播放列表中
    private AdapterView.OnItemClickListener mOnMusicItemClickListener = new AdapterView.OnItemClickListener() {

        @Override

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if(mMusicService != null) {
                //点击响应处，添加播放音乐的代码
                //通过MusicService提供的接口，把要添加的音乐交给MusicService处理
                mMusicService.addPlayList(mMusicList.get(position));
            }
        }
    };
    //定义一个MultiChoiceModeListener将菜单和菜单被点击后的响应事件添加进去
    private ListView.MultiChoiceModeListener mMultiChoiceListener = new AbsListView.MultiChoiceModeListener() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            //增加进入modal模式后的菜单栏菜单项
            getMenuInflater().inflate(R.menu.music_choice_actionbar, menu);
            //使控制区域不可操作
            enableControlPanel(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch(item.getItemId()) {
                case R.id.menu_play: {
                    //添加点击后添加到播放列表的响应
                    //获取被选中的音乐
                    List musicList = new ArrayList<MusicItem>();
                    SparseBooleanArray checkedResult = mMusicListView.getCheckedItemPositions();
                    for (int i = 0; i < checkedResult.size(); i++) {
                        if(checkedResult.valueAt(i)) {
                            int pos = checkedResult.keyAt(i);
                            MusicItem music = mMusicList.get(pos);
                            musicList.add(music);
                        }
                    }
                    //调用MusicService提供的接口，把播放列表保存起来
                    mMusicService.addPlayList(musicList);
                    //退出ListView的modal状态
                    mode.finish();
                }
                break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            //使控制区域可操作
            enableControlPanel(true);
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

        }
    };
    //控制区域的状态是否为可操作状态
    private void enableControlPanel(boolean enabled) {
        mPlayBtn.setEnabled(enabled);
        mPreBtn.setEnabled(enabled);
        mNextBtn.setEnabled(enabled);
        mMusicSeekBar.setEnabled(enabled);
    }
    //更新播放信息
    private void updatePlayingInfo(MusicItem item) {
        //将毫秒单位的时间，转化为00:00形式的格式
        String times = Utils.convertMSecendToTime(item.duration);
        mDurationTime.setText(times);

        times = Utils.convertMSecendToTime(item.playedTime);
        mPlayedTime.setText(times);
        //设置进度条最大值
        mMusicSeekBar.setMax((int) item.duration);
        //设置进度条当前值
        mMusicSeekBar.setProgress((int) item.playedTime);

        mMusicTitle.setText(item.name);
    }
    //注册监听函数，为了获取MusicService的状态
    private MusicService.OnStateChangeListenr mStateChangeListenr = new MusicService.OnStateChangeListenr() {

        @Override
        public void onPlayProgressChange(MusicItem item) {
            //更新播放进度信息
            updatePlayingInfo(item);
        }

        @Override
        public void onPlay(MusicItem item) {
            //更新播放按钮背景
            mPlayBtn.setBackgroundResource(R.mipmap.ic_pause);
            updatePlayingInfo(item);
            //激活控制区域
            enableControlPanel(true);
        }

        @Override
        public void onPause(MusicItem item) {
            //更新播放按钮背景
            mPlayBtn.setBackgroundResource(R.mipmap.ic_play);
            //激活控制区域
            enableControlPanel(true);
        }
    };
    //创建一个AsyncTask，在它的doBackground（）方法中进行查询操作
    private class MusicUpdateTask extends AsyncTask<Object, MusicItem, Void> {
        //工作线程，处理耗时的查询音乐的操作
        @Override
        protected Void doInBackground(Object... params) {
            //查询外部存储地址上的音乐文件
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            //查询音乐文件所使用的字段名称
            String[] searchKey = new String[] {
                    //对应文件在数据库中的检索ID
                    MediaStore.Audio.Media._ID,
                    //对于那个文件的标题
                    MediaStore.Audio.Media.TITLE,
                    //对应文件所在的专辑ID
                    MediaStore.Audio.Albums.ALBUM_ID,
                    //对应文件的存放位置
                    MediaStore.Audio.Media.DATA,
                    //对应文件的播放时长
                    MediaStore.Audio.Media.DURATION
            };
            //查询到的文件路径中包含music这个字段的所有文件
            String where = MediaStore.Audio.Media.DATA + " like \"%"+getString(R.string.search_path)+"%\"";
            String [] keywords = null;
            //设定为默认排序方式
            String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
            //获取ContentResolver对象，并向Media Provider发起查询请求，查询结果存放在Cursor（指标）当中
            ContentResolver resolver = getContentResolver();
            Cursor cursor = resolver.query(uri, searchKey, where, keywords, sortOrder);

            if(cursor != null)
            {
                //遍历Cursor，得到它指向的每一条查询到的信息，当其指向某条数据时，获取它携带的每个字段值
                while(cursor.moveToNext() && ! isCancelled())
                {
                    //获取音乐的路径
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    //获取音乐ID
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    //通过Uri和Id组合出该音乐的特有Uri地址
                    Uri musicUri = Uri.withAppendedPath(uri, id);
                    //获取音乐的名称
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    //获取音乐的时长，单位是毫秒
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                    //该音乐所在专辑ID
                    int albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ID));
                    //通过AlbumID组合出专辑的Uri地址
                    Uri albumUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
                    //创建一个MusicItem对象
                    MusicItem data = new MusicItem(musicUri, albumUri, name, duration, 0);
                    if (uri != null) {
                        ContentResolver res = getContentResolver();
                        data.thumb = Utils.createThumbFromUir(res, albumUri);
                    }

                    Log.d(TAG, "real music found: " + path);

                    publishProgress(data);

                }
                //cursor使用完后要关闭
                cursor.close();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(MusicItem... values) {
        //主线程，把要显示的音乐添加到音乐的展示列表中
            MusicItem data = values[0];

            mMusicList.add(data);
            MusicItemAdapter adapter = (MusicItemAdapter) mMusicListView.getAdapter();
            adapter.notifyDataSetChanged();

        }
    }
    //MusicListActivity绑定MusicService的时候，先定义一个ServiceConnection
    private MusicService.MusicServiceIBinder mMusicService;
    //创建一个ServiceConnection，当绑定Service后，在onServiceConnection（）中会得到Service返回的Binder
    private ServiceConnection mServiceConnection = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //这个service参数，就是Service当中onBind（）返回的Binder
            //获取访问Service的桥梁 --MusicServiceIBinder
            //绑定成功后取得MusicService提供的接口
            mMusicService = (MusicService.MusicServiceIBinder) service;
            //注册监听器
            mMusicService.registerOnStateChangeListener(mStateChangeListenr);
            //获取播放列表中可播放的音乐
            MusicItem item = mMusicService.getCurrentMusic();
            if(item == null) {
                //没有可播放的音乐时，控制区域不可操作
                enableControlPanel(false);
                return;
            }
            else {
                //根据当前被加载的音乐信息更新控制区域信息
                updatePlayingInfo(item);
            }
            if(mMusicService.isPlaying()) {
                //如果音乐处于播放状态则按钮背景设置为暂停图标
                mPlayBtn.setBackgroundResource(R.mipmap.ic_pause);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    public void onClick(View view) {
        switch (view.getId()) {
            //播放音乐按钮
            case R.id.play_btn: {
                //启动Service
                if(mMusicService != null) {
                    if(!mMusicService.isPlaying()) {
                        //开始播放
                        mMusicService.play();
                    }
                    else {
                        //暂停播放
                        mMusicService.pause();
                    }
                }
            }
            break;
            //下一曲按钮
            case R.id.next_btn: {
                if(mMusicService != null) {
                    mMusicService.playNext();
                }
            }
            break;
            //上一曲按钮
            case R.id.pre_btn: {
                if(mMusicService != null) {
                    mMusicService.playPre();
                }
            }
            break;
        }
    }
    //显示播放列表的实现
    private void showPlayList() {

        final AlertDialog.Builder builder=new AlertDialog.Builder(this);
        //设置对话框的图标
        builder.setIcon(R.mipmap.ic_playlist);
        //设计对话框的显示标题
        builder.setTitle(R.string.play_list);
        //获取播放列表，把播放列表中歌曲的名字取出组成新的列表
        List<MusicItem> playList = mMusicService.getPlayList();
        ArrayList<String> data = new ArrayList<String>();
        for(MusicItem music : playList) {
            data.add(music.name);
        }
        if(data.size() > 0) {
            //播放列表有曲目则显示音乐的名称
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data);
            builder.setAdapter(adapter, null);
        }
        else {
            //播放列表没有曲目则显示没有音乐
            builder.setMessage(getString(R.string.no_song));
        }
        //设置对话框可以取消，点击空白处则对话框消失
        builder.setCancelable(true);
        //创建并显示对话框
        builder.create().show();
    }

    @Override
    //将定义菜单的xml文件添加到MusiclistActivity中
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    //当用户点击菜单时将播放列表显示出来
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.play_list_menu: {
                //响应用户的点击事件，显示出播放列表
                showPlayList();
            }
            break;

        }

        return true;
    }
}
