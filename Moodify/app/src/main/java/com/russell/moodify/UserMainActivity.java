package com.russell.moodify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import java.util.ArrayList;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class UserMainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {




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
    Spinner categorySpinner;
    ImageView updateCategories;
    boolean updateSelectedSong = true;
    boolean updateAllSongList = true;
    Thread updateAllSongListThread = null;
    Thread imageThread = null;
    int selectedIndex = -1;
    String selectedLink;
    String selectedID;
    double selectedEnergy = 1;
    double selectedHappy = 1;
    double selectedDanceability = 1;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);







        // Sets up API class objects.
        apiObject = new spotifyAPIFetcher(UserMainActivity.this);
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
                startActivity(new Intent(UserMainActivity.this, MainLoginScreen.class));
            }
        }catch (Exception ex){

        }

        apiObject.getCurrentDeviceThreaded();

        //Original
        //songList = findViewById(R.id.songList);
        //songArrayList = new ArrayList<String>();
        //adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, songArrayList);
        //adapter.setNotifyOnChange(true);
        //songList.setAdapter(adapter);

        songList = findViewById(R.id.songList);
        songArrayList = new ArrayList<songList>();
        adapter = new songListAdapter(this, R.layout.songlistadapter_view_layout, songArrayList, apiObject);
        adapter.setNotifyOnChange(true);
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
        updateCategories = findViewById(R.id.loadGenre);


        albumImg = findViewById(R.id.albumImage);
        loadingInd = findViewById(R.id.loadingIndicatorImageView);

        usersName.setText("Logged in as " + apiObject.username);
        //usersName2 = findViewById(R.id.nav_header_title);
        //usersName2.setText("Logged in as " + apiObject.username);

        //GETTING SONGS STARTER
        //apiObject.getPlaylistIDsThreaded();
        // Currently being called when setting up spinner listener
        // apiObject.getPlaylistIDsThreadedCategorySelected();

        initListeners();

        updateListThreaded();

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        } else if (id == R.id.sign_out){

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

            startActivity(new Intent(UserMainActivity.this, MainLoginScreen.class));

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }






    private void initListeners(){
        danceSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress == 0){
                    apiObject.danceCheck = FALSE;
                    danceTV.setText("Danceability: OFF");
                }else{
                    apiObject.danceCheck = TRUE;
                    danceTV.setText("Danceability: " + (progress - 1));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                apiObject.dance = (double)(seekBar.getProgress() - 1) / 100;
                checkBoxSongCheck();
            }
        });

        happySB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress == 0){
                    apiObject.happyCheck = FALSE;
                    happyTV.setText("Happy: OFF");
                }else{
                    apiObject.happyCheck = TRUE;
                    happyTV.setText("Happy: " + (progress - 1));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                apiObject.happy = (double)(seekBar.getProgress() - 1) / 100;
                checkBoxSongCheck();
            }
        });

        energySB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress == 0){
                    energyTV.setText("Energy: OFF");
                    apiObject.energyCheck = FALSE;
                }else{
                    apiObject.energyCheck = TRUE;
                    energyTV.setText("Energy: " + (progress - 1));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                apiObject.energy = (double)(seekBar.getProgress() - 1) / 100;

                System.out.println("ENERGY:" + apiObject.energy);

                checkBoxSongCheck();
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


        Button addButton = findViewById(R.id.addButton);
        addButton.setText("Add");
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    apiObject.addSongToUserLibraryThreaded(selectedID);
                    Toast.makeText(getApplicationContext(), "Added " + songNameTextView.getText() + " to your library!",Toast.LENGTH_SHORT).show();

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

                energySB.setProgress((int)(selectedEnergy * 100));
                apiObject.energy = selectedEnergy;
                energyTV.setText("Energy: " + (int)(selectedEnergy * 100));
                apiObject.energyCheck = TRUE;

                happySB.setProgress((int)(selectedHappy * 100));
                apiObject.happy = selectedHappy;
                happyTV.setText("Happy: " + (int)(selectedHappy * 100));
                apiObject.happyCheck = TRUE;

                danceSB.setProgress((int)(selectedDanceability * 100));
                apiObject.dance = selectedDanceability;
                danceTV.setText("Danceability: " + (int)(selectedDanceability * 100));
                apiObject.danceCheck = TRUE;


                toleranceSB.setProgress(10);
                toleranceTV.setText("Tolerance: 10");

                //Toast.makeText(getApplicationContext(), "Selected energy: " + selectedEnergy + " : " + apiObject.energy,Toast.LENGTH_SHORT).show();

                checkBoxSongCheck();
                //Toast.makeText(getApplicationContext(), "Test failed successfully!",Toast.LENGTH_SHORT).show();

            }
        });

        songList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(songArrayList.size() > 0) {
                    selectedID = apiObject.displaySongs.get(position).getID();
                    selectedLink = "https://open.spotify.com/track/" + selectedID;
                    selectedEnergy = apiObject.displaySongs.get(position).getEnergy();
                    selectedHappy = apiObject.displaySongs.get(position).getHappy();
                    selectedDanceability = apiObject.displaySongs.get(position).getDanceability();
                }

                //view.setSelected(true);

                selectedIndex = position;
                albumImg.setImageResource(R.drawable.loading);
                imageThread = new Thread(UserMainActivity.this::imageSetter);
                imageThread.start();
            }
        });


        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView)parent.getChildAt(0)).setTextColor(Color.WHITE);
                categorySpinner.setSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        updateCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                v.setAlpha(0f);
                v.setVisibility(View.VISIBLE);
                v.animate().alpha(0.33f).setDuration(1000);


                songArrayList.clear();
                apiObject.displaySongs.clear();
                adapter.notifyDataSetInvalidated();
                adapter.notifyDataSetChanged();
                listAllSongs();


                apiObject.selectedCategory = apiObject.categoryIDs.get(categorySpinner.getSelectedItemPosition());
                apiObject.gettingSongsFinished = false;
                apiObject.getPlaylistIDsThreadedCategorySelected();


                updateAllSongList = true;
                updateListThreaded();
                Toast.makeText(getApplicationContext(), "Selected new genre!",Toast.LENGTH_SHORT).show();

                updateCategories.setClickable(false);





                energySB.setProgress(0);
                apiObject.energy = selectedEnergy;
                energyTV.setText("Energy: OFF");
                apiObject.energyCheck = FALSE;

                happySB.setProgress(0);
                apiObject.happy = selectedHappy;
                happyTV.setText("Happy: OFF");
                apiObject.happyCheck = FALSE;

                danceSB.setProgress(0);
                apiObject.dance = selectedDanceability;
                danceTV.setText("Danceability: OFF");
                apiObject.danceCheck = FALSE;


                toleranceSB.setProgress(15);
                toleranceTV.setText("Tolerance: 15");



                loadingCircleOn();



            }
        });

        albumImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                apiObject.playOnCurrentDeviceThreaded(selectedID);

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
            songArrayList.add(new songList(apiObject.displaySongs.get(i).getSongName(), apiObject.displaySongs.get(i).getArtist(), apiObject.displaySongs.get(i).getCoverartLinkLight(), apiObject.displaySongs.get(i).getID()));
        }
        adapter.notifyDataSetChanged();
    }

    /**
     *
     */
    private void listAllSongs(){

        //loadCircleOff();

        updateAllSongList = true;
        songArrayList.clear();
        apiObject.displaySongs.clear();
        songList sL;
        for(int i = 0; i < apiObject.allSongs.size(); i++){
            sL = new songList(apiObject.allSongs.get(i).getSongName(), apiObject.allSongs.get(i).getArtist(), apiObject.allSongs.get(i).getCoverartLinkLight(), apiObject.allSongs.get(i).getID());
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
        updateAllSongListThread = new Thread(UserMainActivity.this::updateList);
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

        loadCircleOff();

        new Handler(Looper.getMainLooper()).post(new Runnable(){
            @Override
            public void run() {
                listAllSongs();
                updateCategories.setClickable(true);
                updateCategories.animate().alpha(1f).setDuration(1000);
            }
        });

        loadingInd.setImageResource(R.color.green);
    }


    private void loadingCircleOn(){
        new Handler(Looper.getMainLooper()).post(new Runnable(){
            @Override
            public void run() {
                findViewById(R.id.loadingCircle).setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadCircleOff(){
        new Handler(Looper.getMainLooper()).post(new Runnable(){
            @Override
            public void run() {
                findViewById(R.id.loadingCircle).setVisibility(View.GONE);
            }
        });
    }


}
