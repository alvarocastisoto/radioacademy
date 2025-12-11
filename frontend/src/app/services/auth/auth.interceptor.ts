import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // 1. Buscamos el token en el bolsillo
  const token = localStorage.getItem('token');

  // 2. Si hay token, clonamos la petición y le pegamos la cabecera
  if (token) {
    const clonedRequest = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
    // Pasamos la petición clonada (con token)
    return next(clonedRequest);
  }

  // 3. Si no hay token, pasamos la original
  return next(req);
};
