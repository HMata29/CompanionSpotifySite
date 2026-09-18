import { Component, inject, signal } from '@angular/core';
import { ApiService } from './services/api.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {

  private apiService = inject(ApiService);

  healthMessage = signal('Checking backend...');

  recentlyPlayed = signal<any[]>([]);
  recentlyPlayedLoading = signal(true);
  recentlyPlayedError = signal(false);

  constructor() {

    this.apiService.getHealth().subscribe({
      next: response => this.healthMessage.set(response),
      error: error =>
        this.healthMessage.set(
          `Backend connection failed: ${error.status} ${error.statusText}`
        )
    });

    this.loadRecentlyPlayed();
  }

  private loadRecentlyPlayed(): void {

    this.apiService.getRecentlyPlayed().subscribe({

      next: response => {
        this.recentlyPlayed.set(response.items ?? []);
        this.recentlyPlayedLoading.set(false);
      },

      error: error => {
        console.error('Failed to load recently played', error);
        this.recentlyPlayedError.set(true);
        this.recentlyPlayedLoading.set(false);
      }

    });
  }
}