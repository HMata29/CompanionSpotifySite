package com.companionspotify.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "listening_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "spotify_account_id",
                                "track_id",
                                "played_at"
                        }
                )
        }
)
public class ListeningHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "spotify_account_id", nullable = false)
    private SpotifyAccount spotifyAccount;

    @ManyToOne
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    public ListeningHistory() {
    }

    public ListeningHistory(
            SpotifyAccount spotifyAccount,
            Track track,
            Instant playedAt
    ) {
        this.spotifyAccount = spotifyAccount;
        this.track = track;
        this.playedAt = playedAt;
    }

    public Long getId() {
        return id;
    }

    public SpotifyAccount getSpotifyAccount() {
        return spotifyAccount;
    }

    public void setSpotifyAccount(SpotifyAccount spotifyAccount) {
        this.spotifyAccount = spotifyAccount;
    }

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Instant playedAt) {
        this.playedAt = playedAt;
    }
}