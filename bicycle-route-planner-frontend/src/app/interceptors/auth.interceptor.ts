import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { EMPTY, catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  const isApiUrl = req.url.startsWith(environment.apiUrl);
  const isPublicEndpoint =
    req.url.startsWith(`${environment.apiUrl}/auth/`) ||
    req.url.startsWith(`${environment.apiUrl}/telemetry/`);

  if (!isApiUrl || isPublicEndpoint) {
    return next(req);
  }

  const token = authService.getAccessToken();
  const refreshToken = authService.getRefreshToken();

  if (token && !authService.isTokenExpired(token)) {
    return next(addTokenHeader(req, token)).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === HttpStatusCode.Unauthorized) {
          return authService.refreshToken().pipe(
            switchMap((res) => next(addTokenHeader(req, res.accessToken))),
          );
        }
        return throwError(() => error);
      }),
    );
  }

  if (refreshToken && !authService.isTokenExpired(refreshToken)) {
    return authService.refreshToken().pipe(
      switchMap((res) => next(addTokenHeader(req, res.accessToken))),
    );
  }

  if (authService.isAuthenticated()) {
    authService.logout();
  }
  return EMPTY;
};

function addTokenHeader(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });
}
