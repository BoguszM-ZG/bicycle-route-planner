import { Injectable, inject, signal, effect, untracked } from '@angular/core';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { RouteDetailDto, RouteSummaryDto, CreateRouteRequestDto } from '../models/route.model';
import { RouteService } from './route.service';
import { sanitizeFileName, triggerBlobDownload } from '../utils/file-download.util';
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

  private fetchRoutesSubscription?: Subscription;
  private selectRouteSubscription?: Subscription;

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
      if (user) {
        untracked(() => this.fetchRoutes());
      } else {
        this.logger.debug('[RouteStateService] User logged out, clearing RouteStateService');
        untracked(() => this.clearState());
      }
    });
  }

  private clearState(): void {
    this.fetchRoutesSubscription?.unsubscribe();
    this.selectRouteSubscription?.unsubscribe();
    this._routes.set([]);
    this._selectedRoute.set(null);
    this._isLoadingList.set(false);
    this._isSaving.set(false);
  }

  public fetchRoutes(): void {
    this.fetchRoutesSubscription?.unsubscribe();
    this._isLoadingList.set(true);

    this.fetchRoutesSubscription = this.routeService
      .getRoutes()
      .pipe(finalize(() => this._isLoadingList.set(false)))
      .subscribe({
        next: (routes) => this._routes.set(routes),
        error: (err) => {
          this.logger.error('[RouteStateService] Error fetching routes', { error: err });
          this.notificationService.error('ERRORS.FETCH_ROUTES_FAILED');
        },
      });
  }

  public selectRoute(routeId: number): void {
    if (this._selectedRoute()?.id === routeId) {
      this.clearSelectedRoute();
      return;
    }

    this.selectRouteSubscription?.unsubscribe();
    this._selectedRoute.set(null);

    this.selectRouteSubscription = this.routeService.getRouteDetails(routeId).subscribe({
      next: (routeDetails) => this._selectedRoute.set(routeDetails),
      error: (err) => {
        this.logger.error('[RouteStateService] Error fetching route details', { error: err });
        this.notificationService.error('ERRORS.FETCH_ROUTE_DETAILS_FAILED');
      },
    });
  }

  public clearSelectedRoute(): void {
    this.selectRouteSubscription?.unsubscribe();
    this._selectedRoute.set(null);
  }

  public deleteRoute(routeId: number): void {
    this.routeService.deleteRoute(routeId).subscribe({
      next: () => {
        this._routes.update((routes) => routes.filter((r) => r.id !== routeId));
        this.logger.info('[RouteStateService] Route successfully deleted', { routeId });
        if (this._selectedRoute()?.id === routeId) {
          this._selectedRoute.set(null);
        }
      },
      error: (err) => {
        if (err.status === 404) {
          this.logger.debug('[RouteStateService] Route to delete not found:', { routeId });
        } else {
          this.logger.error('[RouteStateService] Error deleting route:', { error: err });
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
          this.logger.info('[RouteStateService] Route successfully saved', {
            routeName: newRouteDetails.name,
          });
        },
        error: (err) => {
          if (err.status === 400) {
            this.logger.debug('[RouteStateService] Route validation failed on server', {
              error: err.error,
            });
            this.notificationService.error('ERRORS.ROUTE_TOO_SHORT');
          } else {
            this.logger.error('[RouteStateService] Error saving route', { error: err });
            this.notificationService.error('ERRORS.SAVE_ROUTE_FAILED');
          }
        },
      });
  }

  public downloadGpx(routeId: number, routeName?: string): void {
    const defaultName =
      routeName ??
      this._routes().find((r) => r.id === routeId)?.name ??
      (this._selectedRoute()?.id === routeId ? this._selectedRoute()?.name : undefined) ??
      `route_${routeId}`;

    this.routeService.exportGpx(routeId).subscribe({
      next: (blob) => {
        triggerBlobDownload(blob, sanitizeFileName(defaultName, 'gpx'));
        this.logger.debug('[RouteStateService] GPX download initiated successfully');
      },
      error: () => {
        this.logger.error('[RouteStateService] Error exporting GPX', { routeId });
        this.notificationService.error('ERRORS.DOWNLOAD_GPX_ERROR');
      },
    });
  }

  public exportGpx(routeId: number): void {
    this.downloadGpx(routeId);
  }

  public moveRouteToFolder(routeId: number, folderId: number | null): void {
    const currentRoute = this._routes().find((r) => r.id === routeId);
    if (currentRoute?.folderId === folderId) {
      return;
    }

    this.routeService.updateRouteFolder(routeId, { folderId }).subscribe({
      next: () => {
        this.logger.info('[RouteStateService] Route moved to new folder.', {
          routeId: routeId,
          targetFolderId: folderId,
        });
        this._routes.update((routes) =>
          routes.map((r) => (r.id === routeId ? { ...r, folderId } : r)),
        );
      },
      error: (err) => {
        if (err.status === 400 || err.status === 404) {
          this.logger.debug('[RouteStateService] Route move validation error:', {
            routeId,
            folderId,
            error: err.error,
          });
        } else {
          this.logger.error('[RouteStateService] Error moving route:', { error: err });
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
    const trimmed = newName.trim();
    if (!trimmed) {
      return;
    }
    const currentRoute = this._routes().find((r) => r.id === routeId);
    if (currentRoute?.name === trimmed) {
      return;
    }

    this.routeService.renameRoute(routeId, { name: trimmed }).subscribe({
      next: () => {
        this.logger.info('[RouteStateService] Route renamed successfully.', {
          routeId: routeId,
          newName: trimmed,
        });
        this._routes.update((routes) =>
          routes.map((r) => (r.id === routeId ? { ...r, name: trimmed } : r)),
        );
      },
      error: (err) => {
        if (err.status === 400 || err.status === 404) {
          this.logger.debug('[RouteStateService] Route rename validation error:', {
            routeId,
            newName: trimmed,
            error: err.error,
          });
        } else {
          this.logger.error('[RouteStateService] Error renaming route:', { error: err });
        }
        this.notificationService.error('ERRORS.RENAME_ROUTE_FAILED');
      },
    });
  }
}
