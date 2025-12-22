import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/admin';

  // Obtener todos los usuarios
  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users`);
  }
  getCoursesLight(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/courses-dropdown`);
  }

  // Matricular
  enrollUser(userId: string, courseId: number): Observable<any> {
    // Usamos params en la URL query string como definimos en el backend
    return this.http.post(`${this.apiUrl}/enroll?userId=${userId}&courseId=${courseId}`, {});
  }

  // Desmatricular
  unenrollUser(userId: string, courseId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/unenroll?userId=${userId}&courseId=${courseId}`, {});
  }
  // Obtener cursos de un usuario específico
  getUserCourses(userId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users/${userId}/courses`);
  }

  changeUserRole(userId: string, newRole: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/${userId}/role?newRole=${newRole}`, {});
  }
}
