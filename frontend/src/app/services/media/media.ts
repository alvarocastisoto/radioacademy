import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs'; // 👈 Importamos map
import { environment } from '../../../environments/environment';

// Definimos qué forma tiene la respuesta del backend
interface UploadResponse {
  url: string;
}

@Injectable({
  providedIn: 'root',
})
export class MediaService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  uploadImage(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http
      .post<UploadResponse>(`${this.apiUrl}/media/upload`, formData)
      .pipe(map((response) => response.url));
  }
}
