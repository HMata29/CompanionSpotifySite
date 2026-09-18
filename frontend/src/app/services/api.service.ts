import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private http = inject(HttpClient);

  getHealth(): Observable<string> {
    return this.http.get('/api/health', {
      responseType: 'text'
    });
  }

  getRecentlyPlayed(): Observable<any> {
    return this.http.get<any>('/api/spotify/recently-played');
  }
}