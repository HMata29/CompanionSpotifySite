package com.companionspotify.backend.repository;

import com.companionspotify.backend.entity.TopArtist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopArtistRepository
        extends JpaRepository<TopArtist, Long> {

    List<TopArtist> findBySpotifyAccountIdAndTimeRange(
            Long spotifyAccountId,
            String timeRange);

    boolean existsBySpotifyAccountIdAndArtistIdAndTimeRange(
            Long spotifyAccountId,
            Long artistId,
            String timeRange);
}