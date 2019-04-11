package com.russell.moodify;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class songListAdapter extends ArrayAdapter<songList> {

    private static final String TAG = "songListAdapter";

    private Context mContext;
    private int mResource;
    private int lastPosition = -1;


    static class ViewHolder{
        TextView songName;
        TextView songArtist;
        ImageView albumImage;
    }


    public songListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<songList> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String songName = getItem(position).getSongName();
        String songArtist = getItem(position).getSongArtist();
        String albumUrl = getItem(position).getAlbumUrl();

        songList sList = new songList(songName, songArtist, albumUrl);

        final View result;
        ViewHolder holder;

        if(convertView == null){
            LayoutInflater inflator = LayoutInflater.from(mContext);
            convertView = inflator.inflate(mResource, parent, false);
            holder = new ViewHolder();
            holder.songName = (TextView) convertView.findViewById(R.id.textview1);
            holder.songArtist = (TextView) convertView.findViewById(R.id.textview2);
            holder.albumImage = (ImageView) convertView.findViewById(R.id.albumImageList);
            result = convertView;
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
            result = convertView;
        }


        Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.load_down_anim : R.anim.load_up_anim);
        result.startAnimation(animation);
        lastPosition = position;

        holder.songName.setText(sList.getSongName());
        holder.songArtist.setText(sList.getSongArtist());
        new DownloadImageTask(holder.albumImage).execute(albumUrl);


        return convertView;
    }
}
