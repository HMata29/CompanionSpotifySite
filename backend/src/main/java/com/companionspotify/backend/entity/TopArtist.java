package com.companionspotify.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "top_artists", uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "spotify_account_id",
                "artist_id",
                "time_range"
        })
})
public class TopArtist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "spotify_account_id", nullable = false)
    private SpotifyAccount spotifyAccount;

    @ManyToOne
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Column(name = "time_range", nullable = false)
    private String timeRange;

    public TopArtist() {
    }

    public TopArtist(
            SpotifyAccount spotifyAccount,
            Artist artist,
            String timeRange) {
        this.spotifyAccount = spotifyAccount;
        this.artist = artist;
        this.timeRange = timeRange;
    }

    public Long getId() {
        return id;
    }

    public SpotifyAccount getSpotifyAccount() {
        return spotifyAccount;
    }

    public void setSpotifyAccount(
            SpotifyAccount spotifyAccount) {
        this.spotifyAccount = spotifyAccount;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(
            Artist artist) {
        this.artist = artist;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(
            String timeRange) {
        this.timeRange = timeRange;
    }
}