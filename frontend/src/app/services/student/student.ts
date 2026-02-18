import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

import { DashboardCourse } from '../../models/dashboard-course';

@Injectable({
  providedIn: 'root',
})
export class StudentService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}`;
  
  getMyCourses(): Observable<DashboardCourse[]> {
    return this.http.get<DashboardCourse[]>(`${this.apiUrl}/student/dashboard`);
  }

  
  getCourseContent(courseId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/student/course/${courseId}/content`);
  }

  getProfile(): Observable<any> {
    return this.http.get(`${this.apiUrl}/users/profile`);
  }

  updateProfile(data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/profile`, data);
  }

  
  getQuizForStudent(quizId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/quizzes/${quizId}`);
  }

  
  
  submitQuiz(payload: { quizId: string; answers: any }): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/quizzes/submit`, payload);
  }
}
