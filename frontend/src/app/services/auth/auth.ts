import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core'; // <--- Importamos 'signal'
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router'; // <--- Importamos Router

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = 'http://localhost:8080/api/auth';

  // 1. EL INTERRUPTOR (Signal)
  // Inicialmente comprobamos si ya existe el token en el navegador
  isLoggedIn = signal<boolean>(!!localStorage.getItem('token'));

  constructor() { }

  register(user: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, user);
  }

  login(credentials: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      // 'tap' es para hacer cosas secundarias sin alterar la respuesta
      tap((response: any) => {
        // Guardamos el token
        localStorage.setItem('token', response.token);
        // ENCENDEMOS EL INTERRUPTOR
        this.isLoggedIn.set(true);
      })
    );
  }

  // 2. MÉTODO PARA CERRAR SESIÓN
  logout() {
    // Borramos el token
    localStorage.removeItem('token');
    // APAGAMOS EL INTERRUPTOR
    this.isLoggedIn.set(false);
    // Redirigimos al home o login
    this.router.navigate(['/login']);
  }
}