import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
// Asegúrate de importar tus interfaces si las tienes
import { DashboardCourse } from '../../models/dashboard-course';

@Injectable({
  providedIn: 'root',
})
export class StudentService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}`;
  // Obtener mis cursos (Dashboard)
  getMyCourses(): Observable<DashboardCourse[]> {
    return this.http.get<DashboardCourse[]>(`${this.apiUrl}/student/dashboard`);
  }

  // Obtener el contenido COMPLETO del curso
  getCourseContent(courseId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/course/${courseId}/content`);
  }

  getProfile(): Observable<any> {
    return this.http.get(`${this.apiUrl}/users/profile`);
  }

  updateProfile(data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/profile`, data);
  }

  // Obtener examen para hacer (SIN respuestas correctas)
  getQuizForStudent(quizId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/quizzes/${quizId}`);
  }

  // 2. Enviar respuestas para corrección (POST)
  // payload debe coincidir con QuizSubmissionDTO: { quizId: string, answers: Map/Object }
  submitQuiz(payload: { quizId: string; answers: any }): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/quizzes/submit`, payload);
  }
}
