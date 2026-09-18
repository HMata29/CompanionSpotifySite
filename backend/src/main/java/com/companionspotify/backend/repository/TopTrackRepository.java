package com.companionspotify.backend.repository;

import com.companionspotify.backend.entity.TopTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopTrackRepository
        extends JpaRepository<TopTrack, Long> {

    List<TopTrack> findBySpotifyAccountIdAndTimeRange(
            Long spotifyAccountId,
            String timeRange);

    boolean existsBySpotifyAccountIdAndTrackIdAndTimeRange(
            Long spotifyAccountId,
            Long trackId,
            String timeRange);
}