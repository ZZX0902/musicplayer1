package com.anddle.anddlemusic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;


public class MusicItemAdapter extends BaseAdapter {

    private List<MusicItem> mData;
    private final LayoutInflater mInflater;
    private final int mResource;
    private Context mContext;

    public MusicItemAdapter(Context context, int resId, List<MusicItem> data)
    {
        mContext = context;
        mData = data;
        mInflater = LayoutInflater.from(context);
        mResource = resId;
    }

    @Override
    //返回音乐的个数
    public int getCount() {
        return mData != null ? mData.size() : 0;
    }

    @Override
    //返回具体哪一首歌曲
    public Object getItem(int position) {
        return mData != null ? mData.get(position): null ;
    }

    @Override
    //返回索引
    public long getItemId(int position) {
        return position;
    }

    @Override
    //创建视图
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(mResource, parent, false);
        }

        MusicItem item = mData.get(position);

        TextView title = (TextView) convertView.findViewById(R.id.music_title);
        title.setText(item.name);

        TextView createTime = (TextView) convertView.findViewById(R.id.music_duration);
        //调用辅助函数转换时间格式
        String times = Utils.convertMSecendToTime(item.duration);
        times = String.format(mContext.getString(R.string.duration), times);
        createTime.setText(times);

        ImageView thumb = (ImageView) convertView.findViewById(R.id.music_thumb);
        if(thumb != null) {
            if (item.thumb != null) {
                thumb.setImageBitmap(item.thumb);
            } else {
                thumb.setImageResource(R.mipmap.default_cover);
            }
        }

        return convertView;
    }

}
