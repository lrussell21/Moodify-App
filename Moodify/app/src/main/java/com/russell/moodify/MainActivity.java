package com.russell.moodify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    spotifyAPIFetcher apiObject;
    createSpotifyAPIFetcher apiObjectStarter;

    ImageView albumImg;

    Runnable updateListUI;
    ListView songList;
    ArrayAdapter<String> adapter;
    ArrayList<String> arrayList;

    SeekBar danceSB;
    SeekBar happySB;
    SeekBar energySB;
    TextView usersName;
    TextView danceTV;
    TextView happyTV;
    TextView energyTV;
    TextView songNameTextView;
    TextView artistNameTextView;
    CheckBox energyCheckBox;
    CheckBox happyCheckBox;
    CheckBox danceCheckBox;

    Thread imageThread = null;

    boolean updateAllSongList = true;
    Thread updateAllSongListThread = null;

    int selectedIndex;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiObject = new spotifyAPIFetcher(MainActivity.this);
        apiObjectStarter = new createSpotifyAPIFetcher(apiObject);


        try{
            String tempCode = getIntent().getDataString();
            String[] codeParts = tempCode.split("code=");
            apiObjectStarter.userCode = codeParts[1];
        }catch (Exception ex){

        }

        Thread tokenStarterThread = new Thread(apiObjectStarter);
        tokenStarterThread.start();

        try{

            tokenStarterThread.join();
            if(apiObjectStarter.getTokenStatus()){
                Toast.makeText(getApplicationContext(), "Successfully logged in!",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getApplicationContext(), "Failed login!",Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, MainLoginScreen.class));
            }
        }catch (Exception ex){

        }

        songList = findViewById(R.id.songList);
        arrayList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, arrayList);

        songList.setAdapter(adapter);

        arrayList.add("No songs...");
        adapter.notifyDataSetChanged();



        usersName = findViewById(R.id.usertextView);
        artistNameTextView = findViewById(R.id.artistNameTextView);
        songNameTextView = findViewById(R.id.songNameTextView);
        danceSB = findViewById(R.id.danceSeekBar);
        happySB = findViewById(R.id.happySeekBar);
        energySB = findViewById(R.id.energySeekBar);
        danceTV = findViewById(R.id.danceTextView);
        happyTV = findViewById(R.id.happyTextView);
        energyTV = findViewById(R.id.energyTextView);
        energyCheckBox = findViewById(R.id.energyCheckBox);
        happyCheckBox = findViewById(R.id.happyCheckBox);
        danceCheckBox = findViewById(R.id.danceCheckBox);

        albumImg = findViewById(R.id.albumImage);

        usersName.setText("Logged in as " + apiObject.username);

        apiObject.getPlaylistIDsThreaded();


        initListeners();

        updateAllSongListThread = new Thread(this::updateList);
        updateAllSongListThread.start();


        updateListUI = new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                songList.invalidateViews();

                if(arrayList.size() > 0) {
                    selectedIndex = 0;
                    //albumImg.setImageResource(R.drawable.loading);
                    imageThread = new Thread(MainActivity.this::imageSetter);
                    imageThread.start();
                }

            }
        };

    }

    private void checkBoxSongCheck(){
        if(!apiObject.energyCheck && !apiObject.happyCheck && !apiObject.danceCheck){
            listAllSongs();
        }else{
            filterSongs();
        }
    }

    private void filterSongs(){
        updateAllSongList = false;
        apiObject.checkAudioFeatures();
        arrayList.clear();
        for(int i = 0; i < apiObject.displaySongs.size(); i++){
            arrayList.add(apiObject.displaySongs.get(i).getArtist() + " - " + apiObject.displaySongs.get(i).getSongName());
        }
        //adapter.notifyDataSetChanged();
        renderList();
    }

    private void listAllSongs(){
        updateAllSongList = true;
        arrayList.clear();
        apiObject.displaySongs.clear();
        for(int i = 0; i < apiObject.allSongs.size(); i++){
            arrayList.add(apiObject.allSongs.get(i).getArtist() + " - " + apiObject.allSongs.get(i).getSongName());
            apiObject.displaySongs.add(apiObject.allSongs.get(i));
        }
        //adapter.notifyDataSetChanged();
        renderList();
    }

    private void imageSetter(){
        artistNameTextView.setText(apiObject.displaySongs.get(selectedIndex).getArtist());
        songNameTextView.setText(apiObject.displaySongs.get(selectedIndex).getSongName());
        try {
            String urldisplay = apiObject.displaySongs.get(selectedIndex).getCoverartLink();
            Bitmap bmp = null;

            InputStream in = new java.net.URL(urldisplay).openStream();
            bmp = BitmapFactory.decodeStream(in);

            if(bmp != null) {
                albumImg.setImageBitmap(bmp);
            }
        } catch(Exception ex){
            System.out.println(ex.toString());
        }
    }

    private void updateList(){
        while(!apiObject.gettingSongsFinished){
            if(updateAllSongList && apiObject.updateList){
                new Handler(Looper.getMainLooper()).post(new Runnable(){
                    @Override
                    public void run() {
                        listAllSongs();
                        apiObject.updateList = false;
                    }
                });
            }
            try{
                Thread.sleep(1000);
            }catch (Exception ex){
                System.out.println(ex.toString());
            }
        }
    }

    private void renderList(){
        runOnUiThread(updateListUI);
    }

    private void initListeners(){
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

        energyCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                apiObject.energyCheck = isChecked;
                checkBoxSongCheck();
            }
        });

        happyCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                apiObject.happyCheck = isChecked;
                checkBoxSongCheck();
            }
        });

        danceCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                apiObject.danceCheck = isChecked;
                checkBoxSongCheck();
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

        Button loginButton = findViewById(R.id.logOutButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String filename = "userCredentials";
                String fileContents = "";
                FileOutputStream outputStream;

                try {
                    outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                    outputStream.write(fileContents.getBytes());
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                startActivity(new Intent(MainActivity.this, MainLoginScreen.class));
            }
        });


        songList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedIndex = position;
                albumImg.setImageResource(R.drawable.loading);
                imageThread = new Thread(MainActivity.this::imageSetter);
                imageThread.start();
            }
        });
    }


}
