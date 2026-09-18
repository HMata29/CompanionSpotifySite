package com.companionspotify.backend.service;

import com.companionspotify.backend.entity.Artist;
import com.companionspotify.backend.entity.ListeningHistory;
import com.companionspotify.backend.entity.SpotifyAccount;
import com.companionspotify.backend.entity.Track;
import com.companionspotify.backend.repository.ArtistRepository;
import com.companionspotify.backend.repository.ListeningHistoryRepository;
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
    private final ListeningHistoryRepository listeningHistoryRepository;

    public SpotifySyncService(
            SpotifyOAuthService spotifyOAuthService,
            SpotifyAccountRepository spotifyAccountRepository,
            ArtistRepository artistRepository,
            TrackRepository trackRepository,
            ListeningHistoryRepository listeningHistoryRepository
    ) {
        this.spotifyOAuthService = spotifyOAuthService;
        this.spotifyAccountRepository = spotifyAccountRepository;
        this.artistRepository = artistRepository;
        this.trackRepository = trackRepository;
        this.listeningHistoryRepository = listeningHistoryRepository;
    }

    public SpotifyAccount syncCurrentUser(HttpSession session) {

        Map<String, Object> spotifyUser =
                spotifyOAuthService.getCurrentUser(session);

        String spotifyUserId =
                (String) spotifyUser.get("id");

        String displayName =
                (String) spotifyUser.get("display_name");

        if (spotifyUserId == null) {
            throw new IllegalStateException(
                    "Spotify user ID not found"
            );
        }

        SpotifyAccount account =
                spotifyAccountRepository
                        .findBySpotifyUserId(spotifyUserId)
                        .orElseGet(SpotifyAccount::new);

        account.setSpotifyUserId(spotifyUserId);
        account.setDisplayName(displayName);

        return spotifyAccountRepository.save(account);
    }

    public int syncTopArtists(
            HttpSession session,
            String timeRange
    ) {

        Map<String, Object> response =
                spotifyOAuthService.getTopArtists(
                        session,
                        timeRange
                );

        Object itemsObject = response.get("items");

        if (!(itemsObject instanceof List<?> items)) {
            return 0;
        }

        int saved = 0;

        for (Object itemObject : items) {

            if (!(itemObject instanceof Map<?, ?> item)) {
                continue;
            }

            String spotifyId =
                    (String) item.get("id");

            String name =
                    (String) item.get("name");

            if (spotifyId == null || name == null) {
                continue;
            }

            Artist artist =
                    artistRepository
                            .findBySpotifyId(spotifyId)
                            .orElseGet(Artist::new);

            artist.setSpotifyId(spotifyId);
            artist.setName(name);

            artistRepository.save(artist);

            saved++;
        }

        return saved;
    }

    public int syncTopTracks(
            HttpSession session,
            String timeRange
    ) {

        Map<String, Object> response =
                spotifyOAuthService.getTopTracks(
                        session,
                        timeRange
                );

        Object itemsObject = response.get("items");

        if (!(itemsObject instanceof List<?> items)) {
            return 0;
        }

        int saved = 0;

        for (Object itemObject : items) {

            if (!(itemObject instanceof Map<?, ?> item)) {
                continue;
            }

            String spotifyId =
                    (String) item.get("id");

            String name =
                    (String) item.get("name");

            if (spotifyId == null || name == null) {
                continue;
            }

            Object artistsObject =
                    item.get("artists");

            if (!(artistsObject instanceof List<?> artists)
                    || artists.isEmpty()) {
                continue;
            }

            Object firstArtistObject =
                    artists.get(0);

            if (!(firstArtistObject instanceof Map<?, ?> artistData)) {
                continue;
            }

            String artistSpotifyId =
                    (String) artistData.get("id");

            String artistName =
                    (String) artistData.get("name");

            if (artistSpotifyId == null || artistName == null) {
                continue;
            }

            Artist artist =
                    artistRepository
                            .findBySpotifyId(artistSpotifyId)
                            .orElseGet(() -> {

                                Artist newArtist =
                                        new Artist();

                                newArtist.setSpotifyId(
                                        artistSpotifyId
                                );

                                newArtist.setName(
                                        artistName
                                );

                                return artistRepository.save(
                                        newArtist
                                );
                            });

            Track track =
                    trackRepository
                            .findBySpotifyId(spotifyId)
                            .orElseGet(Track::new);

            track.setSpotifyId(spotifyId);
            track.setName(name);
            track.setArtist(artist);

            trackRepository.save(track);

            saved++;
        }

        return saved;
    }

    public int syncRecentlyPlayed(
            HttpSession session
    ) {

        Map<String, Object> response =
                spotifyOAuthService.getRecentlyPlayed(
                        session
                );

        Object itemsObject =
                response.get("items");

        if (!(itemsObject instanceof List<?> items)) {

            System.out.println(
                    "RECENTLY PLAYED: items non trovati"
            );

            return 0;
        }

        System.out.println(
                "RECENTLY PLAYED ITEMS: " + items.size()
        );

        SpotifyAccount account =
                syncCurrentUser(session);

        int saved = 0;

        for (Object itemObject : items) {

            if (!(itemObject instanceof Map<?, ?> item)) {

                System.out.println(
                        "SKIP: item non è una Map"
                );

                continue;
            }

            Object trackObject =
                    item.get("track");

            if (!(trackObject instanceof Map<?, ?> trackData)) {

                System.out.println(
                        "SKIP: track non trovata"
                );

                continue;
            }

            String spotifyTrackId =
                    (String) trackData.get("id");

            String trackName =
                    (String) trackData.get("name");

            if (spotifyTrackId == null
                    || trackName == null) {

                System.out.println(
                        "SKIP: track ID o nome mancanti"
                );

                continue;
            }

            Object artistsObject =
                    trackData.get("artists");

            if (!(artistsObject instanceof List<?> artists)
                    || artists.isEmpty()) {

                System.out.println(
                        "SKIP: artisti mancanti per "
                                + trackName
                );

                continue;
            }

            Object firstArtistObject =
                    artists.get(0);

            if (!(firstArtistObject instanceof Map<?, ?> artistData)) {

                System.out.println(
                        "SKIP: dati artista non validi per "
                                + trackName
                );

                continue;
            }

            String artistSpotifyId =
                    (String) artistData.get("id");

            String artistName =
                    (String) artistData.get("name");

            if (artistSpotifyId == null
                    || artistName == null) {

                System.out.println(
                        "SKIP: ID o nome artista mancanti per "
                                + trackName
                );

                continue;
            }

            Artist artist =
                    artistRepository
                            .findBySpotifyId(artistSpotifyId)
                            .orElseGet(() -> {

                                Artist newArtist =
                                        new Artist();

                                newArtist.setSpotifyId(
                                        artistSpotifyId
                                );

                                newArtist.setName(
                                        artistName
                                );

                                return artistRepository.save(
                                        newArtist
                                );
                            });

            Track track =
                    trackRepository
                            .findBySpotifyId(spotifyTrackId)
                            .orElseGet(Track::new);

            track.setSpotifyId(spotifyTrackId);
            track.setName(trackName);
            track.setArtist(artist);

            track = trackRepository.save(track);

            String playedAtString =
                    (String) item.get("played_at");

            if (playedAtString == null) {

                System.out.println(
                        "SKIP: played_at mancante per "
                                + trackName
                );

                continue;
            }

            Instant playedAt;

            try {

                playedAt =
                        Instant.parse(playedAtString);

            } catch (Exception e) {

                System.out.println(
                        "SKIP: played_at non valido per "
                                + trackName
                );

                continue;
            }

            boolean alreadyExists =
                    listeningHistoryRepository
                            .existsBySpotifyAccountIdAndTrackIdAndPlayedAt(
                                    account.getId(),
                                    track.getId(),
                                    playedAt
                            );

            if (alreadyExists) {

                System.out.println(
                        "DUPLICATO: "
                                + trackName
                                + " - "
                                + playedAt
                );

                continue;
            }

            ListeningHistory history =
                    new ListeningHistory(
                            account,
                            track,
                            playedAt
                    );

            listeningHistoryRepository.save(history);

            saved++;

            System.out.println(
                    "SALVATO: "
                            + trackName
                            + " - "
                            + artistName
                            + " - "
                            + playedAt
            );
        }

        System.out.println(
                "TOTALE LISTENING HISTORY SALVATI: "
                        + saved
        );

        return saved;
    }
}