package com.companionspotify.backend.service;

import com.companionspotify.backend.entity.Artist;
import com.companionspotify.backend.entity.ListeningHistory;
import com.companionspotify.backend.entity.Playlist;
import com.companionspotify.backend.entity.PlaylistTrack;
import com.companionspotify.backend.entity.SpotifyAccount;
import com.companionspotify.backend.entity.Track;
import com.companionspotify.backend.repository.ArtistRepository;
import com.companionspotify.backend.repository.ListeningHistoryRepository;
import com.companionspotify.backend.repository.PlaylistRepository;
import com.companionspotify.backend.repository.PlaylistTrackRepository;
import com.companionspotify.backend.repository.SpotifyAccountRepository;
import com.companionspotify.backend.repository.TrackRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class SpotifySyncService {

        private final SpotifyOAuthService spotifyOAuthService;

        private final SpotifyAccountRepository spotifyAccountRepository;
        private final ArtistRepository artistRepository;
        private final TrackRepository trackRepository;
        private final PlaylistRepository playlistRepository;
        private final PlaylistTrackRepository playlistTrackRepository;
        private final ListeningHistoryRepository listeningHistoryRepository;

        public SpotifySyncService(
                        SpotifyOAuthService spotifyOAuthService,
                        SpotifyAccountRepository spotifyAccountRepository,
                        ArtistRepository artistRepository,
                        TrackRepository trackRepository,
                        PlaylistRepository playlistRepository,
                        PlaylistTrackRepository playlistTrackRepository,
                        ListeningHistoryRepository listeningHistoryRepository) {
                this.spotifyOAuthService = spotifyOAuthService;
                this.spotifyAccountRepository = spotifyAccountRepository;
                this.artistRepository = artistRepository;
                this.trackRepository = trackRepository;
                this.playlistRepository = playlistRepository;
                this.playlistTrackRepository = playlistTrackRepository;
                this.listeningHistoryRepository = listeningHistoryRepository;
        }

        public SpotifyAccount syncCurrentUser(
                        HttpSession session) {

                Map<String, Object> spotifyUser = spotifyOAuthService.getCurrentUser(session);

                String spotifyUserId = (String) spotifyUser.get("id");

                String displayName = (String) spotifyUser.get("display_name");

                SpotifyAccount account = spotifyAccountRepository
                                .findBySpotifyUserId(spotifyUserId)
                                .orElse(null);

                if (account == null) {
                        account = new SpotifyAccount(
                                        spotifyUserId,
                                        displayName);
                }

                account.setDisplayName(displayName);

                return spotifyAccountRepository.save(account);
        }

        public int syncTopArtists(
                        HttpSession session,
                        String timeRange) {

                syncCurrentUser(session);

                Map<String, Object> response = spotifyOAuthService.getTopArtists(
                                session,
                                timeRange);

                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

                int saved = 0;

                if (items == null) {
                        return 0;
                }

                for (Map<String, Object> item : items) {

                        String spotifyId = (String) item.get("id");

                        String name = (String) item.get("name");

                        Artist artist = artistRepository
                                        .findBySpotifyId(spotifyId)
                                        .orElse(null);

                        if (artist == null) {
                                artist = new Artist(
                                                spotifyId,
                                                name);
                        }

                        artist.setName(name);

                        artistRepository.save(artist);

                        saved++;
                }

                return saved;
        }

        public int syncTopTracks(
                        HttpSession session,
                        String timeRange) {

                syncCurrentUser(session);

                Map<String, Object> response = spotifyOAuthService.getTopTracks(
                                session,
                                timeRange);

                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

                int saved = 0;

                if (items == null) {
                        return 0;
                }

                for (Map<String, Object> item : items) {

                        saveTrack(item);

                        saved++;
                }

                return saved;
        }

        public int syncRecentlyPlayed(
                        HttpSession session) {

                SpotifyAccount account = syncCurrentUser(session);

                Map<String, Object> response = spotifyOAuthService.getRecentlyPlayed(
                                session);

                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

                int saved = 0;

                if (items == null) {
                        return 0;
                }

                for (Map<String, Object> item : items) {

                        Map<String, Object> trackData = (Map<String, Object>) item.get("track");

                        if (trackData == null) {
                                continue;
                        }

                        Track track = saveTrack(trackData);

                        String playedAtString = (String) item.get("played_at");

                        if (playedAtString == null) {
                                continue;
                        }

                        Instant playedAt = Instant.parse(playedAtString);

                        boolean alreadyExists = listeningHistoryRepository
                                        .existsBySpotifyAccountIdAndTrackIdAndPlayedAt(
                                                        account.getId(),
                                                        track.getId(),
                                                        playedAt);

                        if (!alreadyExists) {

                                ListeningHistory history = new ListeningHistory(
                                                account,
                                                track,
                                                playedAt);

                                listeningHistoryRepository.save(history);

                                saved++;
                        }
                }

                return saved;
        }

        public int syncPlaylists(
                        HttpSession session) {

                SpotifyAccount account = syncCurrentUser(session);

                List<Map<String, Object>> playlistItems = spotifyOAuthService.getAllPlaylists(
                                session);

                int saved = 0;

                for (Map<String, Object> playlistData : playlistItems) {

                        String spotifyPlaylistId = (String) playlistData.get("id");

                        String name = (String) playlistData.get("name");

                        Map<String, Object> externalUrls = (Map<String, Object>) playlistData.get("external_urls");

                        String spotifyUrl = null;

                        if (externalUrls != null) {
                                spotifyUrl = (String) externalUrls.get("spotify");
                        }

                        Playlist playlist = playlistRepository
                                        .findBySpotifyId(spotifyPlaylistId)
                                        .orElse(null);

                        if (playlist == null) {

                                playlist = new Playlist(
                                                spotifyPlaylistId,
                                                name,
                                                spotifyUrl,
                                                account);

                        } else {

                                playlist.setName(name);
                                playlist.setSpotifyUrl(spotifyUrl);
                                playlist.setSpotifyAccount(account);
                        }

                        playlist = playlistRepository.save(playlist);

                        System.out.println(
                                        "SYNC PLAYLIST: "
                                                        + playlist.getName()
                                                        + " | "
                                                        + playlist.getSpotifyId());

                        try {

                                syncPlaylistItems(
                                                session,
                                                playlist);

                                saved++;

                        } catch (Exception e) {

                                System.out.println(
                                                "SKIPPING PLAYLIST: "
                                                                + playlist.getName()
                                                                + " | "
                                                                + playlist.getSpotifyId()
                                                                + " | "
                                                                + e.getMessage());
                                                           }
                }

                return saved;
        }

        private void syncPlaylistItems(
                        HttpSession session,
                        Playlist playlist) {

                List<Map<String, Object>> items = spotifyOAuthService.getAllPlaylistItems(
                                session,
                                playlist.getSpotifyId());

                int position = 0;

                for (Map<String, Object> item : items) {

                        Map<String, Object> trackData = (Map<String, Object>) item.get("item");

                        if (trackData == null) {
                                continue;
                        }

                        Object trackType = trackData.get("type");

                        if (!"track".equals(trackType)) {
                                continue;
                        }

                        Track track = saveTrack(trackData);

                        boolean alreadyExists = playlistTrackRepository
                                        .existsByPlaylistIdAndTrackId(
                                                        playlist.getId(),
                                                        track.getId());

                        if (!alreadyExists) {

                                PlaylistTrack playlistTrack = new PlaylistTrack(
                                                playlist,
                                                track,
                                                position);

                                playlistTrackRepository.save(
                                                playlistTrack);
                        }

                        position++;
                }
        }

        private Track saveTrack(
                        Map<String, Object> trackData) {

                String spotifyTrackId = (String) trackData.get("id");

                String trackName = (String) trackData.get("name");

                List<Map<String, Object>> artists = (List<Map<String, Object>>) trackData.get("artists");

                if (artists == null || artists.isEmpty()) {
                        throw new IllegalStateException(
                                        "Track has no artist: " + trackName);
                }

                Map<String, Object> firstArtist = artists.get(0);

                String spotifyArtistId = (String) firstArtist.get("id");

                String artistName = (String) firstArtist.get("name");

                Artist artist = artistRepository
                                .findBySpotifyId(spotifyArtistId)
                                .orElse(null);

                if (artist == null) {

                        artist = new Artist(
                                        spotifyArtistId,
                                        artistName);
                }

                artist.setName(artistName);

                artist = artistRepository.save(artist);

                Track track = trackRepository
                                .findBySpotifyId(spotifyTrackId)
                                .orElse(null);

                if (track == null) {

                        track = new Track(
                                        spotifyTrackId,
                                        trackName,
                                        artist);

                } else {

                        track.setName(trackName);
                        track.setArtist(artist);
                }

                return trackRepository.save(track);
        }

        public List<ListeningHistory> getListeningHistory(
                        HttpSession session) {

                SpotifyAccount account = syncCurrentUser(session);

                return listeningHistoryRepository
                                .findBySpotifyAccountIdOrderByPlayedAtDesc(
                                                account.getId());
        }

        public List<Track> getTopTracks() {

                return trackRepository.findAll();
        }

        public List<Artist> getTopArtists() {

                return artistRepository.findAll();
        }
}