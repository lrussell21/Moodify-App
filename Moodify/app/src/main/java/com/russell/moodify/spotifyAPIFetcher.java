package com.russell.moodify;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;


public class spotifyAPIFetcher {
    public ArrayList<songs> allSongs = new ArrayList<>();
    public ArrayList<songs> displaySongs = new ArrayList<>();
    public ArrayList<String> playlistIDs = new ArrayList<>();
    public ArrayList<String> categoryIDs = new ArrayList<>();
    public ArrayList<String> categoryNames = new ArrayList<>();
    public String selectedCategory;

    Context context;

    private String[] categories = {"pop", "mood", "edm_dance", "decades", "hiphop", "chill", "workout", "party"};
    //private String[] categories = {"pop", "mood", "edm_dance", "decades", "hiphop", "chill", "workout", "party", "focus", "sleep", "rock", "dinner", "jazz", "rnb", "romance", "indie_alt", "gaming", "soul", "classical"};

    public String username = "";
    public String currentPlayingDeviceID = "";
    private String currentID = "";
    private final String client_id;
    private final String secret_id;
    private static String token;
    private String refreshToken;

    private static int saveLastPos = 0;

    public int playlistAmount = 5;
    public double dance, happy, energy;
    public boolean danceCheck = false;
    public boolean happyCheck = false;
    public boolean energyCheck = false;
    public volatile boolean playlistThreadRun = true;
    public volatile boolean gettingSongsFinished = false;
    public volatile boolean updateList = false;
    public double tolerance = 0.15;
    public int threadNumber = 0;
    private String addSongID = "";
    private Thread playlistIDThreadedCategorySelected = new Thread();
    private boolean playOnCurrentDeviceSucceeded;

    public spotifyAPIFetcher(Context context) {
        this.context = context;
        client_id = context.getResources().getString(R.string.client_id);
        secret_id = context.getResources().getString(R.string.secret_id);
    }

