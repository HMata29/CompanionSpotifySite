package com.companionspotify.backend.repository;

import com.companionspotify.backend.entity.ListeningHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ListeningHistoryRepository
                extends JpaRepository<ListeningHistory, Long> {

        List<ListeningHistory> findBySpotifyAccountIdOrderByPlayedAtDesc(
                        Long spotifyAccountId);

        boolean existsBySpotifyAccountIdAndTrackIdAndPlayedAt(
                        Long spotifyAccountId,
                        Long trackId,
                        Instant playedAt);

        boolean existsBySpotifyAccountIdAndTrackSpotifyId(
                        Long spotifyAccountId,
                        String spotifyId);
}