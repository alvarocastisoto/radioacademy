import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
@Injectable({ providedIn: 'root' })
export class PaymentService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/payment';

  // Paso 1: Ir a Stripe
  buyCourse(courseId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/checkout`, { courseId });
  }

  // Paso 2: Volver y guardar matrícula
  confirmPayment(sessionId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/confirm`, { session_id: sessionId });
  }
}
