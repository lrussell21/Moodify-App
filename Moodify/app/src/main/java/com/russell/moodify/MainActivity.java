package com.russell.moodify;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    spotifyAPIFetcher apiObject;
    createSpotifyAPIFetcher apiObjectStarter;

    ImageView albumImg;

    ListView songList;
    ArrayAdapter<String> adapter;
    ArrayList<String> arrayList;

    SeekBar danceSB;
    SeekBar happySB;
    SeekBar energySB;
    TextView danceTV;
    TextView happyTV;
    TextView energyTV;
    TextView songNameTextView;
    TextView artistNameTextView;

    Thread t;

    int selectedIndex;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiObject = new spotifyAPIFetcher();
        apiObjectStarter = new createSpotifyAPIFetcher(apiObject);

        Thread tokenStarterThread = new Thread(apiObjectStarter);
        tokenStarterThread.start();
        try{
            tokenStarterThread.join();
            if(apiObjectStarter.getTokenStatus()){
                Toast.makeText(getApplicationContext(), "Successfully retrieved token!",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getApplicationContext(), "Failed to retrieve token!",Toast.LENGTH_SHORT).show();
            }
        }catch (Exception ex){

        }

        songList = findViewById(R.id.songList);
        arrayList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, arrayList);

        songList.setAdapter(adapter);

        arrayList.add("No songs...");
        adapter.notifyDataSetChanged();

        artistNameTextView = findViewById(R.id.artistNameTextView);
        songNameTextView = findViewById(R.id.songNameTextView);
        danceSB = findViewById(R.id.danceSeekBar);
        happySB = findViewById(R.id.happySeekBar);
        energySB = findViewById(R.id.energySeekBar);
        danceTV = findViewById(R.id.danceTextView);
        happyTV = findViewById(R.id.happyTextView);
        energyTV = findViewById(R.id.energyTextView);


        albumImg = findViewById(R.id.albumImage);

        apiObject.getPlaylistIDsThreaded();


        danceSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                danceTV.setText("Danceability: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                apiObject.dance = (double)seekBar.getProgress() / 100;
                filterSongs();
            }
        });

        happySB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                happyTV.setText("Happy: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                apiObject.happy = (double)seekBar.getProgress() / 100;
                filterSongs();
            }
        });


        energySB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                energyTV.setText("Energy: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                apiObject.energy = (double)seekBar.getProgress() / 100;
                filterSongs();
            }
        });


        Button tempBut = findViewById(R.id.tempButton);

        tempBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Getting top tracks!",Toast.LENGTH_SHORT).show();
                apiObject.getTopTracks();
            }
        });

        Button refreshButton = findViewById(R.id.refreshButton);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arrayList.clear();
                for(int i = 0; i < apiObject.allSongs.size(); i++){
                    apiObject.displaySongs.add(apiObject.allSongs.get(i));
                    arrayList.add(apiObject.allSongs.get(i).getArtist() + " - " + apiObject.allSongs.get(i).getSongName());
                }
                adapter.notifyDataSetChanged();
            }
        });

        Button playButton = findViewById(R.id.playButton);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String idToGoTo = apiObject.displaySongs.get(selectedIndex).getID();
                String link = "https://open.spotify.com/track/" + idToGoTo;
                Uri uri = Uri.parse(link);
                Intent intent= new Intent(Intent.ACTION_VIEW,uri);
                startActivity(intent);
            }
        });


        songList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedIndex = position;
                Thread test = new Thread(MainActivity.this::imageSetter);
                test.start();
            }
        });

    }

    private void filterSongs(){
        apiObject.checkAudioFeatures();
        arrayList.clear();
        for(int i = 0; i < apiObject.displaySongs.size(); i++){
            arrayList.add(apiObject.displaySongs.get(i).getArtist() + " - " + apiObject.displaySongs.get(i).getSongName());
        }
        adapter.notifyDataSetChanged();
    }

    private void imageSetter(){
        artistNameTextView.setText(apiObject.displaySongs.get(selectedIndex).getArtist());
        songNameTextView.setText(apiObject.displaySongs.get(selectedIndex).getSongName());
        try {
            String urldisplay = apiObject.displaySongs.get(selectedIndex).getCoverartLink();
            Bitmap bmp = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                bmp = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                //Log.e("Error", e.getMessage());
                //Toast.makeText(getApplicationContext(), e.toString(),Toast.LENGTH_SHORT).show();
                //System.out.println(e.toString());
                e.printStackTrace();
            }

            if(bmp != null) {
                albumImg.setImageBitmap(bmp);
            }
        } catch(Exception ex){
            System.out.println(ex.toString());

        }
    }
}
