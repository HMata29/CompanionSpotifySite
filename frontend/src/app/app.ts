import { Component, inject, signal } from '@angular/core';
import {
  ApiService,
  TopTrack,
  TopArtist,
  RecentlyPlayedItem
} from './services/api.service';

type TimeRange = 'short_term' | 'medium_term' | 'long_term';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {

  private apiService = inject(ApiService);

  healthMessage = signal('Checking backend...');

  selectedTimeRange = signal<TimeRange>('medium_term');

  topTracks = signal<TopTrack[]>([]);
  topTracksLoading = signal(true);
  topTracksError = signal(false);

  topArtists = signal<TopArtist[]>([]);
  topArtistsLoading = signal(true);
  topArtistsError = signal(false);

  recentlyPlayed = signal<RecentlyPlayedItem[]>([]);
  recentlyPlayedLoading = signal(true);
  recentlyPlayedError = signal(false);

  constructor() {

    this.checkBackend();

    this.loadTopTracks();

    this.loadTopArtists();

    this.loadRecentlyPlayed();
  }

  private checkBackend(): void {

    this.apiService.getHealth().subscribe({

      next: response => {
        this.healthMessage.set(response);
      },

      error: error => {

        this.healthMessage.set(
          `Backend connection failed: ${error.status} ${error.statusText}`
        );

      }

    });
  }

  private loadTopTracks(): void {

    this.topTracksLoading.set(true);
    this.topTracksError.set(false);

    this.apiService
      .getTopTracks(this.selectedTimeRange())
      .subscribe({

        next: tracks => {

          this.topTracks.set(tracks);

          this.topTracksLoading.set(false);

        },

        error: error => {

          console.error(
            'Failed to load top tracks',
            error
          );

          this.topTracksError.set(true);
          this.topTracksLoading.set(false);

        }

      });
  }

  private loadTopArtists(): void {

    this.topArtistsLoading.set(true);
    this.topArtistsError.set(false);

    this.apiService
      .getTopArtists(this.selectedTimeRange())
      .subscribe({

        next: artists => {

          this.topArtists.set(artists);

          this.topArtistsLoading.set(false);

        },

        error: error => {

          console.error(
            'Failed to load top artists',
            error
          );

          this.topArtistsError.set(true);
          this.topArtistsLoading.set(false);

        }

      });
  }

  selectTimeRange(
    timeRange: TimeRange
  ): void {

    if (this.selectedTimeRange() === timeRange) {
      return;
    }

    this.selectedTimeRange.set(timeRange);

    this.loadTopTracks();

    this.loadTopArtists();
  }

  isTimeRangeSelected(
    timeRange: TimeRange
  ): boolean {

    return this.selectedTimeRange() === timeRange;
  }

  getTopTrackCount(): number {
    return this.topTracks().length;
  }

  getTopArtistCount(): number {
    return this.topArtists().length;
  }

  getRecentlyPlayedCount(): number {
    return this.recentlyPlayed().length;
  }

  private loadRecentlyPlayed(): void {

    this.apiService.getRecentlyPlayed().subscribe({

      next: response => {

        this.recentlyPlayed.set(
          response.items ?? []
        );

        this.recentlyPlayedLoading.set(false);

      },

      error: error => {

        console.error(
          'Failed to load recently played',
          error
        );

        this.recentlyPlayedError.set(true);
        this.recentlyPlayedLoading.set(false);

      }

    });
  }

  getRecentlyPlayedArtist(
    item: RecentlyPlayedItem
  ): string {

    return item.track.artists
      ?.map(artist => artist.name)
      .join(', ') ?? 'Unknown artist';
  }

  getAlbumImage(
    item: RecentlyPlayedItem
  ): string | null {

    return item.track.album?.images?.[0]?.url ?? null;
  }

  formatPlayedAt(
    playedAt: string
  ): string {

    const playedDate = new Date(playedAt);
    const now = new Date();

    const difference =
      now.getTime() - playedDate.getTime();

    const minutes = Math.floor(
      difference / 60000
    );

    if (minutes < 1) {
      return 'Just now';
    }

    if (minutes < 60) {
      return `${minutes} min`;
    }

    const hours = Math.floor(
      minutes / 60
    );

    if (hours < 24) {
      return `${hours}h`;
    }

    const days = Math.floor(
      hours / 24
    );

    return `${days}d`;
  }
}