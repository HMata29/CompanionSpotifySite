package com.companionspotify.backend.repository;

import com.companionspotify.backend.entity.PlaylistTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistTrackRepository
        extends JpaRepository<PlaylistTrack, Long> {

                boolean existsByPlaylistIdAndTrackId(
                        Long playlistId,
                        Long trackId              );
}

