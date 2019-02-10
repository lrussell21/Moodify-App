package com.russell.moodify;

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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;

public class MainLoginScreen extends AppCompatActivity {


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if(checkRefreshToken()){
            startActivity(new Intent(MainLoginScreen.this, MainActivity.class));
        }

        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String spoturl = "https://accounts.spotify.com/authorize";
                String uri = "redirect_uri=moodify%3A%2F%2Flogincallback";
                String client_id = getResources().getString(R.string.client_id);
                String fullURL = spoturl + "?client_id=" + client_id + "&response_type=code&" + uri + "&scope=playlist-read-private%20user-library-read&state=34fFs29kd09";

                Uri urigo = Uri.parse(fullURL);
                Intent intent = new Intent(Intent.ACTION_VIEW,urigo);
                startActivity(intent);
            }
        });
    }



    private boolean checkRefreshToken(){
        String filename = "userCredentials";
        FileInputStream inputStream;
        StringBuffer datax = new StringBuffer("");
        try {
            inputStream = openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);

            String readString = br.readLine();
            while(readString != null){
                datax.append(readString);
                readString = br.readLine();
            }
            isr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(datax.length() > 1){
            return true;
        }else{
            return false;
        }

    }


}

