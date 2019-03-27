package com.russell.moodify;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

public class createSpotifyAPIFetcher implements Runnable {
    spotifyAPIFetcher s;
    public String userCode  = "";
    private boolean tokenSuccess;

    public createSpotifyAPIFetcher(spotifyAPIFetcher s){
        this.s = s;
        tokenSuccess = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void run(){

        if(s.checkRefreshToken()) {
            tokenSuccess = true;
        } else {
            if (s.loginUser(userCode)) {
                tokenSuccess = true;
            } else {
                tokenSuccess = false;
            }
        }
    }

    public boolean getTokenStatus(){
        return tokenSuccess;
    }

}
