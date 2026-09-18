package com.companionspotify.backend.repository;

import com.companionspotify.backend.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findBySpotifyId(String spotifyId);
}