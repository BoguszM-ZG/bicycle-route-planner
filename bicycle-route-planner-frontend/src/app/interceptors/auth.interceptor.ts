import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn, } from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { LoggerService } from '../services/logger.service';
import { environment } from '../../environments/environment';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const logger = inject(LoggerService);
  const token = authService.getAccessToken();

  const isApiUrl = req.url.startsWith(environment.apiUrl);
  const isAuthEndpoint = req.url.includes('/auth/');

  let authReq = req;

  if (token && isApiUrl && !isAuthEndpoint) {
    authReq = addTokenHeader(req, token);
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && isApiUrl && !isAuthEndpoint) {
        return handle401Error(authReq, next, authService, logger);
      }

      return throwError(() => error);
    }),
  );
};

function addTokenHeader(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });
}

function handle401Error(
  request: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  logger: LoggerService,
) {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return authService.refreshToken().pipe(
      switchMap((response) => {
        isRefreshing = false;
        refreshTokenSubject.next(response.accessToken);

        logger.info('Token refreshed successfully. Retrying request.');
        return next(addTokenHeader(request, response.accessToken));
      }),
      catchError((err) => {
        isRefreshing = false;
        logger.warn('Refresh token expired or invalid. Forcing logout.');
        authService.logout();
        return throwError(() => err);
      }),
    );
  } else {
    return refreshTokenSubject.pipe(
      filter((token) => token !== null),
      take(1),
      switchMap((token) => {
        return next(addTokenHeader(request, token as string));
      }),
    );
  }
}
