import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

const ROOT_ROUTE = '/';

export const guestGuard: CanActivateFn = (_route, _state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasValidSession()) {
    return router.createUrlTree([ROOT_ROUTE]);
  }

  if (authService.isAuthenticated()) {
    authService.clearSession();
  }

  return true;
};
