import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';

// Definimos qué forma tiene un usuario
export interface User {
  id: string;
  email: string;
  name: string;
  role: 'ADMIN' | 'USER' | 'ROLE_ADMIN' | 'ROLE_USER';
}

// Definimos qué esperamos recibir del servidor al loguearnos
interface LoginResponse {
  token: string;
  user: User; // <--- IMPORTANTE: Esperamos que el backend nos devuelva esto
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = 'http://localhost:8080/api/auth';

  // 📡 SEÑAL PRINCIPAL: Guarda quién es el usuario actual (o null si no hay nadie)
  currentUser = signal<User | null>(null);

  constructor() {
    // AL INICIAR: Recuperamos la sesión si existe
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
        this.currentUser.set(JSON.parse(storedUser));
      } catch (e) {
        console.error('Datos corruptos en localStorage');
        this.logout();
      }
    }
  }

  // REGISTRO
  register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }

  // LOGIN
  login(credentials: any): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap((response) => {
        // 1. Guardamos Token
        localStorage.setItem('token', response.token);

        // 2. Guardamos Usuario (para que no se borre al refrescar)
        localStorage.setItem('user', JSON.stringify(response.user));

        // 3. Actualizamos la señal (Angular se entera automáticamente)
        this.currentUser.set(response.user);
      })
    );
  }

  // LOGOUT
  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.currentUser.set(null); // Ponemos la señal en vacío
    this.router.navigate(['/auth/login']);
  }

  // Ayuda para saber si hay token crudo
  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  // Método para actualizar los datos del usuario manualmente
  updateUserFields(newData: any) {
    const current = this.currentUser(); // Obtenemos valor actual
    if (current) {
      // Fusionamos lo viejo con lo nuevo
      const updatedUser = { ...current, ...newData };

      // 1. Actualizamos la Señal (Signal)
      this.currentUser.set(updatedUser);

      // 2. Actualizamos el LocalStorage para que persista al recargar
      localStorage.setItem('user', JSON.stringify(updatedUser)); // OJO: Revisa si tu clave es 'user_session' o 'user_data'
    }
  }
}
