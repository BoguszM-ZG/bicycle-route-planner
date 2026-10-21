import { HttpInterceptorFn, HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../environments/environment';

const AUTH_ENDPOINT = '/auth/';
const TELEMETRY_ENDPOINT = '/telemetry/';
const AUTH_HEADER = 'Authorization';
const BEARER_PREFIX = 'Bearer ';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  const isApiUrl = req.url.startsWith(environment.apiUrl);
  const isExcludedEndpoint = req.url.includes(AUTH_ENDPOINT) || req.url.includes(TELEMETRY_ENDPOINT);

  if (!isApiUrl || isExcludedEndpoint) {
    return next(req);
  }

  const token = authService.getAccessToken();
  const refreshToken = authService.getRefreshToken();

  if (token && !authService.isTokenExpired(token)) {
    return next(addTokenHeader(req, token)).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
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

  return next(req);
};

function addTokenHeader(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({
    setHeaders: { [AUTH_HEADER]: `${BEARER_PREFIX}${token}` },
  });
}
