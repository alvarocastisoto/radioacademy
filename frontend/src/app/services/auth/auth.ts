import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';


export interface User {
  id: string;
  email: string;
  name: string;
  surname: string;
  role: 'ADMIN' | 'STUDENT' | 'TEACHER' | 'ROLE_ADMIN' | 'ROLE_STUDENT';
  avatar?: string;

  
  phone?: string;
  dni?: string;
  region?: string;
  createdAt?: string;
}


export interface AuthResponse {
  token: string;
  user: User;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = environment.apiUrl + '/auth';

  
  currentUser = signal<User | null>(null);

  constructor() {
    this.checkLocalStorage();
  }

  
  private checkLocalStorage() {
    const storedUser = localStorage.getItem('user');
    const token = localStorage.getItem('token');

    if (storedUser && token) {
      try {
        this.currentUser.set(JSON.parse(storedUser));
      } catch (e) {
        console.error('Datos corruptos en localStorage, cerrando sesión...');
        this.logout();
      }
    }
  }

  
  
  register(userData: any): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/register`, userData)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  
  login(credentials: any): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  
  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  
  private handleAuthSuccess(response: AuthResponse) {
    
    localStorage.setItem('token', response.token);

    
    localStorage.setItem('user', JSON.stringify(response.user));

    
    this.currentUser.set(response.user);
  }

  
  isLoggedIn(): boolean {
    return !!localStorage.getItem('token'); 
  }

  
  updateUserFields(newData: Partial<User>) {
    
    const current = this.currentUser();
    if (current) {
      const updatedUser = { ...current, ...newData };
      this.currentUser.set(updatedUser);
      localStorage.setItem('user', JSON.stringify(updatedUser));
    }
  }

  requestPasswordReset(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/reset-password`, { token, password });
  }
}
