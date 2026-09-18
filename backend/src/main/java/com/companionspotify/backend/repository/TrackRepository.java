package com.companionspotify.backend.repository;

import com.companionspotify.backend.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {

    Optional<Track> findBySpotifyId(String spotifyId);
}