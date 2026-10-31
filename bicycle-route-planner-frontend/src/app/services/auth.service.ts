import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, finalize, shareReplay, switchMap, of } from 'rxjs';
import { jwtDecode } from 'jwt-decode';

import { RegisterRequest, LoginRequest, AuthResponse, UserDto } from '../models/auth.model';
import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly router = inject(Router);
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private readonly _currentUser = signal<UserDto | null>(null);
  private readonly _isLoading = signal<boolean>(false);
  private readonly _error = signal<string | null>(null);

  public readonly currentUser = this._currentUser.asReadonly();
  public readonly isLoading = this._isLoading.asReadonly();
  public readonly error = this._error.asReadonly();

  public readonly isAuthenticated = computed(() => this._currentUser() !== null);

  private static readonly ACCESS_TOKEN_KEY = 'accessToken';
  private static readonly REFRESH_TOKEN_KEY = 'refreshToken';
  private static readonly CURRENT_USER_KEY = 'currentUser';
  private static readonly CLOCK_SKEW_MS = 30_000;
  private static readonly LOGIN_ROUTE = '/login';
  private static readonly DEFAULT_AUTH_ERROR_KEY = 'AUTH.INVALID_CREDENTIALS';

  private refreshInProgress$: Observable<AuthResponse> | null = null;

  constructor() {
    this.hydrateAuthState();
  }

  public register(data: RegisterRequest): Observable<AuthResponse> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, data).pipe(
      tap((response) => this.handleAuthSuccess(response)),
      catchError((error: HttpErrorResponse) => this.handleAuthError(error)),
      finalize(() => this._isLoading.set(false)),
    );
  }

  public login(data: LoginRequest): Observable<AuthResponse> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, data).pipe(
      tap((response) => this.handleAuthSuccess(response)),
      catchError((error: HttpErrorResponse) => this.handleAuthError(error)),
      finalize(() => this._isLoading.set(false)),
    );
  }

  public refreshToken(): Observable<AuthResponse> {
    if (this.refreshInProgress$) {
      return this.refreshInProgress$;
    }

    const refreshToken = this.getRefreshToken();

    if (!refreshToken) {
      this.logout();
      return throwError(() => new Error('No refresh token available'));
    }

    let currentRefresh$: Observable<AuthResponse>;
    currentRefresh$ = (this.refreshInProgress$ = this.http
      .post<AuthResponse>(`${this.apiUrl}/refresh`, { refreshToken })
      .pipe(
        switchMap((response) => {
          if (
            this.refreshInProgress$ !== currentRefresh$ ||
            this.getRefreshToken() !== refreshToken
          ) {
            this.logger.debug(
              '[Auth] Session invalidated or changed during refresh. Discarding response.',
            );
            return throwError(() => new Error('Session invalidated during token refresh'));
          }
          this.logger.debug('[Auth] Session successfully refreshed');
          this.handleAuthSuccess(response);
          return of(response);
        }),
        catchError((error: unknown) => {
          const isHttp =
            error instanceof HttpErrorResponse ||
            (typeof error === 'object' && error !== null && 'status' in error);
          const status = (error as { status?: number })?.status;

          if (status === 401 || status === 403) {
            if (
              this.refreshInProgress$ === currentRefresh$ &&
              this.getRefreshToken() === refreshToken
            ) {
              this.logger.debug('[Auth] Session expired or refresh token invalid. Logging out.');
              this.logout();
            } else {
              this.logger.debug(
                '[Auth] Refresh token rejected but session already changed or terminated.',
              );
            }
          } else if (isHttp) {
            this.logger.error(
              '[Auth] Failed to refresh session due to server or network error:',
              error,
            );
          }
          return throwError(() => error);
        }),
        finalize(() => {
          if (this.refreshInProgress$ === currentRefresh$) {
            this.refreshInProgress$ = null;
          }
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      ));

    return this.refreshInProgress$;
  }

  public clearSession(): void {
    this._currentUser.set(null);
    this._error.set(null);
    this.refreshInProgress$ = null;
    this.clearAuthStorage();
  }

  public logout(): void {
    this.clearSession();
    this.logger.info('[Auth] User logged out');
    void this.router.navigate([AuthService.LOGIN_ROUTE]);
  }

  public getAccessToken(): string | null {
    return localStorage.getItem(AuthService.ACCESS_TOKEN_KEY);
  }

  public getRefreshToken(): string | null {
    return localStorage.getItem(AuthService.REFRESH_TOKEN_KEY);
  }

  public isTokenExpired(token: string | null): boolean {
    if (!token) {
      return true;
    }
    try {
      const payload = jwtDecode<{ exp?: number }>(token);
      if (typeof payload.exp !== 'number' || !Number.isFinite(payload.exp)) {
        return true;
      }
      return Date.now() >= payload.exp * 1000 - AuthService.CLOCK_SKEW_MS;
    } catch {
      return true;
    }
  }

  public hasValidSession(): boolean {
    if (!this.isAuthenticated()) {
      return false;
    }

    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();

    const isAccessValid = !!accessToken && !this.isTokenExpired(accessToken);
    const isRefreshValid = !!refreshToken && !this.isTokenExpired(refreshToken);

    return isAccessValid || isRefreshValid;
  }

  private clearAuthStorage(): void {
    localStorage.removeItem(AuthService.ACCESS_TOKEN_KEY);
    localStorage.removeItem(AuthService.REFRESH_TOKEN_KEY);
    localStorage.removeItem(AuthService.CURRENT_USER_KEY);
  }

  private hydrateAuthState(): void {
    const userStr = localStorage.getItem(AuthService.CURRENT_USER_KEY);
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();

    if (!userStr || (!accessToken && !refreshToken)) {
      this.clearSession();
      return;
    }

    if (this.isTokenExpired(accessToken) && this.isTokenExpired(refreshToken)) {
      this.clearSession();
      return;
    }

    try {
      this._currentUser.set(JSON.parse(userStr));
    } catch {
      this.clearSession();
    }
  }

  private handleAuthSuccess(response: AuthResponse): void {
    this._currentUser.set(response.user);

    localStorage.setItem(AuthService.ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.setItem(AuthService.REFRESH_TOKEN_KEY, response.refreshToken);
    localStorage.setItem(AuthService.CURRENT_USER_KEY, JSON.stringify(response.user));
  }

  private handleAuthError(error: HttpErrorResponse): Observable<never> {
    if (error.status === 401 || error.status === 400) {
      this.logger.debug('[Auth] Authentication failed with client error:', {
        status: error.status,
      });
    } else {
      this.logger.error('[Auth] Authentication server failure:', error);
    }
    const errorMsg =
      error.status === 401
        ? AuthService.DEFAULT_AUTH_ERROR_KEY
        : error.error?.detail || error.error?.message || AuthService.DEFAULT_AUTH_ERROR_KEY;
    this._error.set(errorMsg);

    return throwError(() => error);
  }
}
