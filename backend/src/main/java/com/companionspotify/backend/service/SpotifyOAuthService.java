package com.companionspotify.backend.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Service
public class SpotifyOAuthService {

    private static final String AUTHORIZE_URL =
            "https://accounts.spotify.com/authorize";

    private static final String TOKEN_URL =
            "https://accounts.spotify.com/api/token";

    private static final String SPOTIFY_API_URL =
            "https://api.spotify.com/v1";

    private static final String STATE_SESSION_KEY =
            "spotify_oauth_state";

    private static final String ACCESS_TOKEN_SESSION_KEY =
            "spotify_access_token";

    private static final String REFRESH_TOKEN_SESSION_KEY =
            "spotify_refresh_token";

    private final RestClient restClient = RestClient.create();

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    public String buildAuthorizationUrl(HttpSession session) {

        String state = generateState();

        session.setAttribute(STATE_SESSION_KEY, state);

        String scope = String.join(" ",
                "user-read-private",
                "user-top-read",
                "user-read-recently-played",
                "playlist-read-private"
        );

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
            HttpSession session
    ) {

        String expectedState =
                (String) session.getAttribute(STATE_SESSION_KEY);

        if (expectedState == null || !expectedState.equals(state)) {
            throw new IllegalArgumentException("Invalid OAuth state");
        }

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        Map<String, Object> response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers ->
                        headers.setBasicAuth(
                                clientId,
                                clientSecret,
                                StandardCharsets.UTF_8
                        )
                )
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException(
                    "Spotify did not return an access token"
            );
        }

        session.setAttribute(
                ACCESS_TOKEN_SESSION_KEY,
                response.get("access_token")
        );

        if (response.get("refresh_token") != null) {
            session.setAttribute(
                    REFRESH_TOKEN_SESSION_KEY,
                    response.get("refresh_token")
            );
        }

        session.removeAttribute(STATE_SESSION_KEY);

        return response;
    }

    public Map<String, Object> getCurrentUser(HttpSession session) {

        String accessToken =
                (String) session.getAttribute(ACCESS_TOKEN_SESSION_KEY);

        if (accessToken == null) {
            throw new IllegalStateException(
                    "Spotify account is not connected"
            );
        }

        return restClient.get()
                .uri(SPOTIFY_API_URL + "/me")
                .headers(headers ->
                        headers.setBearerAuth(accessToken)
                )
                .retrieve()
                .body(Map.class);
    }

    private String generateState() {

        byte[] bytes = new byte[32];

        new SecureRandom().nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String encode(String value) {

        return java.net.URLEncoder
                .encode(
                        value,
                        StandardCharsets.UTF_8
                );
    }

    public Map<String, Object> getTopTracks(
        HttpSession session,
        String timeRange
        ) {

        String accessToken =
                (String) session.getAttribute(ACCESS_TOKEN_SESSION_KEY);

        if (accessToken == null) {
                throw new IllegalStateException(
                        "Spotify account is not connected"
                );
        }

        return restClient.get()
                .uri(SPOTIFY_API_URL + "/me/top/tracks"
                        + "?time_range=" + timeRange
                        + "&limit=50")
                .headers(headers ->
                        headers.setBearerAuth(accessToken)
                )
                .retrieve()
                .body(Map.class);
        }

        public Map<String, Object> getTopArtists(
        HttpSession session,
        String timeRange
        ) {

        String accessToken =
                (String) session.getAttribute(ACCESS_TOKEN_SESSION_KEY);

        if (accessToken == null) {
                throw new IllegalStateException(
                        "Spotify account is not connected"
                );
        }

        return restClient.get()
                .uri(SPOTIFY_API_URL + "/me/top/artists"
                        + "?time_range=" + timeRange
                        + "&limit=50")
                .headers(headers ->
                        headers.setBearerAuth(accessToken)
                )
                .retrieve()
                .body(Map.class);
        }

        public Map<String, Object> getRecentlyPlayed(
        HttpSession session
        ) {

        String accessToken =
                (String) session.getAttribute(ACCESS_TOKEN_SESSION_KEY);

        if (accessToken == null) {
                throw new IllegalStateException(
                        "Spotify account is not connected"
                );
        }

        return restClient.get()
                .uri(SPOTIFY_API_URL + "/me/player/recently-played"
                        + "?limit=50")
                .headers(headers ->
                        headers.setBearerAuth(accessToken)
                )
                .retrieve()
                .body(Map.class);
        }

        public Map<String, Object> getPlaylists(
        HttpSession session
        ) {

        String accessToken =
                (String) session.getAttribute(ACCESS_TOKEN_SESSION_KEY);

        if (accessToken == null) {
                throw new IllegalStateException(
                        "Spotify account is not connected"
                );
        }

        return restClient.get()
                .uri(SPOTIFY_API_URL + "/me/playlists"
                        + "?limit=50")
                .headers(headers ->
                        headers.setBearerAuth(accessToken)
                )
                .retrieve()
                .body(Map.class);
        }

        public Map<String, Object> getPlaylistItems(
        HttpSession session,
        String playlistId
        ) {

        String accessToken =
                (String) session.getAttribute(ACCESS_TOKEN_SESSION_KEY);

        if (accessToken == null) {
                throw new IllegalStateException(
                        "Spotify account is not connected"
                );
        }

        return restClient.get()
                .uri(SPOTIFY_API_URL + "/playlists/"
                        + playlistId + "/items"
                        + "?limit=50")
                .headers(headers ->
                        headers.setBearerAuth(accessToken)
                )
                .retrieve()
                .body(Map.class);
        }
}