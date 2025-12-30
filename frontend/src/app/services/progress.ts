import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ProgressService {
  // Ajusta el puerto si tu backend no está en el 8080
  private apiUrl = 'http://localhost:8080/api/progress';

  constructor(private http: HttpClient) {}

  // 1. Marcar o Desmarcar una lección
  toggleProgress(lessonId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${lessonId}/toggle`, {});
  }

  // 2. Obtener lista de IDs de lecciones completadas de un curso
  getCourseProgress(courseId: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/course/${courseId}`);
  }
}
