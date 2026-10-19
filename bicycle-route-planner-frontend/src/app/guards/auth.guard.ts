import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

const LOGIN_ROUTE = '/login';

export const authGuard: CanActivateFn = (_route, _state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasValidSession()) {
    return true;
  }

  if (authService.isAuthenticated()) {
    authService.clearSession();
  }

  return router.createUrlTree([LOGIN_ROUTE]);
};
