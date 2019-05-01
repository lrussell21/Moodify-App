package com.russell.moodify;

public class songList {
    private String songName;
    private String songArtist;
    private String albumUrl;
    private String songID;

    public songList(String songName, String songArtist, String albumUrl, String ID) {
        this.songName = songName;
        this.songArtist = songArtist;
        this.albumUrl = albumUrl;
        this.songID = ID;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getSongArtist() {
        return songArtist;
    }

    public void setSongArtist(String songArtist) {
        this.songArtist = songArtist;
    }

    public String getAlbumUrl() {
        return albumUrl;
    }

    public void setAlbumUrl(String albumUrl) {
        this.albumUrl = albumUrl;
    }

    public String getSongID(){
        return songID;
    }
}
