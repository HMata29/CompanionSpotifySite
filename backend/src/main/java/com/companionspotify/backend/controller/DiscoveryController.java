package com.companionspotify.backend.controller;

import com.companionspotify.backend.service.DiscoveryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    public DiscoveryController(
            DiscoveryService discoveryService) {

        this.discoveryService = discoveryService;
    }

    @GetMapping
    public List<Map<String, Object>> getDiscovery(
            HttpSession session) {

        return discoveryService.getDiscoveryCandidates(
                session);
    }
}