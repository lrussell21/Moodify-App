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
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


public class spotifyAPIFetcher {
    public ArrayList<songs> allSongs = new ArrayList<>();
    public ArrayList<songs> displaySongs = new ArrayList<>();
    public ArrayList<String> playlistIDs = new ArrayList<>();
    public ArrayList<String> categoryIDs = new ArrayList<>();
    public ArrayList<String> categoryNames = new ArrayList<>();
    public String selectedCategory;

    private String[] categories = {"pop", "mood", "edm_dance", "decades", "hiphop", "chill", "workout", "party"};
    //private String[] categories = {"pop", "mood", "edm_dance", "decades", "hiphop", "chill", "workout", "party", "focus", "sleep", "rock", "dinner", "jazz", "rnb", "romance", "indie_alt", "gaming", "soul", "classical"};

    public String username = "";
    private final String client_id;
    private final String secret_id;
    private static String token;
    private String refreshToken;


    Context context;

    private static int saveLastPos = 0;

    public double dance, happy, energy;
    public boolean danceCheck = false;
    public boolean happyCheck = false;
    public boolean energyCheck = false;
    public volatile boolean playlistThreadRun = true;
    public volatile boolean gettingSongsFinished = false;
    public volatile boolean updateList = false;
    public double tolerance = 0.15;
    public int threadNumber = 0;

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
     * Creates a background thread to start gathering playlists.
     */
    public void getPlaylistIDsThreadedCategorySelected() {
        //playlistIDThreadedCategorySelected.interrupt();
        threadNumber++;
        if(allSongs.size() != 0) {
            playlistThreadRun = false;
            try {
                do {
                    Thread.sleep(50);
                } while (!playlistThreadRun);
            } catch (Exception ex) {

            }
        }
        Thread playlistIDThreadedCategorySelected = new Thread(this::getPlaylistIDsCategorySelected);
        playlistIDThreadedCategorySelected.start();
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
                if(items.length() > 5){
                    amountOfPlaylists = 5;
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
            if (items.length() > 5) {
                amountOfPlaylists = 5;
            } else {
                amountOfPlaylists = items.length();
            }
            for (int i = 0; i < items.length(); i++) {
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
    /*
    private void playlistIDToSongs() {

        //TESTING DELETE
        allSongs.clear();
        displaySongs.clear();

        getSongIDs threadSong;
        Thread getFeatures;
        Thread[] s = new Thread[5];
        //int saveSize = 0;
        System.out.println("Amount of Playlists: " + playlistIDs.size());
        for (int i = 0; i < 5; i++) {
            try {
                //Thread.sleep(100);
                if(playlistIDs.size() >= i) {


                    threadSong = new getSongIDs(this, token, playlistIDs.get(i));
                    s[i] = new Thread(threadSong);
                    System.out.println("Started thread: " + i);
                    s[i].start();
                }
                //updateList = true; // For MainActivity to know when to update list.
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
        try {
            for(int i = 0; i < 5; i++){
                s[i].join();
            }
            updateList = true; // For MainActivity to know when to update list.
            System.out.println("Getting track features...");
            getFeatures = new Thread(this::getTrackFeatures);
            getFeatures.start();
            getFeatures.join();
        }catch(Exception ex){

        }
        gettingSongsFinished = true;
        System.out.println("NUMBER OF SONGS: " + allSongs.size());
    }
*/


    private void playlistIDToSongs() {

        //TESTING DELETE
        allSongs.clear();
        displaySongs.clear();

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

    /**
     * Checks if each songs track features match the user's input in the UI.
     */
    public void checkAudioFeatures() {
        displaySongs.clear();
        for (songs song : allSongs) {
            if(!danceCheck && !happyCheck && !energyCheck){
                displaySongs = (ArrayList<songs>)allSongs.clone();
                break;
            }
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
