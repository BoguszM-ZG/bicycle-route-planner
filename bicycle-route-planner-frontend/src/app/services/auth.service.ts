import { Injectable, inject, signal, computed, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, finalize } from 'rxjs';

import { RegisterRequest, LoginRequest, AuthResponse, UserDto } from '../models/auth.model';
import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private readonly _currentUser = signal<UserDto | null>(null);
  private readonly _isLoading = signal<boolean>(false);
  private readonly _error = signal<string | null>(null);

  public readonly currentUser = this._currentUser.asReadonly();
  public readonly isLoading = this._isLoading.asReadonly();
  public readonly error = this._error.asReadonly();

  public readonly isAuthenticated = computed(() => this._currentUser() !== null);

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
    const refreshToken = this.getRefreshToken();

    if (!refreshToken) {
      this.logout();
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, { refreshToken }).pipe(
      tap((response) => {
        this.logger.debug('Session successfully refreshed in AuthService');
        this.handleAuthSuccess(response);
      }),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 || error.status === 403) {
          this.logger.debug('[Auth] Session expired or refresh token invalid. Logging out.');
        } else {
          this.logger.error('[Auth] Failed to refresh session due to server error:', error);
        }
        this.logout();
        return throwError(() => error);
      }),
    );
  }

  public logout(): void {
    this._currentUser.set(null);
    this.logger.info('User logged out');
    this._error.set(null);

    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('currentUser');
    }

    void this.router.navigate(['/login']);
  }

  public getAccessToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('accessToken');
    }
    return null;
  }

  public getRefreshToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('refreshToken');
    }
    return null;
  }

  private hydrateAuthState(): void {
    if (isPlatformBrowser(this.platformId)) {
      const userStr = localStorage.getItem('currentUser');
      if (userStr) {
        try {
          this._currentUser.set(JSON.parse(userStr));
        } catch {
          this.logout();
        }
      }
    }
  }

  private handleAuthSuccess(response: AuthResponse): void {
    this._currentUser.set(response.user);

    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('accessToken', response.accessToken);
      if (response.refreshToken) {
        localStorage.setItem('refreshToken', response.refreshToken);
      }
      localStorage.setItem('currentUser', JSON.stringify(response.user));
    }
  }

  private handleAuthError(error: HttpErrorResponse): Observable<never> {
    if (error.status === 401 || error.status === 400) {
      this.logger.debug('[Auth] Authentication failed with client error:', { status: error.status });
    } else {
      this.logger.error('[Auth] Authentication server failure:', error);
    }
    const errorMsg =
      error.error?.message || 'Invalid authentication credentials. Please try again.';
    this._error.set(errorMsg);

    return throwError(() => error);
  }
}
