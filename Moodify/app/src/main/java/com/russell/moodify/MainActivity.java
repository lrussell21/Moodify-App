package com.russell.moodify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    spotifyAPIFetcher apiObject;
    createSpotifyAPIFetcher apiObjectStarter;

    ImageView albumImg;
    ImageView loadingInd;
    ListView songList;
    //ArrayAdapter<String> adapter;
    songListAdapter adapter;
    ArrayList<songList> songArrayList;
    ArrayAdapter<String> adapterCategory;
    ArrayList<String> categoryArrayList;
    SeekBar danceSB;
    SeekBar happySB;
    SeekBar energySB;
    SeekBar toleranceSB;
    TextView usersName;
    TextView danceTV;
    TextView happyTV;
    TextView energyTV;
    TextView toleranceTV;
    TextView songNameTextView;
    TextView artistNameTextView;
    CheckBox energyCheckBox;
    CheckBox happyCheckBox;
    CheckBox danceCheckBox;
    Spinner categorySpinner;
    boolean updateSelectedSong = true;
    boolean updateAllSongList = true;
    Thread updateAllSongListThread = null;
    Thread imageThread = null;
    int selectedIndex;
    String selectedLink;
    double selectedEnergy = 1;
    double selectedHappy = 1;
    double selectedDanceability = 1;

    //@RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Sets up API class objects.
        apiObject = new spotifyAPIFetcher(MainActivity.this);
        apiObjectStarter = new createSpotifyAPIFetcher(apiObject);

        // If user logs in the api token is in intent.
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

        //Original
        //songList = findViewById(R.id.songList);
        //songArrayList = new ArrayList<String>();
        //adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, songArrayList);
        //adapter.setNotifyOnChange(true);
        //songList.setAdapter(adapter);

        songList = findViewById(R.id.songList);
        songArrayList = new ArrayList<songList>();
        adapter = new songListAdapter(this, R.layout.songlistadapter_view_layout, songArrayList);
        songList.setAdapter(adapter);

        //songArrayList.add("Loading...");
        //adapter.notifyDataSetChanged();


        categorySpinner = findViewById(R.id.categorySpinner);
        categoryArrayList = apiObject.categoryNames;
        adapterCategory = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categoryArrayList);
        adapterCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterCategory.setNotifyOnChange(true);
        categorySpinner.setAdapter(adapterCategory);



        usersName = findViewById(R.id.usertextView);
        artistNameTextView = findViewById(R.id.artistNameTextView);
        songNameTextView = findViewById(R.id.songNameTextView);
        danceSB = findViewById(R.id.danceSeekBar);
        happySB = findViewById(R.id.happySeekBar);
        energySB = findViewById(R.id.energySeekBar);
        toleranceSB = findViewById(R.id.toleranceSeekBar);
        danceTV = findViewById(R.id.danceTextView);
        happyTV = findViewById(R.id.happyTextView);
        energyTV = findViewById(R.id.energyTextView);
        toleranceTV = findViewById(R.id.toleranceTextView);
        energyCheckBox = findViewById(R.id.energyCheckBox);
        happyCheckBox = findViewById(R.id.happyCheckBox);
        danceCheckBox = findViewById(R.id.danceCheckBox);

        albumImg = findViewById(R.id.albumImage);
        loadingInd = findViewById(R.id.loadingIndicatorImageView);

        usersName.setText("Logged in as " + apiObject.username);


        //GETTING SONGS STARTER
        //apiObject.getPlaylistIDsThreaded();
        // Currently being called when setting up spinner listener
        // apiObject.getPlaylistIDsThreadedCategorySelected();

        initListeners();

        updateListThreaded();

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

        toleranceSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                toleranceTV.setText("Tolerance: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                apiObject.tolerance = (double)seekBar.getProgress() / 100;
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
                try {

                    // DELETE
                    apiObject.getCurrentDeviceThreaded();


                    Uri uri = Uri.parse(selectedLink);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }catch (Exception ex){

                }
            }
        });

        // Sets mood to one similar to selected song.
        Button similarButton = findViewById(R.id.similarButton);
        similarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Similar
                // Set tolerance to 10, set bars to songs.
                if(!energyCheckBox.isChecked()){
                    energyCheckBox.setChecked(!energyCheckBox.isChecked());
                    //apiObject.energyCheck = true;
                }
                if(!happyCheckBox.isChecked()){
                    happyCheckBox.setChecked(!happyCheckBox.isChecked());
                    //apiObject.happyCheck = true;
                }
                if(!danceCheckBox.isChecked()){
                    danceCheckBox.setChecked(!danceCheckBox.isChecked());
                    //apiObject.danceCheck = true;
                }

                energySB.setProgress((int)(selectedEnergy * 100));
                apiObject.energy = selectedEnergy;
                energyTV.setText("Energy: " + (int)(selectedEnergy * 100));

                happySB.setProgress((int)(selectedHappy * 100));
                apiObject.happy = selectedHappy;
                happyTV.setText("Happy: " + (int)(selectedHappy * 100));

                danceSB.setProgress((int)(selectedDanceability * 100));
                apiObject.dance = selectedDanceability;
                danceTV.setText("Danceability: " + (int)(selectedDanceability * 100));

                toleranceSB.setProgress(10);
                toleranceTV.setText("Tolerance: 10");

                //Toast.makeText(getApplicationContext(), "Selected energy: " + selectedEnergy + " : " + apiObject.energy,Toast.LENGTH_SHORT).show();

                checkBoxSongCheck();
                //Toast.makeText(getApplicationContext(), "Test failed successfully!",Toast.LENGTH_SHORT).show();

            }
        });

        Button logoutButton = findViewById(R.id.logOutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
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
                if(songArrayList.size() > 0) {
                    selectedLink = "https://open.spotify.com/track/" + apiObject.displaySongs.get(position).getID();
                    selectedEnergy = apiObject.displaySongs.get(position).getEnergy();
                    selectedHappy = apiObject.displaySongs.get(position).getHappy();
                    selectedDanceability = apiObject.displaySongs.get(position).getDanceability();
                }
                selectedIndex = position;
                albumImg.setImageResource(R.drawable.loading);
                imageThread = new Thread(MainActivity.this::imageSetter);
                imageThread.start();
            }
        });


        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                categorySpinner.setSelection(position);
                songArrayList.clear();
                adapter.notifyDataSetChanged();
                apiObject.selectedCategory = apiObject.categoryIDs.get(position);
                apiObject.getPlaylistIDsThreadedCategorySelected();
                apiObject.gettingSongsFinished = false;

                updateAllSongList = true;
                updateListThreaded();
                Toast.makeText(getApplicationContext(), "Selected new genre!",Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    /**
     *
     */
    private void checkBoxSongCheck(){
        if(!apiObject.energyCheck && !apiObject.happyCheck && !apiObject.danceCheck){
            listAllSongs();
        }else{
            filterSongs();
        }
    }

    /**
     *
     */
    private void filterSongs(){
        updateSelectedSong = true;
        updateAllSongList = false;
        apiObject.checkAudioFeatures();
        songArrayList.clear();
        for(int i = 0; i < apiObject.displaySongs.size(); i++){
            songArrayList.add(new songList(apiObject.displaySongs.get(i).getSongName(), apiObject.displaySongs.get(i).getArtist(), apiObject.displaySongs.get(i).getCoverartLinkLight()));
        }
        adapter.notifyDataSetChanged();
    }

    /**
     *
     */
    private void listAllSongs(){
        updateAllSongList = true;
        songArrayList.clear();
        apiObject.displaySongs.clear();
        songList sL;
        for(int i = 0; i < apiObject.allSongs.size(); i++){
            sL = new songList(apiObject.allSongs.get(i).getSongName(), apiObject.allSongs.get(i).getArtist(), apiObject.allSongs.get(i).getCoverartLinkLight());
            songArrayList.add(sL);
            apiObject.displaySongs.add(apiObject.allSongs.get(i));
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * Sets selected song data and fetches coverart.
     */
    private void imageSetter(){
        artistNameTextView.setText(apiObject.displaySongs.get(selectedIndex).getArtist());
        songNameTextView.setText(apiObject.displaySongs.get(selectedIndex).getSongName());
        try {
            String urldisplay = apiObject.displaySongs.get(selectedIndex).getCoverartLink();
            new DownloadImageTask((ImageView) findViewById(R.id.albumImage)).execute(urldisplay);

        } catch(Exception ex){
            System.out.println(ex.toString());
        }
    }

    /**
     * Initiates updateList with new thread.
     */
    private void updateListThreaded(){
        updateAllSongListThread = new Thread(MainActivity.this::updateList);
        updateAllSongListThread.start();
    }

    /**
     * If no mood checkboxes are checked every second checks if new songs were added, and if
     * there were new songs added updates list.
     */
    private void updateList(){
        loadingInd.setImageResource(android.R.color.holo_red_light);
        while(!apiObject.gettingSongsFinished){
            if(updateAllSongList && apiObject.updateList){
                new Handler(Looper.getMainLooper()).post(new Runnable(){
                    @Override
                    public void run() {
                        listAllSongs();
                        //apiObject.updateList = false;
                    }
                });
            }
            try{
                Thread.sleep(100);
            }catch (Exception ex){
                System.out.println(ex.toString());
            }
        }
        loadingInd.setImageResource(R.color.green);
    }

}
