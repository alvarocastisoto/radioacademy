import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
@Injectable({
  providedIn: 'root',
})
export class CourseService {
  private http = inject(HttpClient);

  // 1. Definimos la raíz de la API para evitar errores de escritura
  private readonly API_BASE = environment.apiUrl;

  // --- CURSOS ---
  getCourses(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_BASE}/courses`);
  }

  createCourse(courseData: any): Observable<any> {
    return this.http.post<any>(`${this.API_BASE}/courses`, courseData);
  }

  deleteCourse(courseId: string): Observable<any> {
    return this.http.delete<any>(`${this.API_BASE}/courses/${courseId}`);
  }
  // OBTENER POR ID (Para rellenar el formulario)
  getCourseById(id: string): Observable<any> {
    return this.http.get<any>(`${this.API_BASE}/courses/${id}`);
  }

  // ACTUALIZAR (PUT)
  updateCourse(id: string, courseData: any): Observable<any> {
    return this.http.put<any>(`${this.API_BASE}/courses/${id}`, courseData);
  }

  // --- MÓDULOS ---
  createModule(moduleData: any): Observable<any> {
    return this.http.post<any>(`${this.API_BASE}/modules`, moduleData);
  }

  getCourseModules(courseId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_BASE}/modules/course/${courseId}`);
  }

  deleteModule(moduleId: string): Observable<any> {
    return this.http.delete<any>(`${this.API_BASE}/modules/${moduleId}`);
  }

  // --- LECCIONES ---

  // CORREGIDO: Ahora apunta a /api/lessons, no a /api/courses/lessons
  createLesson(lessonData: FormData): Observable<any> {
    return this.http.post(`${this.API_BASE}/lessons`, lessonData);
  }

  getModuleLessons(moduleId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_BASE}/lessons/module/${moduleId}`);
  }

  deleteLesson(lessonId: string): Observable<any> {
    return this.http.delete<any>(`${this.API_BASE}/lessons/${lessonId}`);
  }

  // CORREGIDO: Ahora apunta bien
  getLessonById(lessonId: string): Observable<any> {
    return this.http.get(`${this.API_BASE}/lessons/${lessonId}`);
  }

  // CORREGIDO: Ahora apunta bien
  updateLesson(lessonId: string, lessonData: FormData): Observable<any> {
    return this.http.put(`${this.API_BASE}/lessons/${lessonId}`, lessonData);
  }
}
