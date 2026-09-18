package com.companionspotify.backend.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class SpotifyOAuthService {

        private static final String AUTHORIZE_URL = "https://accounts.spotify.com/authorize";

        private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

        private static final String SPOTIFY_API_URL = "https://api.spotify.com/v1";

        private static final String STATE_SESSION_KEY = "spotify_oauth_state";

        private static final String ACCESS_TOKEN_SESSION_KEY = "spotify_access_token";

        private static final String REFRESH_TOKEN_SESSION_KEY = "spotify_refresh_token";

        private static final int MAX_RETRIES = 3;

        private static final long DEFAULT_RETRY_DELAY_MS = 2000;

        private final RestClient restClient = RestClient.create();


        @Value("${spotify.client-id}")
        private String clientId;

        @Value("${spotify.client-secret}")
        private String clientSecret;

        @Value("${spotify.redirect-uri}")
        private String redirectUri;

        public String buildAuthorizationUrl(HttpSession session) {

                String state = generateState();

                session.setAttribute(
                                STATE_SESSION_KEY,
                                state);

                String scope = String.join(" ",
                                "user-read-private",
                                "user-top-read",
                                "user-read-recently-played",
                                "playlist-read-private",
                                "playlist-read-collaborative");

                return AUTHORIZE_URL
                                + "?response_type=code"
                                + "&client_id=" + encode(clientId)
                                + "&scope=" + encode(scope)
                                + "&redirect_uri=" + encode(redirectUri)
                                + "&state=" + encode(state);
        }

        public Map<String, Object> exchangeCodeForToken(
                        String code,
                        String state,
                        HttpSession session) {

                String expectedState = (String) session.getAttribute(
                                STATE_SESSION_KEY);

                if (expectedState == null || !expectedState.equals(state)) {

                        throw new IllegalArgumentException(
                                        "Invalid OAuth state");
                }

                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

                form.add(
                                "grant_type",
                                "authorization_code");

                form.add(
                                "code",
                                code);

                form.add(
                                "redirect_uri",
                                redirectUri);

                Map<String, Object> response = restClient.post()
                                .uri(TOKEN_URL)
                                .contentType(
                                                MediaType.APPLICATION_FORM_URLENCODED)
                                .headers(headers -> headers.setBasicAuth(
                                                clientId,
                                                clientSecret,
                                                StandardCharsets.UTF_8))
                                .body(form)
                                .retrieve()
                                .body(Map.class);

                if (response == null
                                || response.get("access_token") == null) {

                        throw new IllegalStateException(
                                        "Spotify did not return an access token");
                }

                session.setAttribute(
                                ACCESS_TOKEN_SESSION_KEY,
                                response.get("access_token"));

                if (response.get("refresh_token") != null) {

                        session.setAttribute(
                                        REFRESH_TOKEN_SESSION_KEY,
                                        response.get("refresh_token"));
                }

                session.removeAttribute(
                                STATE_SESSION_KEY);

                return response;
        }

        public Map<String, Object> getCurrentUser(
                        HttpSession session) {

                return executeGet(
                                getAccessToken(session),
                                SPOTIFY_API_URL + "/me");
        }

        public Map<String, Object> getCurrentUser(
                        String accessToken) {

                return executeGet(
                                accessToken,
                                SPOTIFY_API_URL + "/me");
        }

        public Map<String, Object> getTopTracks(
                        HttpSession session,
                        String timeRange) {

                return executeGet(
                                getAccessToken(session),
                                SPOTIFY_API_URL + "/me/top/tracks"
                                                + "?time_range=" + timeRange
                                                + "&limit=50");
        }

        public Map<String, Object> getTopArtists(
                        HttpSession session,
                        String timeRange) {

                return executeGet(
                                getAccessToken(session),
                                SPOTIFY_API_URL + "/me/top/artists"
                                                + "?time_range=" + timeRange
                                                + "&limit=50");
        }

        public Map<String, Object> getRecentlyPlayed(
                        HttpSession session) {

                return executeGet(
                                getAccessToken(session),
                                SPOTIFY_API_URL
                                                + "/me/player/recently-played"
                                                + "?limit=50");
        }

        public Map<String, Object> getPlaylists(
                        HttpSession session) {

                return executeGet(
                                getAccessToken(session),
                                SPOTIFY_API_URL
                                                + "/me/playlists"
                                                + "?limit=50");
        }

        public Map<String, Object> getPlaylistItems(
                        HttpSession session,
                        String playlistId) {

                return executeGet(
                                getAccessToken(session),
                                SPOTIFY_API_URL
                                                + "/playlists/"
                                                + playlistId
                                                + "/items"
                                                + "?limit=50");
        }

        public List<Map<String, Object>> getAllPlaylists(
                        HttpSession session) {

                return getAllPlaylists(
                                getAccessToken(session));
        }

        public List<Map<String, Object>> getAllPlaylists(
                        String accessToken) {

                List<Map<String, Object>> allPlaylists = new ArrayList<>();

                int offset = 0;
                int limit = 50;

                final int maxPlaylists = 40;

                while (allPlaylists.size() < maxPlaylists) {

                        Map<String, Object> response = executeGet(
                                        accessToken,
                                        SPOTIFY_API_URL
                                                        + "/me/playlists"
                                                        + "?limit=" + limit
                                                        + "&offset=" + offset);

                        if (response == null) {
                                break;
                        }

                        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

                        if (items == null || items.isEmpty()) {
                                break;
                        }

                        int remaining = maxPlaylists - allPlaylists.size();

                        if (items.size() > remaining) {

                                allPlaylists.addAll(
                                                items.subList(0, remaining));

                        } else {

                                allPlaylists.addAll(items);
                        }

                        offset += items.size();

                        if (items.size() < limit) {
                                break;
                        }
                }

                return allPlaylists;
        }

        public List<Map<String, Object>> getAllPlaylistItems(
                        HttpSession session,
                        String playlistId) {

                return getAllPlaylistItems(
                                getAccessToken(session),
                                playlistId);
        }

        public List<Map<String, Object>> getAllPlaylistItems(
                        String accessToken,
                        String playlistId) {

                List<Map<String, Object>> allItems = new ArrayList<>();

                int offset = 0;
                int limit = 50;

                while (true) {

                        Map<String, Object> response = executeGet(
                                        accessToken,
                                        SPOTIFY_API_URL
                                                        + "/playlists/"
                                                        + playlistId
                                                        + "/items"
                                                        + "?limit=" + limit
                                                        + "&offset=" + offset);

                        if (response == null) {
                                break;
                        }

                        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

                        if (items == null || items.isEmpty()) {
                                break;
                        }

                        allItems.addAll(items);

                        Object totalObject = response.get("total");

                        int total = totalObject instanceof Number
                                        ? ((Number) totalObject).intValue()
                                        : allItems.size();

                        offset += items.size();

                        if (offset >= total) {
                                break;
                        }
                }

                return allItems;
        }

        private Map<String, Object> executeGet(
                        String accessToken,
                        String uri) {

                for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

                        try {

                                return restClient.get()
                                                .uri(uri)
                                                .headers(headers -> headers.setBearerAuth(
                                                                accessToken))
                                                .retrieve()
                                                .body(Map.class);

                        } catch (RestClientResponseException e) {

                                if (e.getStatusCode().value() != 429) {
                                        throw e;
                                }

                                /*
                                 * Spotify uses QUOTA_EXCEEDED to indicate
                                 * Development Mode quota exhaustion.
                                 *
                                 * This is different from the normal
                                 * temporary rate limit.
                                 *
                                 * Retrying immediately would not help.
                                 */
                                if (isQuotaExceeded(e)) {

                                        System.out.println(
                                                        "Spotify quota exceeded. "
                                                                        + "Not retrying: "
                                                                        + uri);

                                        throw e;
                                }

                                /*
                                 * Normal 429 rate limit.
                                 *
                                 * Respect Retry-After when Spotify provides it.
                                 */
                                if (attempt == MAX_RETRIES) {

                                        System.out.println(
                                                        "Spotify rate limit exceeded after "
                                                                        + MAX_RETRIES
                                                                        + " attempts: "
                                                                        + uri);

                                        throw e;
                                }

                                long delay = calculateRetryDelay(
                                                e,
                                                attempt);

                                System.out.println(
                                                "Spotify rate limit reached. "
                                                                + "Retrying in "
                                                                + delay
                                                                + " ms. Attempt "
                                                                + attempt
                                                                + "/"
                                                                + MAX_RETRIES);

                                sleep(delay);
                        }
                }

                throw new IllegalStateException(
                                "Spotify request failed unexpectedly");
        }

        private boolean isQuotaExceeded(
                        RestClientResponseException exception) {

                String responseBody = exception.getResponseBodyAsString();

                if (responseBody == null
                                || responseBody.isBlank()) {

                        return false;
                }

                String normalizedBody = responseBody.replaceAll("\\s+", "");

                return normalizedBody.contains(
                                "\"reason\":\"QUOTA_EXCEEDED\"");
        }

        private long calculateRetryDelay(
                        RestClientResponseException exception,
                        int attempt) {

                String retryAfter = exception.getResponseHeaders()
                                .getFirst("Retry-After");

                if (retryAfter != null) {

                        try {

                                long seconds = Long.parseLong(
                                                retryAfter);

                                return Math.max(
                                                seconds * 1000,
                                                0);

                        } catch (NumberFormatException ignored) {

                                // Fall back to exponential backoff.
                        }
                }

                long exponentialDelay = DEFAULT_RETRY_DELAY_MS
                                * (1L << (attempt - 1));

                return exponentialDelay;
        }

        private void sleep(
                        long milliseconds) {

                try {

                        Thread.sleep(milliseconds);

                } catch (InterruptedException e) {

                        Thread.currentThread().interrupt();

                        throw new IllegalStateException(
                                        "Spotify request retry interrupted",
                                        e);
                }
        }

        public String getAccessToken(
                        HttpSession session) {

                String accessToken = (String) session.getAttribute(
                                ACCESS_TOKEN_SESSION_KEY);

                if (accessToken == null) {

                        throw new IllegalStateException(
                                        "Spotify account is not connected");
                }

                return accessToken;
        }

        private String generateState() {

                byte[] bytes = new byte[32];

                new SecureRandom().nextBytes(bytes);

                return Base64.getUrlEncoder()
                                .withoutPadding()
                                .encodeToString(bytes);
        }

        private String encode(
                        String value) {

                return java.net.URLEncoder
                                .encode(
                                                value,
                                                StandardCharsets.UTF_8);
        }
}