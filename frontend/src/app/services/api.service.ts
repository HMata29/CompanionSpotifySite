import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TopTrack {
  id: number;
  track: {
    id: number;
    spotifyId: string;
    name: string;
    imageUrl: string | null;
    artist: {
      id: number;
      spotifyId: string;
      name: string;
    };
  };
  timeRange: string;
}

export interface TopArtist {
  id: number;
  artist: {
    id: number;
    spotifyId: string;
    name: string;
    imageUrl: string | null;
  };
  timeRange: string;
}

export interface RecentlyPlayedItem {
  track: {
    id: string;
    name: string;
    artists: {
      id: string;
      name: string;
    }[];
    album?: {
      images?: {
        url: string;
        width?: number;
        height?: number;
      }[];
    };
  };
  played_at: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private http = inject(HttpClient);

  getHealth(): Observable<string> {
    return this.http.get(
      '/api/health',
      {
        responseType: 'text'
      }
    );
  }

  getTopTracks(
    timeRange: string = 'medium_term'
  ): Observable<TopTrack[]> {

    return this.http.get<TopTrack[]>(
      `/api/music/top-tracks?timeRange=${timeRange}`
    );
  }

  getTopArtists(
    timeRange: string = 'medium_term'
  ): Observable<TopArtist[]> {

    return this.http.get<TopArtist[]>(
      `/api/music/top-artists?timeRange=${timeRange}`
    );
  }

  getRecentlyPlayed(): Observable<{
    items: RecentlyPlayedItem[];
  }> {

    return this.http.get<{
      items: RecentlyPlayedItem[];
    }>('/api/spotify/recently-played');
  }
}