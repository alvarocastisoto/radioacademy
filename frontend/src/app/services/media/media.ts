import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class MediaService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl; // Asegúrate de que es 'http://localhost:8080/api'

  uploadImage(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);

    // 👇 CAMBIO CRUCIAL:
    // 1. Añadimos { responseType: 'text' } porque Java devuelve un String, no un JSON.
    // 2. Quitamos el .pipe(map(...)) porque la respuesta YA es la URL.
    return this.http.post(`${this.apiUrl}/media/upload`, formData, { responseType: 'text' });
  }
}
