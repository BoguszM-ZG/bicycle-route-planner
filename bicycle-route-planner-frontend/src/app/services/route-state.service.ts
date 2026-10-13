import { Injectable, inject, signal, effect } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { RouteDetailDto, RouteSummaryDto, CreateRouteRequestDto } from '../models/route.model';
import { RouteService } from './route.service';

import { LoggerService } from './logger.service';
import { AuthService } from './auth.service';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root',
})
export class RouteStateService {
  private readonly routeService = inject(RouteService);
  private readonly logger = inject(LoggerService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  private readonly _routes = signal<RouteSummaryDto[]>([]);
  private readonly _selectedRoute = signal<RouteDetailDto | null>(null);

  private readonly _isLoadingList = signal<boolean>(false);
  private readonly _isSaving = signal<boolean>(false);

  public readonly routes = this._routes.asReadonly();
  public readonly selectedRoute = this._selectedRoute.asReadonly();

  public readonly isLoadingList = this._isLoadingList.asReadonly();
  public readonly isSaving = this._isSaving.asReadonly();

  constructor() {
    effect(() => {
      const user = this.authService.currentUser();
      if (!user) {
        this.logger.debug('User logged out, clearing RouteStateService');
        this.clearState();
      }
    });
  }

  private clearState(): void {
    this._routes.set([]);
    this._selectedRoute.set(null);
    this._isLoadingList.set(false);
    this._isSaving.set(false);
  }

  public fetchRoutes(): void {
    this._isLoadingList.set(true);

    this.routeService
      .getRoutes()
      .pipe(finalize(() => this._isLoadingList.set(false)))
      .subscribe({
        next: (routes) => this._routes.set(routes),
        error: (err) => {
          this.logger.error('Error fetching routes', err);
          this.notificationService.error('ERRORS.FETCH_ROUTES_FAILED');
        },
      });
  }

  public selectRoute(routeId: number): void {
    if (this._selectedRoute()?.id === routeId) {
      this.clearSelectedRoute();
      return;
    }

    this._selectedRoute.set(null);

    this.routeService.getRouteDetails(routeId).subscribe({
      next: (routeDetails) => this._selectedRoute.set(routeDetails),
      error: (err) => {
        this.logger.error('Error fetching route details', err);
        this.notificationService.error('ERRORS.FETCH_ROUTE_DETAILS_FAILED');
      },
    });
  }

  public clearSelectedRoute(): void {
    this._selectedRoute.set(null);
  }

  public deleteRoute(routeId: number): void {
    this.routeService.deleteRoute(routeId).subscribe({
      next: () => {
        this._routes.update((routes) => routes.filter((r) => r.id !== routeId));
        this.logger.info('[Route] Route successfully deleted', { routeId });
        if (this._selectedRoute()?.id === routeId) {
          this._selectedRoute.set(null);
        }
      },
      error: (err) => {
        if (err.status === 404) {
          this.logger.debug('[Route] Route to delete not found:', { routeId });
        } else {
          this.logger.error('[Route] Error deleting route:', err);
        }
        this.notificationService.error('ERRORS.DELETE_ROUTE_FAILED');
      },
    });
  }

  public createRoute(dto: CreateRouteRequestDto, onSuccess?: () => void): void {
    this._isSaving.set(true);

    this.routeService
      .addRoute(dto)
      .pipe(finalize(() => this._isSaving.set(false)))
      .subscribe({
        next: (newRouteDetails) => {
          const summary: RouteSummaryDto = {
            id: newRouteDetails.id!,
            name: newRouteDetails.name,
            distanceMeters: newRouteDetails.distanceMeters,
            folderId: newRouteDetails.folderId,
          };
          this._routes.update((routes) => [summary, ...routes]);

          if (onSuccess) onSuccess();
          this.logger.info('[Route] Route successfully saved', newRouteDetails.name);
        },
        error: (err) => {
          if (err.status === 400) {
            this.logger.debug('[Route] Route validation failed on server', err.error);
            this.notificationService.error('ERRORS.ROUTE_TOO_SHORT');
          } else {
            this.logger.error('Error saving route', err);
            this.notificationService.error('ERRORS.SAVE_ROUTE_FAILED');
          }
        },
      });
  }

  public exportGpx(routeId: number): void {
    const route = this._routes().find((r) => r.id === routeId);
    const routeName = route ? route.name : `trasa_${routeId}`;

    this.routeService.downloadGpx(routeId, routeName).subscribe({
      next: () => {
        this.logger.debug('[Route] GPX download initiated successfully');
      },
      error: () => {
        this.logger.error('Error exporting GPX', { routeId });
        this.notificationService.error('ERRORS.DOWNLOAD_GPX_ERROR');
      },
    });
  }

  public moveRouteToFolder(routeId: number, folderId: number | null): void {
    this.routeService.updateRouteFolder(routeId, { folderId }).subscribe({
      next: () => {
        this.logger.info(
          '[Route] Route moved to new folder.',
          { routeId: routeId, targetFolderId: folderId },
        );
        this._routes.update((routes) =>
          routes.map((r) => (r.id === routeId ? { ...r, folderId } : r)),
        );
      },
      error: (err) => {
        if (err.status === 400 || err.status === 404) {
          this.logger.debug('[Route] Route move validation error:', { routeId, folderId, error: err.error });
        } else {
          this.logger.error('[Route] Error moving route:', err);
        }
        this.notificationService.error('ERRORS.MOVE_ROUTE_FAILED');
      },
    });
  }

  public handleFolderDeleted(folderId: number): void {
    this._routes.update((routes) =>
      routes.map((r) => (r.folderId === folderId ? { ...r, folderId: null } : r)),
    );
  }

  public renameRoute(routeId: number, newName: string): void {
    this.routeService.renameRoute(routeId, { name: newName }).subscribe({
      next: () => {
        this.logger.info('[Route] Route renamed successfully.', {
          routeId: routeId,
          newName: newName,
        });
        this._routes.update((routes) =>
          routes.map((r) => (r.id === routeId ? { ...r, name: newName } : r)),
        );
      },
      error: (err) => {
        if (err.status === 400 || err.status === 404) {
          this.logger.debug('[Route] Route rename validation error:', { routeId, newName, error: err.error });
        } else {
          this.logger.error('[Route] Error renaming route:', err);
        }
        this.notificationService.error('ERRORS.RENAME_ROUTE_FAILED');
      },
    });
  }
}
