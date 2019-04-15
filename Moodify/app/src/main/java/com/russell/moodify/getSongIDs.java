package com.russell.moodify;

import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class getSongIDs implements Runnable {

    private spotifyAPIFetcher s;
    private String token;
    private String playlistID;
    private songs newSong;

    /**
     * Initialize getSongsIDs object.
     * @param s Spotify api object.
     * @param token Spotify api token
     * @param list Spotify Playlist ID
     */
    public getSongIDs(spotifyAPIFetcher s, String token, String list){
        this.s = s;
        this.token = token;
        playlistID = list;
    }

    /**
     * For use with thread. Gets Spotify playlist ID and gets all the songs in the playlist.
     */
    public void run(){
            String fullOuputString = "";
            try {
                URL url = new URL("https://api.spotify.com/v1/playlists/" + playlistID + "/tracks");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);


                if (conn.getResponseCode() != 200) {
                    System.out.println("ERROR WITH PLAYLIST ID: " + playlistID);
                    System.out.println("Failed : HTTP error code : " + conn.getResponseCode() + " " + conn.getResponseMessage());
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

            // Parse Json received from Spotify
            parseData(fullOuputString);
    }

    /**
     * Parse Json with songs from Spotify.
     * @param data Json text.
     */
    public void parseData(String data){

        try {
            JSONObject parseDataObj = new JSONObject(data);
            JSONArray dataArray = parseDataObj.getJSONArray("items");
            JSONObject indTrack;
            JSONObject trackObj;
            JSONArray artistData;
            JSONObject artistObj;
            JSONObject coverArtObj;
            JSONArray coverArtArray;
            JSONObject coverArtLinkObj;
            JSONObject coverArtLinkObj2;

            for(int i = 0; i < dataArray.length(); i++){
                indTrack = dataArray.getJSONObject(i);

                trackObj = indTrack.getJSONObject("track");

                // Artist data
                artistData = trackObj.getJSONArray("artists");
                artistObj = artistData.getJSONObject(0);

                // Cover art album link
                coverArtObj = trackObj.getJSONObject("album");
                coverArtArray = coverArtObj.getJSONArray("images");
                coverArtLinkObj = coverArtArray.getJSONObject(1); // 300x300
                coverArtLinkObj2 = coverArtArray.getJSONObject(2);// 64x64

                newSong = new songs(s, trackObj.getString("id"), artistObj.getString("name"), trackObj.getString("name"), coverArtLinkObj.getString("url"), coverArtLinkObj2.getString("url"));

                System.out.println("CREATED NEW SONG OBJECT-----------------------" + i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
