package com.companionspotify.backend.repository;

import com.companionspotify.backend.entity.SpotifyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpotifyAccountRepository extends JpaRepository<SpotifyAccount, Long> {

    Optional<SpotifyAccount> findBySpotifyUserId(String spotifyUserId);
}