import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class CourseService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/courses';

  // Obtener todos los cursos
  getCourses(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  // Obtener módulos de un curso
  getCourseModules(courseId: string): Observable<any[]> {
    return this.http.get<any[]>(`http://localhost:8080/api/modules/course/${courseId}`);
  }

  // Obtener lecciones de un módulo
  getModuleLessons(moduleId: string): Observable<any[]> {
    return this.http.get<any[]>(`http://localhost:8080/api/lessons/module/${moduleId}`);
  }
}
