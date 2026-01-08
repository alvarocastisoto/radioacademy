import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class MediaService {
  private http = inject(HttpClient);
  // Asegúrate de que tu environment tenga apiUrl: 'http://localhost:8080/api'
  private apiUrl = environment.apiUrl;

  uploadImage(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http
      .post<{ url: string }>(`${this.apiUrl}/media/upload`, formData)
      .pipe(map((response) => response.url));
  }
}
