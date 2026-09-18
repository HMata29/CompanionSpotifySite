package com.companionspotify.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "spotify_accounts")
public class SpotifyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spotify_user_id", nullable = false, unique = true)
    private String spotifyUserId;

    @Column(name = "display_name")
    private String displayName;

    public SpotifyAccount() {
    }

    public SpotifyAccount(String spotifyUserId, String displayName) {
        this.spotifyUserId = spotifyUserId;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public String getSpotifyUserId() {
        return spotifyUserId;
    }

    public void setSpotifyUserId(String spotifyUserId) {
        this.spotifyUserId = spotifyUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}