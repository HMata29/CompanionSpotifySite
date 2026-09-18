package com.companionspotify.backend.controller;

import com.companionspotify.backend.entity.SpotifyAccount;
import com.companionspotify.backend.service.SpotifyOAuthService;
import com.companionspotify.backend.service.SpotifySyncService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
public class SpotifyController {

    private final SpotifySyncService spotifySyncService;
    private final SpotifyOAuthService spotifyOAuthService;

    public SpotifyController(
            SpotifyOAuthService spotifyOAuthService,
            SpotifySyncService spotifySyncService
    ) {
        this.spotifyOAuthService = spotifyOAuthService;
        this.spotifySyncService = spotifySyncService;
    }

    @GetMapping("/api/spotify/login")
    public RedirectView login(HttpSession session) {

        String authorizationUrl =
                spotifyOAuthService.buildAuthorizationUrl(session);

        return new RedirectView(authorizationUrl);
    }

    @GetMapping("/api/spotify/callback")
    public Map<String, Object> callback(
            @RequestParam String code,
            @RequestParam String state,
            HttpSession session
    ) {

        spotifyOAuthService.exchangeCodeForToken(
                code,
                state,
                session
        );

        return spotifyOAuthService.getCurrentUser(session);
    }

    @GetMapping("/api/spotify/me")
    public Map<String, Object> me(
            HttpSession session
    ) {

        return spotifyOAuthService.getCurrentUser(session);
    }

    @GetMapping("/api/spotify/top/tracks")
    public Map<String, Object> topTracks(
            @RequestParam(defaultValue = "medium_term")
            String timeRange,
            HttpSession session
    ) {

        return spotifyOAuthService.getTopTracks(
                session,
                timeRange
        );
    }

    @GetMapping("/api/spotify/top/artists")
    public Map<String, Object> topArtists(
            @RequestParam(defaultValue = "medium_term")
            String timeRange,
            HttpSession session
    ) {

        return spotifyOAuthService.getTopArtists(
                session,
                timeRange
        );
    }

    @GetMapping("/api/spotify/recently-played")
    public Map<String, Object> recentlyPlayed(
            HttpSession session
    ) {

        return spotifyOAuthService.getRecentlyPlayed(
                session
        );
    }

    @GetMapping("/api/spotify/playlists")
    public Map<String, Object> playlists(
            HttpSession session
    ) {

        return spotifyOAuthService.getPlaylists(
                session
        );
    }

    @GetMapping("/api/spotify/playlists/{playlistId}/items")
    public Map<String, Object> playlistItems(
            @PathVariable String playlistId,
            HttpSession session
    ) {

        return spotifyOAuthService.getPlaylistItems(
                session,
                playlistId
        );
    }

    @GetMapping("/api/spotify/sync")
    public SpotifyAccount sync(
            HttpSession session
    ) {

        return spotifySyncService.syncCurrentUser(
                session
        );
    }

    @GetMapping("/api/spotify/sync/top-artists")
    public Map<String, Object> syncTopArtists(
            @RequestParam(defaultValue = "medium_term")
            String timeRange,
            HttpSession session
    ) {

        int saved =
                spotifySyncService.syncTopArtists(
                        session,
                        timeRange
                );

        return Map.of(
                "savedArtists", saved,
                "timeRange", timeRange
        );
    }

    @GetMapping("/api/spotify/sync/top-tracks")
    public Map<String, Object> syncTopTracks(
            @RequestParam(defaultValue = "medium_term")
            String timeRange,
            HttpSession session
    ) {

        int saved =
                spotifySyncService.syncTopTracks(
                        session,
                        timeRange
                );

        return Map.of(
                "savedTracks", saved,
                "timeRange", timeRange
        );
    }

    @GetMapping("/api/spotify/sync/recently-played")
        public Map<String, Object> syncRecentlyPlayed(
                HttpSession session
        ) {

        int saved =
                spotifySyncService.syncRecentlyPlayed(
                        session
                );

        return Map.of(
                "savedListeningHistory", saved
        );
        }
}