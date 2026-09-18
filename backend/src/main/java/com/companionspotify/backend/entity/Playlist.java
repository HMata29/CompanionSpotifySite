package com.companionspotify.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "playlists")
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spotify_id", nullable = false, unique = true)
    private String spotifyId;

    @Column(nullable = false)
    private String name;

    @Column(name = "spotify_url")
    private String spotifyUrl;

    @Column(name = "snapshot_id")
    private String snapshotId;

    @ManyToOne
    @JoinColumn(name = "spotify_account_id", nullable = false)
    private SpotifyAccount spotifyAccount;

    public Playlist() {
    }

    public Playlist(
            String spotifyId,
            String name,
            String spotifyUrl,
            String snapshotId,
            SpotifyAccount spotifyAccount) {
        this.spotifyId = spotifyId;
        this.name = name;
        this.spotifyUrl = spotifyUrl;
        this.snapshotId = snapshotId;
        this.spotifyAccount = spotifyAccount;
    }

    public Long getId() {
        return id;
    }

    public String getSpotifyId() {
        return spotifyId;
    }

    public void setSpotifyId(String spotifyId) {
        this.spotifyId = spotifyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpotifyUrl() {
        return spotifyUrl;
    }

    public void setSpotifyUrl(String spotifyUrl) {
        this.spotifyUrl = spotifyUrl;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public SpotifyAccount getSpotifyAccount() {
        return spotifyAccount;
    }

    public void setSpotifyAccount(SpotifyAccount spotifyAccount) {
        this.spotifyAccount = spotifyAccount;
    }
}