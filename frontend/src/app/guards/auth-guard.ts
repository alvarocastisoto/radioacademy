import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1. Obtenemos el usuario actual
  const user = authService.currentUser();

  // 2. Comprobamos si existe y si es ADMIN
  // (Si solo quieres comprobar login, quita la parte de user.role === 'ADMIN')
  if (user && user.role === 'ADMIN') {
    return true; // ✅ PASA ADELANTE
  }

  // 3. Si no cumple, lo mandamos al Login
  router.navigate(['/auth/login']);
  return false; // ⛔ STOP
};
