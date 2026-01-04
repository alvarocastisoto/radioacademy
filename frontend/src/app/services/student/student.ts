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
  private readonly BASE_URL = 'http://localhost:8080/api';
  // Obtener mis cursos
  getMyCourses(): Observable<any[]> {
    return this.http.get<any[]>('http://localhost:8080/api/student/dashboard');
  }

  // Obtener el contenido COMPLETO del curso (Módulos + Lecciones)
  getCourseContent(courseId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/course/${courseId}/content`);
  }

  getProfile(): Observable<any> {
    return this.http.get(`${this.BASE_URL}/users/profile`);
  }

  updateProfile(data: any): Observable<any> {
    return this.http.put(`${this.BASE_URL}/users/profile`, data);
  }
}
