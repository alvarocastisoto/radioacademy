import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
@Injectable({ providedIn: 'root' })
export class PaymentService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/payment';

  
  buyCourse(courseId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/checkout`, { courseId });
  }

  
  confirmPayment(sessionId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/confirm`, { session_id: sessionId });
  }
}
