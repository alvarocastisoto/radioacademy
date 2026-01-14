import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private http = inject(HttpClient);

  private apiUrl = environment.apiUrl + '/admin';

  // Obtener todos los usuarios
  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users`);
  }

  // Obtener cursos ligeros (Dropdown)
  getCoursesLight(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/courses-dropdown`);
  }

  // ==========================================
  // 👇 CORRECCIÓN IMPORTANTE AQUÍ 👇
  // ==========================================

  // 1. Cambiado courseId de 'number' a 'string' (los UUID son strings)
  // 2. Usamos HttpParams para construir la URL limpiamente
  enrollUser(userId: string, courseId: string): Observable<any> {
    const params = new HttpParams().set('userId', userId).set('courseId', courseId);

    // El body va vacío {}, los datos van en la URL (params)
    return this.http.post(`${this.apiUrl}/enroll`, {}, { params });
  }

  unenrollUser(userId: string, courseId: string): Observable<any> {
    const params = new HttpParams().set('userId', userId).set('courseId', courseId);

    return this.http.post(`${this.apiUrl}/unenroll`, {}, { params });
  }

  // Obtener cursos de un usuario específico
  getUserCourses(userId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users/${userId}/courses`);
  }

  // Cambiar rol
  changeUserRole(userId: string, newRole: string): Observable<any> {
    const params = new HttpParams().set('newRole', newRole);
    return this.http.put(`${this.apiUrl}/users/${userId}/role`, {}, { params });
  }
}
