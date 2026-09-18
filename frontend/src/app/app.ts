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

  constructor() {
    console.log('App component initialized');
    console.log('Calling /api/health...');

    this.apiService.getHealth().subscribe({
      next: (response) => {
        console.log('Backend response:', response);
        this.healthMessage.set(response);
      },
      error: (error) => {
        console.error('Backend connection error:', error);
        this.healthMessage.set(
          `Backend connection failed: ${error.status} ${error.statusText}`
        );
      },
      complete: () => {
        console.log('Health request completed');
      }
    });
  }
}