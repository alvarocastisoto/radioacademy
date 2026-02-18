import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
@Injectable({
  providedIn: 'root',
})
export class CourseService {
  private http = inject(HttpClient);

  
  private readonly API_BASE = environment.apiUrl;

  
  getCourses(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_BASE}/courses`);
  }

  createCourse(courseData: any): Observable<any> {
    return this.http.post<any>(`${this.API_BASE}/courses`, courseData);
  }

  deleteCourse(courseId: string): Observable<any> {
    return this.http.delete<any>(`${this.API_BASE}/courses/${courseId}`);
  }
  
  getCourseById(id: string): Observable<any> {
    return this.http.get<any>(`${this.API_BASE}/courses/${id}`);
  }

  
  updateCourse(id: string, courseData: any): Observable<any> {
    return this.http.put<any>(`${this.API_BASE}/courses/${id}`, courseData);
  }

  
  createModule(moduleData: any): Observable<any> {
    return this.http.post<any>(`${this.API_BASE}/modules`, moduleData);
  }

  getCourseModules(courseId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_BASE}/modules/course/${courseId}`);
  }

  deleteModule(moduleId: string): Observable<any> {
    return this.http.delete<any>(`${this.API_BASE}/modules/${moduleId}`);
  }

  

  
  createLesson(lessonData: FormData): Observable<any> {
    return this.http.post(`${this.API_BASE}/lessons`, lessonData);
  }

  getModuleLessons(moduleId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_BASE}/lessons/module/${moduleId}`);
  }

  deleteLesson(lessonId: string): Observable<any> {
    return this.http.delete<any>(`${this.API_BASE}/lessons/${lessonId}`);
  }

  
  getLessonById(lessonId: string): Observable<any> {
    return this.http.get(`${this.API_BASE}/lessons/${lessonId}`);
  }

  
  updateLesson(lessonId: string, lessonData: FormData): Observable<any> {
    return this.http.put(`${this.API_BASE}/lessons/${lessonId}`, lessonData);
  }
}
