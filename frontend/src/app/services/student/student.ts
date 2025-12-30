import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class StudentService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/student`; // Asegúrate de tener esta URL base correcta

  // Obtener mis cursos
  getMyCourses(): Observable<any[]> {
    return this.http.get<any[]>('http://localhost:8080/api/student/dashboard');
  }

  // Obtener el contenido COMPLETO del curso (Módulos + Lecciones)
  getCourseContent(courseId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/course/${courseId}/content`);
  }
}
