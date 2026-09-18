package com.companionspotify.backend.service;

import com.companionspotify.backend.entity.Artist;
import com.companionspotify.backend.entity.ListeningHistory;
import com.companionspotify.backend.entity.Playlist;
import com.companionspotify.backend.entity.PlaylistTrack;
import com.companionspotify.backend.entity.SpotifyAccount;
import com.companionspotify.backend.entity.Track;
import com.companionspotify.backend.entity.TopTrack;
import com.companionspotify.backend.entity.TopArtist;
import com.companionspotify.backend.repository.TopArtistRepository;
import com.companionspotify.backend.repository.TopTrackRepository;
import com.companionspotify.backend.repository.ArtistRepository;
import com.companionspotify.backend.repository.ListeningHistoryRepository;
import com.companionspotify.backend.repository.PlaylistRepository;
import com.companionspotify.backend.repository.PlaylistTrackRepository;
import com.companionspotify.backend.repository.SpotifyAccountRepository;
import com.companionspotify.backend.repository.TrackRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import com.companionspotify.backend.entity.TopArtist;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SpotifySyncService {

        private final SpotifyOAuthService spotifyOAuthService;

        private final SpotifyAccountRepository spotifyAccountRepository;
        private final ArtistRepository artistRepository;
        private final TrackRepository trackRepository;
        private final PlaylistRepository playlistRepository;
        private final PlaylistTrackRepository playlistTrackRepository;
        private final ListeningHistoryRepository listeningHistoryRepository;
        private final TopTrackRepository topTrackRepository;
        private final TopArtistRepository topArtistRepository;

        private volatile boolean playlistSyncRunning = false;

        private final AtomicInteger playlistSyncProcessed = new AtomicInteger(0);

        private final AtomicInteger playlistSyncTotal = new AtomicInteger(0);

        private final AtomicInteger playlistSyncFailed = new AtomicInteger(0);

        public SpotifySyncService(
                        SpotifyOAuthService spotifyOAuthService,
                        SpotifyAccountRepository spotifyAccountRepository,
                        ArtistRepository artistRepository,
                        TrackRepository trackRepository,
                        PlaylistRepository playlistRepository,
                        PlaylistTrackRepository playlistTrackRepository,
                        ListeningHistoryRepository listeningHistoryRepository,
                        TopTrackRepository topTrackRepository,
                        TopArtistRepository topArtistRepository) {
                this.spotifyOAuthService = spotifyOAuthService;
                this.spotifyAccountRepository = spotifyAccountRepository;
                this.artistRepository = artistRepository;
                this.trackRepository = trackRepository;
                this.playlistRepository = playlistRepository;
                this.playlistTrackRepository = playlistTrackRepository;
                this.listeningHistoryRepository = listeningHistoryRepository;
                this.topTrackRepository = topTrackRepository;
                this.topArtistRepository = topArtistRepository;
        }

        public SpotifyAccount syncCurrentUser(
                        HttpSession session) {

                Map<String, Object> spotifyUser = spotifyOAuthService.getCurrentUser(session);

                return saveSpotifyAccount(spotifyUser);
        }

        private SpotifyAccount syncCurrentUser(
                        String accessToken) {

                Map<String, Object> spotifyUser = spotifyOAuthService.getCurrentUser(accessToken);

                return saveSpotifyAccount(spotifyUser);
        }

        private SpotifyAccount saveSpotifyAccount(
                        Map<String, Object> spotifyUser) {

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

                SpotifyAccount account = syncCurrentUser(session);

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

                        String imageUrl = null;

                        List<Map<String, Object>> images = (List<Map<String, Object>>) item.get("images");

                        if (images != null && !images.isEmpty()) {

                                Map<String, Object> firstImage = images.get(0);

                                imageUrl = (String) firstImage.get("url");
                        }

                        Artist artist = artistRepository
                                        .findBySpotifyId(spotifyId)
                                        .orElse(null);

                        if (artist == null) {

                                artist = new Artist(
                                                spotifyId,
                                                name);
                        }

                        artist.setName(name);
                        artist.setImageUrl(imageUrl);

                        artist = artistRepository.save(artist);

                        boolean alreadyExists = topArtistRepository
                                        .existsBySpotifyAccountIdAndArtistIdAndTimeRange(
                                                        account.getId(),
                                                        artist.getId(),
                                                        timeRange);

                        if (!alreadyExists) {

                                TopArtist topArtist = new TopArtist(
                                                account,
                                                artist,
                                                timeRange);

                                topArtistRepository.save(topArtist);

                                saved++;
                        }
                }

                return saved;
        }

        public int syncTopTracks(
                        HttpSession session,
                        String timeRange) {

                SpotifyAccount account = syncCurrentUser(session);

                Map<String, Object> response = spotifyOAuthService.getTopTracks(
                                session,
                                timeRange);

                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

                int saved = 0;

                if (items == null) {
                        return 0;
                }

                for (Map<String, Object> item : items) {

                        Track track = saveTrack(item);

                        boolean alreadyExists = topTrackRepository
                                        .existsBySpotifyAccountIdAndTrackIdAndTimeRange(
                                                        account.getId(),
                                                        track.getId(),
                                                        timeRange);

                        if (!alreadyExists) {

                                TopTrack topTrack = new TopTrack(
                                                account,
                                                track,
                                                timeRange);

                                topTrackRepository.save(topTrack);

                                saved++;
                        }
                }

                return saved;
        }

        @SuppressWarnings("unchecked")
        public int syncRecentlyPlayed(
                        HttpSession session) {

                SpotifyAccount account = syncCurrentUser(session);

                String accessToken = spotifyOAuthService.getAccessToken(session);

                String url = "https://api.spotify.com/v1/me/player/recently-played"
                                + "?limit=50";

                int pages = 0;
                int maxPages = 5;

                int saved = 0;

                while (url != null && pages < maxPages) {

                        Map<String, Object> response = spotifyGet(accessToken, url);

                        if (response == null) {
                                break;
                        }

                        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

                        if (items == null || items.isEmpty()) {
                                break;
                        }

                        for (Map<String, Object> item : items) {

                                Map<String, Object> trackData = (Map<String, Object>) item.get("track");

                                if (trackData == null) {
                                        continue;
                                }

                                String playedAtString = (String) item.get("played_at");

                                if (playedAtString == null) {
                                        continue;
                                }

                                Instant playedAt = Instant.parse(playedAtString);

                                Track track = saveTrack(trackData);

                                boolean alreadyExists = listeningHistoryRepository
                                                .existsBySpotifyAccountIdAndTrackIdAndPlayedAt(
                                                                account.getId(),
                                                                track.getId(),
                                                                playedAt);

                                if (alreadyExists) {
                                        continue;
                                }

                                ListeningHistory history = new ListeningHistory(
                                                account,
                                                track,
                                                playedAt);

                                listeningHistoryRepository.save(history);

                                saved++;
                        }

                        Map<String, Object> cursors = (Map<String, Object>) response.get("cursors");

                        String before = cursors != null
                                        ? (String) cursors.get("before")
                                        : null;

                        if (before == null) {
                                break;
                        }

                        url = "https://api.spotify.com/v1/me/player/recently-played"
                                        + "?before="
                                        + before
                                        + "&limit=50";

                        pages++;
                }

                System.out.println(
                                "TOTALE NUOVI LISTENING HISTORY SALVATI: "
                                                + saved);

                return saved;
        }

        public void syncPlaylistsAsync(
                        HttpSession session) {

                if (playlistSyncRunning) {
                        return;
                }

                String accessToken = spotifyOAuthService.getAccessToken(session);

                playlistSyncRunning = true;
                playlistSyncProcessed.set(0);
                playlistSyncTotal.set(0);
                playlistSyncFailed.set(0);

                Thread.startVirtualThread(() -> {

                        try {

                                syncPlaylists(accessToken);

                        } catch (Exception e) {

                                System.out.println(
                                                "PLAYLIST SYNC FAILED: "
                                                                + e.getClass().getSimpleName()
                                                                + " | "
                                                                + e.getMessage());

                        } finally {

                                playlistSyncRunning = false;
                        }
                });
        }

        public int syncPlaylists(
                        HttpSession session) {

                String accessToken = spotifyOAuthService.getAccessToken(session);

                return syncPlaylists(accessToken);
        }

        private int syncPlaylists(
                        String accessToken) {

                SpotifyAccount account = syncCurrentUser(accessToken);

                List<Map<String, Object>> playlistItems = spotifyOAuthService.getAllPlaylists(
                                accessToken);

                playlistSyncTotal.set(
                                playlistItems.size());

                int saved = 0;

                for (Map<String, Object> playlistData : playlistItems) {

                        String spotifyPlaylistId = (String) playlistData.get("id");

                        String name = (String) playlistData.get("name");

                        String snapshotId = (String) playlistData.get("snapshot_id");

                        Map<String, Object> externalUrls = (Map<String, Object>) playlistData.get("external_urls");

                        String spotifyUrl = null;

                        if (externalUrls != null) {

                                spotifyUrl = (String) externalUrls.get("spotify");
                        }

                        Playlist playlist = playlistRepository
                                        .findBySpotifyId(spotifyPlaylistId)
                                        .orElse(null);

                        boolean isNewPlaylist = playlist == null;

                        boolean playlistChanged = true;

                        if (playlist != null) {

                                playlistChanged = snapshotId == null
                                                || !snapshotId.equals(
                                                                playlist.getSnapshotId());
                        }

                        if (playlist == null) {

                                playlist = new Playlist(
                                                spotifyPlaylistId,
                                                name,
                                                spotifyUrl,
                                                snapshotId,
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
                                                        + playlist.getSpotifyId()
                                                        + " | changed="
                                                        + playlistChanged);

                        if (!isNewPlaylist && !playlistChanged) {

                                playlistSyncProcessed.incrementAndGet();

                                System.out.println(
                                                "SKIP PLAYLIST: "
                                                                + playlist.getName()
                                                                + " | snapshot unchanged");

                                System.out.println(
                                                "PLAYLIST SYNC PROGRESS: "
                                                                + playlistSyncProcessed.get()
                                                                + "/"
                                                                + playlistSyncTotal.get());

                                continue;
                        }

                        try {

                                syncPlaylistItems(
                                                accessToken,
                                                playlist);

                                playlist.setSnapshotId(snapshotId);

                                playlistRepository.save(playlist);

                                saved++;

                        } catch (Exception e) {

                                playlistSyncFailed.incrementAndGet();

                                System.out.println(
                                                "SKIPPING PLAYLIST: "
                                                                + playlist.getName()
                                                                + " | "
                                                                + playlist.getSpotifyId()
                                                                + " | "
                                                                + e.getClass().getSimpleName()
                                                                + " | "
                                                                + e.getMessage());
                        }

                        playlistSyncProcessed.incrementAndGet();

                        System.out.println(
                                        "PLAYLIST SYNC PROGRESS: "
                                                        + playlistSyncProcessed.get()
                                                        + "/"
                                                        + playlistSyncTotal.get());
                }

                return saved;
        }

        private void syncPlaylistItems(
                        String accessToken,
                        Playlist playlist) {

                List<Map<String, Object>> items = spotifyOAuthService.getAllPlaylistItems(
                                accessToken,
                                playlist.getSpotifyId());

                /*
                 * Keep track of all Spotify track IDs that are
                 * currently present in the playlist.
                 */
                Set<Long> currentTrackIds = new HashSet<>();

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

                        String spotifyTrackId = (String) trackData.get("id");

                        if (spotifyTrackId == null) {
                                continue;
                        }

                        Track track = saveTrack(trackData);

                        currentTrackIds.add(track.getId());

                        PlaylistTrack playlistTrack = playlistTrackRepository
                                        .findByPlaylistId(
                                                        playlist.getId())
                                        .stream()
                                        .filter(existing -> existing.getTrack()
                                                        .getId()
                                                        .equals(track.getId()))
                                        .findFirst()
                                        .orElse(null);

                        if (playlistTrack == null) {

                                playlistTrack = new PlaylistTrack(
                                                playlist,
                                                track,
                                                position);

                        } else {

                                playlistTrack.setTrackPosition(
                                                position);
                        }

                        playlistTrackRepository.save(
                                        playlistTrack);

                        position++;
                }

                /*
                 * Remove tracks that are no longer present
                 * in the Spotify playlist.
                 */
                List<PlaylistTrack> existingTracks = playlistTrackRepository
                                .findByPlaylistId(
                                                playlist.getId());

                for (PlaylistTrack existingTrack : existingTracks) {

                        if (!currentTrackIds.contains(
                                        existingTrack.getTrack().getId())) {

                                playlistTrackRepository.delete(
                                                existingTrack);
                        }
                }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> spotifyGet(
                        String accessToken,
                        String uri) {

                org.springframework.web.client.RestClient restClient = org.springframework.web.client.RestClient
                                .create();

                return restClient.get()
                                .uri(uri)
                                .headers(headers -> headers.setBearerAuth(accessToken))
                                .retrieve()
                                .body(Map.class);
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

                /*
                 * Get artist image from Spotify.
                 */
                String artistImageUrl = null;

                List<Map<String, Object>> artistImages = (List<Map<String, Object>>) firstArtist.get("images");

                if (artistImages != null && !artistImages.isEmpty()) {

                        Map<String, Object> firstArtistImage = artistImages.get(0);

                        artistImageUrl = (String) firstArtistImage.get("url");
                }

                Artist artist = artistRepository
                                .findBySpotifyId(spotifyArtistId)
                                .orElse(null);

                if (artist == null) {

                        artist = new Artist(
                                        spotifyArtistId,
                                        artistName);
                }

                artist.setName(artistName);

                if (artistImageUrl != null) {
                        artist.setImageUrl(artistImageUrl);
                }

                artist = artistRepository.save(artist);

                /*
                 * Get album cover from Spotify.
                 */
                String trackImageUrl = null;

                Map<String, Object> album = (Map<String, Object>) trackData.get("album");

                if (album != null) {

                        List<Map<String, Object>> albumImages = (List<Map<String, Object>>) album.get("images");

                        if (albumImages != null && !albumImages.isEmpty()) {

                                Map<String, Object> firstAlbumImage = albumImages.get(0);

                                trackImageUrl = (String) firstAlbumImage.get("url");
                        }
                }

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

                if (trackImageUrl != null) {
                        track.setImageUrl(trackImageUrl);
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

        public List<TopTrack> getTopTracks(
                        HttpSession session,
                        String timeRange) {

                SpotifyAccount account = syncCurrentUser(session);

                return topTrackRepository
                                .findBySpotifyAccountIdAndTimeRange(
                                                account.getId(),
                                                timeRange);
        }

        public List<TopArtist> getTopArtists(
                        HttpSession session,
                        String timeRange) {

                SpotifyAccount account = syncCurrentUser(session);

                return topArtistRepository
                                .findBySpotifyAccountIdAndTimeRange(
                                                account.getId(),
                                                timeRange);
        }

        public Map<String, Object> getPlaylistSyncStatus() {

                String status = playlistSyncRunning
                                ? "RUNNING"
                                : "IDLE";

                return Map.of(
                                "status", status,
                                "processed", playlistSyncProcessed.get(),
                                "total", playlistSyncTotal.get(),
                                "failed", playlistSyncFailed.get());
        }
}