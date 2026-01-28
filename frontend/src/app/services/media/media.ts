import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

// Backend nuevo: { path: "uploads/images/courses/uuid.jpg" }
// Backend viejo: { url: "http://..." }
interface UploadResponse {
  path?: string;
  url?: string;
}

@Injectable({ providedIn: 'root' })
export class MediaService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  /**
   * Sube imagen o pdf. Devuelve el PATH RELATIVO que debes guardar en BD.
   * * @param file El archivo a subir
   * @param folder (Opcional) Nombre de la subcarpeta: 'courses', 'users', 'modules'
   * @returns Ej: "uploads/images/courses/xxx.jpg"
   */
  uploadFile(file: File, folder: string = 'others'): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);

    // 👇 AÑADIDO: Enviamos el parámetro 'folder' al backend
    if (folder) {
      formData.append('folder', folder);
    }

    return this.http.post<UploadResponse>(`${this.apiUrl}/media/upload`, formData).pipe(
      map((res) => {
        const p = res.path ?? res.url;
        if (!p) throw new Error('Respuesta inválida del backend (no viene path/url)');
        // Si viene URL completa (legacy), te la devuelvo tal cual.
        return p;
      }),
    );
  }

  /**
   * Si guardas paths relativos en BD, para imágenes públicas necesitas construir URL pública.
   * Para PDFs NO lo uses (se sirven por endpoint autenticado).
   */
  toPublicUrl(pathOrUrl: string | null | undefined): string {
    if (!pathOrUrl) return ''; // Manejo de nulos seguro
    if (pathOrUrl.startsWith('http')) return pathOrUrl;

    // environment.apiUrl suele ser "...:8080/api" -> quitamos /api
    const base = this.apiUrl.replace(/\/api\/?$/, '');

    // Quitamos barras duplicadas al principio del path
    const clean = pathOrUrl.replace(/^\/+/, '');

    return `${base}/${clean}`;
  }

  /**
   * PDF premium: descarga/visualización por endpoint autenticado.
   * download=false -> inline (para previsualizar)
   * download=true  -> attachment (para descargar)
   */
  getLessonPdfBlob(lessonId: string, download: boolean): Observable<Blob> {
    const url = `${this.apiUrl}/student/lessons/${lessonId}/pdf?download=${download}`;
    return this.http.get(url, { responseType: 'blob' });
  }
}