    /**
     * Encodes given string to base64
     * @param toEncode string to encode
     * @return encoded string.
     */
    private String base64endcoder(String toEncode){
        byte[] data = toEncode.getBytes();
        return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP);
    }

    private String base64decoder(String toDecode){
        System.out.println(toDecode);
        byte[] decodedBytes = android.util.Base64.decode(toDecode, Base64.DEFAULT);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Attempts to use refresh token, if it is good
     * then user data and category playlists will be fetched.
     * @return if refresh token is good returns true.
     */
    public boolean checkRefreshToken(){
        String filename = "userCredentials";
        FileInputStream inputStream;
        StringBuilder datax = new StringBuilder();
        try {
            inputStream = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);

            String readString = br.readLine();
            while(readString != null){
                datax.append(readString);
                readString = br.readLine();
            }
            isr.close();

            refreshToken = datax.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //useRefreshToken();
        String spoturl = "https://accounts.spotify.com/api/token";
        String urlParameters = "grant_type=refresh_token&refresh_token=" + refreshToken;
        String data = "";
        try {

            URL url = new URL(spoturl + "?" + urlParameters);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Authorization", "Basic " + base64endcoder((client_id + ":" + secret_id)));


            if (conn.getResponseCode() != 200) {
                //throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }


            try {
                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
                String output;
                //System.out.println("Output from Server ....");
                while ((output = br.readLine()) != null) {
                    data += output;
                    //System.out.println(output);
                }
                br.close();
            } catch (Exception e) {
                System.out.printf(e.getMessage());
            }
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            JSONObject parseObj = new JSONObject(data);
            token = parseObj.getString("access_token");
        } catch (Exception ex){

        }

        return getUserDetails();
    }

    /**
     * Gives Spotify client id and user code and returns token and refresh token.
     * @return token and refresh token for api use.
     */
    //@RequiresApi(api = Build.VERSION_CODES.O)
    public boolean loginUser(String APIcode) {

        String spoturl = "https://accounts.spotify.com/api/token";
        String urlParameters = "grant_type=authorization_code&code=" + APIcode + "&redirect_uri=moodify%3A%2F%2Flogincallback";
        String tokenStringReceive = "";
        try {

            URL url = new URL(spoturl + "?" + urlParameters);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Authorization", "Basic " + base64endcoder((client_id + ":" + secret_id)));

            if (conn.getResponseCode() != 200) {
                System.out.println(conn.getResponseMessage());
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }


            try {
                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
                String output;
                while ((output = br.readLine()) != null) {
                    tokenStringReceive += output;
                }
                br.close();
            } catch (Exception e) {
                System.out.printf(e.getMessage());
            }

            conn.disconnect();

            try {
                JSONObject parseObj = new JSONObject(tokenStringReceive);
                token = parseObj.getString("access_token");
                String refreshToken = parseObj.getString("refresh_token");

                String filename = "userCredentials";
                String fileContents = refreshToken;
                FileOutputStream outputStream;

                try {
                    outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
                    outputStream.write(fileContents.getBytes());
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception ex){

            }

            return getUserDetails();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean getUserDetails(){
        String spoturl = "https://api.spotify.com/v1/me";
        String data = "";
        try {
            URL url = new URL(spoturl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);


            if (conn.getResponseCode() != 200) {
                return false;
            }

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
                String output;
                //System.out.println("Output from Server ....");
                while ((output = br.readLine()) != null) {
                    data += output;
                    //System.out.println(output);
                }
                br.close();
            } catch (Exception e) {
                System.out.printf(e.getMessage());
            }
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            JSONObject parseObj = new JSONObject(data);
            username = parseObj.getString("display_name");
            //System.out.println(data);
        } catch (Exception ex){

        }
        return getCategories();
    }

    /**
     * Gets top 50 US tracks.
     */
    public void getTopTracks() {
        getSongIDs temp = new getSongIDs(this, token, "37i9dQZEVXbLRQDuF5jeBp");
        Thread t = new Thread(temp);
        t.start();
    }

    /**
     * Creates a background thread to start gathering playlists.
     */
    public void getPlaylistIDsThreaded() {
        Thread s = new Thread(this::getPlaylistIDs);
        s.start();
    }

    /**
     * Gets list of playlist ID's from each category.
     */
    public void getPlaylistIDs() {
        for (int cats = 0; cats < categories.length; cats++) {

            String fullOuputString = "";
            try {
                URL url = new URL("https://api.spotify.com/v1/browse/categories/" + categories[cats] + "/playlists");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                if (conn.getResponseCode() != 200) {
                    throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
                }

                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

                String output;
                //System.out.println("Output from Server ....");
                while ((output = br.readLine()) != null) {
                    //System.out.println(output);
                    fullOuputString += output + "\n";
                }
                br.close();
                conn.disconnect();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            // Parse incoming Json for playlist ID's.
            try {
                JSONObject parseObj = new JSONObject(fullOuputString);
                JSONObject mainObj = parseObj.getJSONObject("playlists");
                JSONArray items = mainObj.getJSONArray("items");

                JSONObject playlistObj;
                int amountOfPlaylists;
                if(items.length() > 10){
                    amountOfPlaylists = 10;
                }else{
                    amountOfPlaylists = items.length();
                }
                for(int i = 0; i < amountOfPlaylists; i++){
                    playlistObj = items.getJSONObject(i);
                    playlistIDs.add(playlistObj.getString("id"));
                }
            } catch (Exception ex){

            }
        }

        playlistIDToSongs();
    }

    /**
     * Creates a background thread to start gathering playlists.
     */
    public void getPlaylistIDsThreadedCategorySelected() {

        if(allSongs.size() != 0 && !playlistThreadRun) {
            playlistThreadRun = false;
            threadNumber++;
            try {
                int maxRun = 0;
                do {
                    if(maxRun >= 20){
                        playlistThreadRun = true;
                    }
                    Thread.sleep(50);
                    maxRun++;
                } while (!playlistThreadRun);
            } catch (Exception ex) {

            }
        }else{
            threadNumber++;
        }

        playlistIDThreadedCategorySelected = new Thread(this::getPlaylistIDsCategorySelected);
        playlistIDThreadedCategorySelected.start();
    }


    /**
     * Gets list of playlist ID's from each category.
     */
    public void getPlaylistIDsCategorySelected() {
        playlistIDs.clear();
        String fullOuputString = "";
        try {
            URL url = new URL("https://api.spotify.com/v1/browse/categories/" + selectedCategory + "/playlists");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output;
            //System.out.println("Output from Server ....");
            while ((output = br.readLine()) != null) {
                //System.out.println(output);
                fullOuputString += output + "\n";
            }
            br.close();
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Parse incoming Json for playlist ID's.
        try {
            JSONObject parseObj = new JSONObject(fullOuputString);
            JSONObject mainObj = parseObj.getJSONObject("playlists");
            JSONArray items = mainObj.getJSONArray("items");

            JSONObject playlistObj;
            int amountOfPlaylists;
            if (items.length() > playlistAmount) {
                amountOfPlaylists = playlistAmount;
            } else {
                amountOfPlaylists = items.length();
            }
            // TODO : Change BACK to amountOfPlaylists
            for (int i = 0; i < amountOfPlaylists; i++) {
                playlistObj = items.getJSONObject(i);
                playlistIDs.add(playlistObj.getString("id"));
            }
        } catch (Exception ex) {

        }
        playlistIDToSongs();
    }

    /**
     * Gets list of category ID's from each category.
     */
    public boolean getCategories() {
        String fullOuputString = "";
        try {
            URL url = new URL("https://api.spotify.com/v1/browse/categories");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            if (conn.getResponseCode() != 200) {
                //throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
                return false;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output;
            //System.out.println("Output from Server ....");
            while ((output = br.readLine()) != null) {
                //System.out.println(output);
                fullOuputString += output + "\n";
            }
            br.close();
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Parse incoming Json for playlist ID's.
        try {
            JSONObject parseObj = new JSONObject(fullOuputString);
            JSONObject mainObj = parseObj.getJSONObject("categories");
            JSONArray items = mainObj.getJSONArray("items");

            JSONObject playlistObj;
            for (int i = 0; i < items.length(); i++) {
                playlistObj = items.getJSONObject(i);
                categoryIDs.add(playlistObj.getString("id"));
                categoryNames.add(playlistObj.getString("name"));
            }
        } catch (Exception ex) {

        }
        if (categoryIDs.size() > 0) {
            selectedCategory = categoryIDs.get(0);
        }
        return true;
    }

    /**
     * Creates a background thread to start gathering songs from playlists.
     */
    public void playlistIDToSongsThreaded() {
        Thread s = new Thread(this::playlistIDToSongs);
        s.start();
    }

    /**
     * Goes through each playlist, creates a thread then gets all the songs from the playlist and adds it to allsongs Arraylist.
     */
    private void playlistIDToSongs() {

        //TESTING DELETE
        allSongs.clear();
        displaySongs.clear();
        saveLastPos = 0;

        int currentThreadNumber = threadNumber;

        gettingSongsFinished = false;
        updateList = false;

        getSongIDs threadSong;
        Thread getFeatures;
        Thread s[] = new Thread[10]; // Max 10 IDs from categoryToIDs
        //int saveSize = 0;
        for (int i = 0; i < playlistIDs.size(); i++) {
            try {
                //Thread.sleep(100);
                /*
                if(currentThreadNumber != threadNumber){
                    playlistThreadRun = true;
                    Thread.currentThread().interrupt();
                    return;
                }
                */
                threadSong = new getSongIDs(this, token, playlistIDs.get(i));
                s[i] = new Thread(threadSong);
                System.out.println("Started thread: " + i);
                s[i].start();
                //s.join();
                //updateList = true; // For MainActivity to know when to update list.
                /*
                if(i % 3 == 0){
                    System.out.println("Getting track features...");
                    getFeatures = new Thread(this::getTrackFeatures);
                    getFeatures.start();
                    getFeatures.join();
                }
                */
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
        //updateList = true;
        try{
            for(int i = 0; i < s.length; i++){
                // In case there are less than 10 playlists so a thread in the array isn't started.
                if(s[i].isAlive()) {
                    s[i].join();
                }
            }

            System.out.println("Getting track features...");
            getFeatures = new Thread(this::getTrackFeatures);
            getFeatures.start();
            getFeatures.join();

        }catch (Exception ex){

        }

        updateList = true;
        // So updateList thread has chance to execute listAll
        try{
            Thread.sleep(100);
        } catch (Exception ex){

        }
        gettingSongsFinished = true;
        playlistThreadRun = true;
        System.out.println("NUMBER OF SONGS: " + allSongs.size());
    }

    //OLD
    /*
        private void playlistIDToSongs() {

        //TESTING DELETE
        allSongs.clear();
        displaySongs.clear();
        saveLastPos = 0;

        int currentThreadNumber = threadNumber;

        gettingSongsFinished = false;

        getSongIDs threadSong;
        Thread getFeatures;
        Thread s = null;
        //int saveSize = 0;
        for (int i = 0; i < playlistIDs.size(); i++) {
            try {
                //Thread.sleep(100);
                if(currentThreadNumber != threadNumber){
                    playlistThreadRun = true;
                    Thread.currentThread().interrupt();
                    return;
                }
                threadSong = new getSongIDs(this, token, playlistIDs.get(i));
                s = new Thread(threadSong);
                System.out.println("Started thread: " + i);
                s.start();
                s.join();
                updateList = true; // For MainActivity to know when to update list.
                if(i % 3 == 0){
                    System.out.println("Getting track features...");
                    getFeatures = new Thread(this::getTrackFeatures);
                    getFeatures.start();
                    getFeatures.join();
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
        try{
            System.out.println("Getting track features...");
            getFeatures = new Thread(this::getTrackFeatures);
            getFeatures.start();
            getFeatures.join();
        }catch (Exception ex){

        }
        gettingSongsFinished = true;
        playlistThreadRun = true;
        System.out.println("NUMBER OF SONGS: " + allSongs.size());
    }

     */

    /**
     * Starts getTrackFeatures in a thread so network isn't on main thread.
     */
    public void getTrackFeaturesThreaded(){
        Thread t = new Thread(this::getTrackFeatures);
        t.start();
    }

    /**
     * Goes through allSongs Arraylist and gets each tracks features from the Spotify API.
     */
    public void getTrackFeatures() {
        boolean run = true;
        do {

            int count = saveLastPos;
            int bounds = saveLastPos + 100;
            String urlIDs = "";
            // Creates string of song IDs to send to API for track features.
            for (int song = saveLastPos; song < bounds; song++) {
                if (song < allSongs.size()) {
                    urlIDs += ",";
                    urlIDs += allSongs.get(song).getID();
                    saveLastPos++;
                } else {
                    run = false;
                    break;
                }
            }
            if(urlIDs == ""){
                break;
            }
            String fullOuputString = "";
            try {
                URL url = new URL("https://api.spotify.com/v1/audio-features/?ids=" + urlIDs.substring(1));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                if (conn.getResponseCode() != 200) {
                    System.out.println("ERROR: " + conn.getResponseCode() + " " + conn.getResponseMessage());
                    throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
                }

                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

                String output;
                while ((output = br.readLine()) != null) {
                    //System.out.println(output);
                    fullOuputString += output + "\n";
                }
                br.close();
                conn.disconnect();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                JSONObject parseDataObj = new JSONObject(fullOuputString);
                JSONArray audioFeatures = parseDataObj.getJSONArray("audio_features");

                JSONObject indSongs;

                for(int i = 0; i < audioFeatures.length(); i++){
                    indSongs = audioFeatures.getJSONObject(i);
                    allSongs.get(count).setDanceability(indSongs.getDouble("danceability"));
                    allSongs.get(count).setHappy(indSongs.getDouble("valence"));
                    allSongs.get(count).setEnergy(indSongs.getDouble("energy"));
                    count++;
                }

            }catch (Exception ex){

            }
        } while (run);
        System.out.println("All song data retrieved!...");
    }

    public void getCurrentDeviceThreaded(){
        Thread t = new Thread(this::getCurrentDevice);
        t.start();
    }

    private void getCurrentDevice(){

        String fullOuputString = "";
        try {
            URL url = new URL("https://api.spotify.com/v1/me/player/devices");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            if (conn.getResponseCode() != 200) {
                System.out.println(conn.getResponseMessage());
                //throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output;
            //System.out.println("Output from Server ....");
            while ((output = br.readLine()) != null) {
                //System.out.println(output);
                fullOuputString += output + "\n";
            }
            br.close();
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //System.out.println(fullOuputString);
        // Parse incoming Json for playlist ID's.
        try {

            JSONObject parseObj = new JSONObject(fullOuputString);
            //JSONObject mainObj = parseObj.getJSONObject("devices");
            JSONArray items = parseObj.getJSONArray("devices");

            JSONObject playlistObj;
            for(int i = 0; i < items.length(); i++) {
                playlistObj = items.getJSONObject(i);
                if(playlistObj.getString("is_active") == "true"){
                    currentPlayingDeviceID = playlistObj.getString("id");
                    break;
                    //System.out.println(playlistObj.getString("id"));
                }
            }
            //System.out.println("Test Output:");
            //System.out.println(playlistObj.getString("id"));
            /*
            JSONObject playlistObj;
            int amountOfPlaylists;
            if (items.length() > 5) {
                amountOfPlaylists = 5;
            } else {
                amountOfPlaylists = items.length();
            }
            for (int i = 0; i < items.length(); i++) {
                playlistObj = items.getJSONObject(i);
                playlistIDs.add(playlistObj.getString("id"));
            }
            */
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }



    }

    public boolean playOnCurrentDeviceThreaded(String ID){
        currentID = ID;
        playOnCurrentDeviceSucceeded = true;
        Thread t = new Thread(this::playOnCurrentDevice);
        t.start();
        try {
            t.join();
        }catch (Exception ex){
            System.out.println(ex.getMessage());
        }
        return playOnCurrentDeviceSucceeded;
    }

    private void playOnCurrentDevice(){

        String uriLink = "{\"uris\": [\"spotify:track:" + currentID + "\"]}";

        String fullOuputString = "";
        try {
            URL url = new URL("https://api.spotify.com/v1/me/player/play");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            //conn.setRequestProperty("Content-Type", "application/json");
            //conn.setRequestProperty("uris", uriLink);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            OutputStreamWriter out = new OutputStreamWriter(
                    conn.getOutputStream());
            out.write(uriLink);
            out.close();

            // This request uses a 204 as successful
            if (conn.getResponseCode() != 204) {
                System.out.println(conn.getResponseMessage());
                playOnCurrentDeviceSucceeded = false;
                conn.disconnect();
                String selectedLink = "https://open.spotify.com/track/" + currentID;
                Uri uri = Uri.parse(selectedLink);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                context.startActivity(intent);
                return;
                //throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }




            conn.disconnect();
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }



    public void addSongToUserLibraryThreaded(String ID){
        addSongID = ID;
        Thread s = new Thread(this::addSongToUserLibrary);
        s.start();
    }


    private void addSongToUserLibrary(){

        String idsLink = "?ids=" + addSongID;

        String fullOuputString = "";
        try {
            URL url = new URL("https://api.spotify.com/v1/me/tracks" + idsLink);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            if (conn.getResponseCode() != 200) {
                System.out.println(conn.getResponseMessage());
                conn.disconnect();
                return;
                //throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            conn.disconnect();
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    /**
     * Checks if each songs track features match the user's input in the UI.
     */
    public void checkAudioFeatures() {
        displaySongs.clear();
        if(!danceCheck && !happyCheck && !energyCheck){
            displaySongs = (ArrayList<songs>)allSongs.clone();
        }else {
            for (songs song : allSongs) {
                if ((this.dance <= (song.getDanceability() + tolerance) && this.dance >= (song.getDanceability() - tolerance)) || !danceCheck) {
                    if ((this.happy <= (song.getHappy() + tolerance) && this.happy >= (song.getHappy() - tolerance)) || !happyCheck) {
                        if ((this.energy <= (song.getEnergy() + tolerance) && this.energy >= (song.getEnergy() - tolerance)) || !energyCheck) {
                            displaySongs.add(song);
                        }
                    }
                }
            }
        }
    }

}
