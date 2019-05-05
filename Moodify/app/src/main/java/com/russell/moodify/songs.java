package com.russell.moodify;

public class songs {
    private double danceability = -1.0, happy = -1.0, energy = -1.0;
    private String artist;
    private String songName;
    private String ID;
    private String coverartLink;
    private String coverartLinkLight;

    /**
     * Initialize song object with specified values.
     * @param ID Song's ID.
     * @param artist Song's artist.
     * @param songName Song's name.
     * @param coverartLink Link to song's album cover art.
     */
    public songs(String ID, String artist, String songName, String coverartLink, String coverartLinkLight){
        this.ID = ID;
        this.artist = artist;
        this.songName = songName;
        this.coverartLink = coverartLink;
        this.coverartLinkLight = coverartLinkLight;
    }

    /**
     * Gets song artist.
     * @return string of artist name.
     */
    public String getArtist() {
        return artist;
    }

    /**
     * Get song name.
     * @return string of song's name.
     */
    public String getSongName() {
        return songName;
    }

    /**
     * Get song's Spotify ID.
     * @return string with song's Spotify ID.
     */
    public String getID() {
        return ID;
    }

    /**
     * Get song's album art link 300x300.
     * @return string of link to cover art.
     */
    public String getCoverartLink() {
        return coverartLink;
    }

    /**
     * Get song's album art link 64x64.
     * @return string of link to cover art.
     */
    public String getCoverartLinkLight() {
        return coverartLinkLight;
    }

    /**
     * Get danceability of track.
     * @return double of song's danceability.
     */
    public double getDanceability() {
        return danceability;
    }

    /**
     * Get how happy the track is.
     * @return double of song's happiness.
     */
    public double getHappy() {
        return happy;
    }

    /**
     * Get how energetic a song is.
     * @return double of how energetic the track is.
     */
    public double getEnergy() {
        return energy;
    }

    /**
     * Set song's danceability.
     * @param danceability double of how danceable the song is (0.0 - 1.0)/
     */
    public void setDanceability(double danceability) {
        this.danceability = danceability;
    }

    /**
     * Set song's happiness.
     * @param happy  double of how happy the song is (0.0 - 1.0)/
     */
    public void setHappy(double happy) {
        this.happy = happy;
    }

    /**
     * Set song's energy.
     * @param energy  double of how energetic the song is (0.0 - 1.0)/
     */
    public void setEnergy(double energy) {
        this.energy = energy;
    }

    /**
     * Returns a string of songs details.
     * @return a string of songs details.
     */
    public String toString(){
        return artist + " - " + songName + " : " + ID + "\n" + "Dance: " + danceability + "\n" + "Happy: " + happy + "\n" + "Energy: " + energy;
    }

}
