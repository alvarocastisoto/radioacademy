import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.currentUser();

  //  Aquí SÍ exigimos ser ADMIN
  if (user && (user.role === 'ADMIN' || user.role === 'ROLE_ADMIN')) {
    return true;
  }

  // Si intenta entrar pero no es admin, lo mandamos al inicio
  router.navigate(['/']);
  return false;
};
