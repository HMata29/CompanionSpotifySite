package com.companionspotify.backend.service;

import com.companionspotify.backend.entity.SpotifyAccount;
import com.companionspotify.backend.repository.ListeningHistoryRepository;
import com.companionspotify.backend.repository.SpotifyAccountRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DiscoveryService {

    private static final String SPOTIFY_API_URL = "https://api.spotify.com/v1";

    private final SpotifyOAuthService spotifyOAuthService;
    private final ListeningHistoryRepository listeningHistoryRepository;
    private final SpotifyAccountRepository spotifyAccountRepository;

    public DiscoveryService(
            SpotifyOAuthService spotifyOAuthService,
            ListeningHistoryRepository listeningHistoryRepository,
            SpotifyAccountRepository spotifyAccountRepository) {

        this.spotifyOAuthService = spotifyOAuthService;
        this.listeningHistoryRepository = listeningHistoryRepository;
        this.spotifyAccountRepository = spotifyAccountRepository;
    }

    public List<Map<String, Object>> getDiscoveryCandidates(
            HttpSession session) {

        String accessToken = spotifyOAuthService.getAccessToken(session);

        /*
         * Get the Spotify user from the current session.
         */
        Map<String, Object> currentUser = spotifyOAuthService.getCurrentUser(session);

        if (currentUser == null || currentUser.get("id") == null) {
            return new ArrayList<>();
        }

        String spotifyUserId = (String) currentUser.get("id");

        /*
         * Find the corresponding SpotifyAccount in the database.
         */
        SpotifyAccount account = spotifyAccountRepository
                .findBySpotifyUserId(spotifyUserId)
                .orElse(null);

        if (account == null) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> candidates = new ArrayList<>();

        /*
         * Get the user's medium-term top tracks.
         */
        Map<String, Object> topTracks = spotifyOAuthService.getTopTracks(
                session,
                "medium_term");

        List<Map<String, Object>> trackItems = (List<Map<String, Object>>) topTracks.get("items");

        if (trackItems == null) {
            return candidates;
        }

        /*
         * Keep track of artists we have already searched.
         */
        Set<String> processedArtists = new HashSet<>();

        /*
         * Keep track of candidate Spotify IDs already added.
         */
        Set<String> addedTrackIds = new HashSet<>();

        /*
         * Use the user's top artists as discovery seeds.
         */
        for (Map<String, Object> track : trackItems) {

            List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");

            if (artists == null || artists.isEmpty()) {
                continue;
            }

            Map<String, Object> artist = artists.get(0);

            String artistName = (String) artist.get("name");

            if (artistName == null || artistName.isBlank()) {
                continue;
            }

            /*
             * Skip artists that we have already searched.
             */
            if (!processedArtists.add(artistName)) {
                continue;
            }

            Map<String, Object> searchResponse = searchArtistTracks(
                    accessToken,
                    artistName);

            if (searchResponse == null) {
                continue;
            }

            Map<String, Object> tracks = (Map<String, Object>) searchResponse.get("tracks");

            if (tracks == null) {
                continue;
            }

            List<Map<String, Object>> searchItems = (List<Map<String, Object>>) tracks.get("items");

            if (searchItems == null) {
                continue;
            }

            /*
             * Filter discovery candidates.
             */
            for (Map<String, Object> candidate : searchItems) {

                String spotifyTrackId = (String) candidate.get("id");

                if (spotifyTrackId == null) {
                    continue;
                }

                /*
                 * Skip tracks already present in the user's
                 * listening history.
                 */
                if (listeningHistoryRepository
                        .existsBySpotifyAccountIdAndTrackSpotifyId(
                                account.getId(),
                                spotifyTrackId)) {

                    continue;
                }

                /*
                 * Avoid duplicate candidates.
                 */
                if (!addedTrackIds.add(spotifyTrackId)) {
                    continue;
                }

                candidates.add(candidate);
            }
        }

        return candidates;
    }

    private Map<String, Object> searchArtistTracks(
            String accessToken,
            String artistName) {

        String encodedArtist = java.net.URLEncoder.encode(
                artistName,
                java.nio.charset.StandardCharsets.UTF_8);

        String uri = SPOTIFY_API_URL
                + "/search"
                + "?q=artist:"
                + encodedArtist
                + "&type=track"
                + "&limit=10";

        return spotifyGet(
                accessToken,
                uri);
    }

    private Map<String, Object> spotifyGet(
            String accessToken,
            String uri) {

        /*
         * Temporary implementation.
         *
         * We will move the generic Spotify GET operation
         * into SpotifyOAuthService once the discovery
         * flow is working.
         */

        org.springframework.web.client.RestClient restClient = org.springframework.web.client.RestClient.create();

        return restClient.get()
                .uri(uri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(Map.class);
    }
}