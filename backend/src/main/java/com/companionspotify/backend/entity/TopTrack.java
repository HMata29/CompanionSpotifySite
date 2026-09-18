package com.companionspotify.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "top_tracks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "spotify_account_id",
                "track_id",
                "time_range"
        })
})
public class TopTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "spotify_account_id", nullable = false)
    private SpotifyAccount spotifyAccount;

    @ManyToOne
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "time_range", nullable = false)
    private String timeRange;

    public TopTrack() {
    }

    public TopTrack(
            SpotifyAccount spotifyAccount,
            Track track,
            String timeRange) {
        this.spotifyAccount = spotifyAccount;
        this.track = track;
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

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }
}