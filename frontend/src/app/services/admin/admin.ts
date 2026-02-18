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

  
  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users`);
  }

  
  getCoursesLight(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/courses-dropdown`);
  }

  
  
  

  
  
  enrollUser(userId: string, courseId: string): Observable<any> {
    const params = new HttpParams().set('userId', userId).set('courseId', courseId);

    
    return this.http.post(`${this.apiUrl}/enroll`, {}, { params });
  }

  unenrollUser(userId: string, courseId: string): Observable<any> {
    const params = new HttpParams().set('userId', userId).set('courseId', courseId);

    return this.http.post(`${this.apiUrl}/unenroll`, {}, { params });
  }

  
  getUserCourses(userId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users/${userId}/courses`);
  }

  
  changeUserRole(userId: string, newRole: string): Observable<any> {
    const params = new HttpParams().set('newRole', newRole);
    return this.http.put(`${this.apiUrl}/users/${userId}/role`, {}, { params });
  }

  
  registeredDaily(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/metrics/registered-daily`);
  }

  enrollmentsDaily(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/metrics/enrollments-daily`);
  }

  revenueDaily(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/metrics/revenue-daily`);
  }

  topCourses(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/metrics/top-courses`);
  }
}
