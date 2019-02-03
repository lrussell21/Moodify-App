package com.russell.moodify;

import android.os.Build;
import android.support.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;


public class spotifyAPIFetcher {
    public ArrayList<songs> allSongs = new ArrayList<>();
    public ArrayList<songs> displaySongs = new ArrayList<>();
    public ArrayList<String> playlistIDs = new ArrayList<>();
    private String[] categories = {"pop", "mood", "edm_dance", "decades", "hiphop", "chill", "workout", "party"};
    //private String[] categories = {"pop", "mood", "edm_dance", "decades", "hiphop", "chill", "workout", "party", "focus", "sleep", "rock", "dinner", "jazz", "rnb", "romance", "indie_alt", "gaming", "soul", "classical"};

    private static final String client_id = "10187fa73dd54eb3833f69da476e6861";
    private static final String secret_id = "72bbadeb5355484db26245c552429326";
    private static String token;

    private static int saveLastPos = 0;

    public double dance, happy, energy;
    public boolean danceCheck = false;
    public boolean happyCheck = false;
    public boolean energyCheck = false;
    public double tolerance = 0.15;

    public spotifyAPIFetcher() { }

    /**
     * Gives Spotify client id and secret id and returns token.
     * @return token for api use.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean getToken() {

        String spoturl = "https://accounts.spotify.com/api/token";
        String urlParameters = "grant_type=client_credentials";
        String tokenStringReceive = "";
        try {

            URL url = new URL(spoturl + "?" + urlParameters);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((client_id + ":" + secret_id).getBytes()));


            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }


            try {
                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
                String output;
                //System.out.println("Output from Server ....");
                while ((output = br.readLine()) != null) {
                    tokenStringReceive += output;
                    //System.out.println(output);
                }
                setToken(tokenStringReceive);
            } catch (Exception e) {
                System.out.printf(e.getMessage());
            }
            conn.disconnect();
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Parses token json for token then sets token variable.
     * @param serverToken
     */
    private void setToken(String serverToken) {
        token = serverToken.substring(17, 100);
        System.out.println("TOKEN: " + token);
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
        getSongIDs threadSong;
        Thread getFeatures;
        Thread s = null;
        //int saveSize = 0;
        for (int i = 0; i < playlistIDs.size(); i++) {
            try {
                //saveSize = allSongs.size();
                Thread.sleep(100);
                threadSong = new getSongIDs(this, token, playlistIDs.get(i));
                s = new Thread(threadSong);
                System.out.println("Started thread: " + i);
                s.start();
                s.join();
                if(i % 10 == 0){
                    System.out.println("Getting track features...");
                    getFeatures = new Thread(this::getTrackFeatures);
                    getFeatures.start();
                    getFeatures.join();
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
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
            /*
            JsonParser jsonparser = new JsonParser();
            JsonElement jsonTree = jsonparser.parse(fullOuputString);
            JsonObject jsonObject = null;
            if (jsonTree.isJsonObject()) {
                jsonObject = jsonTree.getAsJsonObject();
            }

            JsonArray audioFeatures = jsonObject.get("audio_features").getAsJsonArray();
            JsonObject indSongs;
            String tempString = "";
            for (int songToSetFeature = 0; songToSetFeature < audioFeatures.size(); songToSetFeature++) {
                try {
                    indSongs = audioFeatures.get(songToSetFeature).getAsJsonObject();
                    allSongs.get(count).setDanceability(indSongs.get("danceability").getAsDouble());
                    allSongs.get(count).setHappy(indSongs.get("valence").getAsDouble());
                    allSongs.get(count).setEnergy(indSongs.get("energy").getAsDouble());
                    //System.out.println("----------------------------------------------");
                    //System.out.println("ID " + allSongs.get(count).getID());
                    //System.out.println("Dance " + indSongs.get("danceability").getAsDouble());
                    //System.out.println("Valence " + indSongs.get("valence").getAsDouble());
                    //System.out.println("Energy" + indSongs.get("energy").getAsDouble());
                    //System.out.println("----------------------------------------------");
                    count++;
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
            */
        } while (run);
        System.out.println("All song data retrieved!...");
    }

    /**
     * Checks if each songs track features match the user's input in the UI.
     */
    public void checkAudioFeatures() {
        displaySongs.clear();
        for (songs song : allSongs) {
            /*
            if(!danceCheck && !happyCheck && !energyCheck){
                displaySongs = (ArrayList<songs>)allSongs.clone();
                break;
            }
            */
            if ((this.dance <= (song.getDanceability() + tolerance) && this.dance >= (song.getDanceability() - tolerance))) {
                if ((this.happy <= (song.getHappy() + tolerance) && this.happy >= (song.getHappy() - tolerance))) {
                    if ((this.energy <= (song.getEnergy() + tolerance) && this.energy >= (song.getEnergy() - tolerance))) {
                        displaySongs.add(song);
                    }
                }
            }
        }
    }
}
