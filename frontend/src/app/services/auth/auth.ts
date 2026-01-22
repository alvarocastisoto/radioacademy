import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

// 1. INTERFACES (Sincronizadas con tu backend DTO)
export interface User {
  id: string;
  email: string;
  name: string;
  surname: string;
  role: 'ADMIN' | 'STUDENT' | 'TEACHER' | 'ROLE_ADMIN' | 'ROLE_STUDENT';
  avatar?: string;

  // 👇👇 Tienes que añadir estos campos explícitamente 👇👇
  phone?: string;
  dni?: string;
  region?: string;
  createdAt?: string;
}

// Renombramos LoginResponse a AuthResponse porque sirve para Login y Register
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

  // 📡 SEÑAL PRINCIPAL
  currentUser = signal<User | null>(null);

  constructor() {
    this.checkLocalStorage();
  }

  // ✅ RECUPERAR SESIÓN AL RECARGAR
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

  // ✅ REGISTRO (Ahora con Auto-Login) 🚀
  // Fíjate que ahora devuelve Observable<AuthResponse> y hace el tap()
  register(userData: any): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/register`, userData)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  // ✅ LOGIN
  login(credentials: any): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  // ✅ LOGOUT
  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  // 🛠️ HELPER PRIVADO: Centraliza el guardado de sesión
  private handleAuthSuccess(response: AuthResponse) {
    // 1. Guardar Token
    localStorage.setItem('token', response.token);

    // 2. Guardar Usuario
    localStorage.setItem('user', JSON.stringify(response.user));

    // 3. Actualizar Señal
    this.currentUser.set(response.user);
  }

  // Helpers de Utilidad
  isLoggedIn(): boolean {
    return !!localStorage.getItem('token'); // Simple check de existencia
  }

  // Actualizar datos locales (ej: tras editar perfil)
  updateUserFields(newData: Partial<User>) {
    // Usamos Partial<User> para más seguridad
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
