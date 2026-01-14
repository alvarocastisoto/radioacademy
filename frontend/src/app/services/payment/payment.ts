import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/payment';

  // Paso 1: Ir a Stripe
  buyCourse(courseId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/checkout`, { courseId });
  }

  // Paso 2: Volver y guardar matrícula
  confirmPayment(sessionId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/confirm`, { session_id: sessionId });
  }
}
