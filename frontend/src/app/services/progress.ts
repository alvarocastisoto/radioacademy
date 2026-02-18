import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ProgressService {
  
  private apiUrl = 'http://localhost:8080/api/progress';

  constructor(private http: HttpClient) {}

  
  toggleProgress(lessonId: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${lessonId}/toggle`, {});
  }

  
  getCourseProgress(courseId: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/course/${courseId}`);
  }


  

}
