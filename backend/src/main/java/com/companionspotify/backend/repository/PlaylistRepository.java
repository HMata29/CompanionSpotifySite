package com.companionspotify.backend.repository;

import com.companionspotify.backend.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    Optional<Playlist> findBySpotifyId(String spotifyId);
}