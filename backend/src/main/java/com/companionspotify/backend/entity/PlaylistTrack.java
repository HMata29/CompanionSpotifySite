package com.companionspotify.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "playlist_tracks",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"playlist_id", "track_id"}
                )
        }
)
public class PlaylistTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "track_position")
    private Integer trackPosition;

    public PlaylistTrack() {
    }

    public PlaylistTrack(
            Playlist playlist,
            Track track,
            Integer trackPosition
    ) {
        this.playlist = playlist;
        this.track = track;
        this.trackPosition = trackPosition;
    }

    public Long getId() {
        return id;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public Integer getTrackPosition() {
        return trackPosition;
    }

    public void setTrackPosition(Integer trackPosition) {
        this.trackPosition = trackPosition;
    }
}