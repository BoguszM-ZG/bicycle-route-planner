import { Injectable, inject, signal, computed, effect, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpStatusCode } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import {
  RouteDetailDto,
  RouteSummaryDto,
  CreateRouteRequestDto,
  ElevationPointDto,
  RouteExportFormat,
} from '../models/route.model';
import { RouteService } from './route.service';
import { fileNameFromContentDisposition, triggerBlobDownload } from '../utils/file-download.util';
import { LoggerService } from './logger.service';
import { AuthService } from './auth.service';
import { NotificationService } from './notification.service';
import { FolderStateService } from './folder-state.service';
import { AvoidanceZoneStateService } from './avoidance-zone-state.service';

@Injectable({
  providedIn: 'root',
})
export class RouteStateService {
  private readonly routeService = inject(RouteService);
  private readonly logger = inject(LoggerService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly folderStateService = inject(FolderStateService);
  private readonly avoidanceZoneStateService = inject(AvoidanceZoneStateService);

  private fetchRoutesSubscription?: Subscription;
  private selectRouteSubscription?: Subscription;
  private fetchElevationSubscription?: Subscription;
  private fetchIntersectingZonesSubscription?: Subscription;

  private readonly _routes = signal<RouteSummaryDto[]>([]);
  private readonly _selectedRouteId = signal<number | null>(null);
  private readonly _selectedRoute = signal<RouteDetailDto | null>(null);
  private readonly _elevationProfile = signal<ElevationPointDto[] | null>(null);
  private readonly _intersectingZones = signal<{ routeId: number; zoneIds: number[] } | null>(null);

  private readonly _isLoadingList = signal<boolean>(false);
  private readonly _isLoadingDetails = signal<boolean>(false);
  private readonly _isSaving = signal<boolean>(false);
  private readonly _isLoadingElevation = signal<boolean>(false);
  private readonly _hasElevationError = signal<boolean>(false);

  public readonly routes = this._routes.asReadonly();
  public readonly selectedRouteId = this._selectedRouteId.asReadonly();
  public readonly selectedRoute = this._selectedRoute.asReadonly();
  public readonly elevationProfile = this._elevationProfile.asReadonly();

  public readonly isLoadingList = this._isLoadingList.asReadonly();
  public readonly isLoadingDetails = this._isLoadingDetails.asReadonly();
  public readonly isSaving = this._isSaving.asReadonly();
  public readonly isLoadingElevation = this._isLoadingElevation.asReadonly();
  public readonly hasElevationError = this._hasElevationError.asReadonly();

  public readonly intersectingZones = computed(() => {
    const intersecting = this._intersectingZones();
    if (!intersecting || intersecting.routeId !== this.selectedRouteId()) {
      return [];
    }
    return this.avoidanceZoneStateService
      .zones()
      .filter((zone) => zone.isActive && intersecting.zoneIds.includes(zone.id));
  });

  private readonly zoneIdsKey = computed(() =>
    this.avoidanceZoneStateService
      .zones()
      .map((zone) => zone.id)
      .join(','),
  );

  constructor() {
    effect(() => {
      if (this.authService.isAuthenticated()) {
        untracked(() => this.fetchRoutes());
      } else {
        this.logger.debug('[RouteState] User logged out, clearing state');
        untracked(() => this.clearState());
      }
    });

    this.folderStateService.folderDeleted$
      .pipe(takeUntilDestroyed())
      .subscribe((folderId) => this.handleFolderDeleted(folderId));

    effect(() => {
      const routeId = this.selectedRouteId();
      this.zoneIdsKey();
      untracked(() => this.fetchIntersectingZones(routeId));
    });
  }

  private clearState(): void {
    this.fetchRoutesSubscription?.unsubscribe();
    this.selectRouteSubscription?.unsubscribe();
    this.fetchElevationSubscription?.unsubscribe();
    this.fetchIntersectingZonesSubscription?.unsubscribe();
    this._routes.set([]);
    this._selectedRouteId.set(null);
    this._selectedRoute.set(null);
    this._elevationProfile.set(null);
    this._intersectingZones.set(null);
    this._isLoadingList.set(false);
    this._isLoadingDetails.set(false);
    this._isSaving.set(false);
    this._isLoadingElevation.set(false);
    this._hasElevationError.set(false);
  }

  private fetchRoutes(): void {
    this.fetchRoutesSubscription?.unsubscribe();
    this._isLoadingList.set(true);

    this.fetchRoutesSubscription = this.routeService
      .getRoutes()
      .pipe(finalize(() => this._isLoadingList.set(false)))
      .subscribe({
        next: (routes) => this._routes.set(routes),
        error: (err) => {
          this.logger.error('[RouteState] Error fetching routes', {
            status: err.status,
            type: err.error?.type,
          });
          this.notificationService.error('ERRORS.FETCH_ROUTES_FAILED');
        },
      });
  }

  private fetchIntersectingZones(routeId: number | null): void {
    this.fetchIntersectingZonesSubscription?.unsubscribe();
    if (routeId === null) {
      this._intersectingZones.set(null);
      return;
    }

    this.fetchIntersectingZonesSubscription = this.routeService
      .getIntersectingZoneIds(routeId)
      .subscribe({
        next: (zoneIds) => this._intersectingZones.set({ routeId, zoneIds }),
        error: (err) => {
          this.logger.error('[RouteState] Error fetching intersecting zones', {
            routeId,
            status: err.status,
            type: err.error?.type,
          });
        },
      });
  }

  public selectRoute(routeId: number): void {
    if (this.selectedRouteId() === routeId) {
      this.clearSelectedRoute();
      return;
    }

    this.selectRouteSubscription?.unsubscribe();
    this._selectedRouteId.set(routeId);
    this._selectedRoute.set(null);
    this._isLoadingDetails.set(true);

    this.fetchElevationProfile(routeId);

    this.selectRouteSubscription = this.routeService
      .getRouteDetails(routeId)
      .pipe(finalize(() => this._isLoadingDetails.set(false)))
      .subscribe({
        next: (routeDetails) => {
          this._selectedRoute.set(routeDetails);
        },
        error: (err) => {
          this.clearSelectedRoute();
          this.logger.error('[RouteState] Error fetching route details', {
            status: err.status,
            type: err.error?.type,
          });
          this.notificationService.error('ERRORS.FETCH_ROUTE_DETAILS_FAILED');
        },
      });
  }

  public clearSelectedRoute(): void {
    this.selectRouteSubscription?.unsubscribe();
    this.fetchElevationSubscription?.unsubscribe();
    this._selectedRouteId.set(null);
    this._selectedRoute.set(null);
    this._elevationProfile.set(null);
    this._isLoadingElevation.set(false);
    this._isLoadingDetails.set(false);
    this._hasElevationError.set(false);
  }

  public deleteRoute(routeId: number): void {
    this.routeService.deleteRoute(routeId).subscribe({
      next: () => {
        this._routes.update((routes) => routes.filter((r) => r.id !== routeId));
        this.logger.info('[RouteState] Route successfully deleted', { routeId });
        if (this.selectedRouteId() === routeId) {
          this.clearSelectedRoute();
        }
        this.notificationService.success('SUCCESS.DELETE_ROUTE');
      },
      error: (err) => {
        if (err.status === HttpStatusCode.NotFound) {
          this.logger.debug('[RouteState] Route to delete not found', { routeId });
        } else {
          this.logger.error('[RouteState] Error deleting route', {
            status: err.status,
            type: err.error?.type,
          });
        }
        this.notificationService.error('ERRORS.DELETE_ROUTE_FAILED');
      },
    });
  }

  public createRoute(dto: CreateRouteRequestDto, onSuccess?: () => void): void {
    if (this._isSaving()) {
      return;
    }

    this._isSaving.set(true);

    this.routeService
      .addRoute(dto)
      .pipe(finalize(() => this._isSaving.set(false)))
      .subscribe({
        next: (newRouteDetails) => {
          this._isSaving.set(false);
          const summary: RouteSummaryDto = {
            id: newRouteDetails.id,
            name: newRouteDetails.name,
            distanceMeters: newRouteDetails.distanceMeters,
            folderId: newRouteDetails.folderId,
          };
          this._routes.update((routes) => [summary, ...routes]);

          this.logger.info('[RouteState] Route successfully saved', {
            routeName: newRouteDetails.name,
          });
          this.notificationService.success('SUCCESS.CREATE_ROUTE');
          onSuccess?.();
        },
        error: (err) => {
          if (err.status === HttpStatusCode.BadRequest) {
            this.logger.debug('[RouteState] Route validation failed on server', {
              status: err.status,
              type: err.error?.type,
            });
            const validationErrors = err.error?.validationErrors;
            const errorType = typeof err.error?.type === 'string' ? err.error.type : '';
            const hasGeometryError = Boolean(
              validationErrors?.['routeGeom'] ||
              validationErrors?.['startPoint'] ||
              errorType.includes('invalid-geometry'),
            );
            this.notificationService.error(
              hasGeometryError ? 'ERRORS.ROUTE_TOO_SHORT' : 'ERRORS.SAVE_ROUTE_FAILED',
            );
          } else {
            this.logger.error('[RouteState] Error saving route', {
              status: err.status,
              type: err.error?.type,
            });
            this.notificationService.error('ERRORS.SAVE_ROUTE_FAILED');
          }
        },
      });
  }

  public downloadRoute(routeId: number, format: RouteExportFormat): void {
    this.routeService.exportRoute(routeId, format).subscribe({
      next: (response) => {
        const fileName = fileNameFromContentDisposition(
          response.headers.get('Content-Disposition'),
          `route.${format}`,
        );
        triggerBlobDownload(response.body, fileName);
        this.logger.debug('[RouteState] Route download initiated successfully', { format });
      },
      error: (err) => {
        this.logger.error('[RouteState] Error exporting route', {
          routeId,
          status: err.status,
          type: err.error?.type,
        });
        this.notificationService.error('ERRORS.DOWNLOAD_ROUTE_ERROR');
      },
    });
  }

  public moveRouteToFolder(routeId: number, folderId: number | null): void {
    if (this.findRoute(routeId)?.folderId === folderId) {
      return;
    }

    this.routeService.updateRouteFolder(routeId, { folderId }).subscribe({
      next: () => {
        this.logger.info('[RouteState] Route moved to new folder', {
          routeId,
          targetFolderId: folderId,
        });
        this._routes.update((routes) =>
          routes.map((r) => (r.id === routeId ? { ...r, folderId } : r)),
        );
        if (this._selectedRoute()?.id === routeId) {
          this._selectedRoute.update((current) => (current ? { ...current, folderId } : null));
        }
        this.notificationService.success('SUCCESS.MOVE_ROUTE');
      },
      error: (err) => {
        if (err.status === HttpStatusCode.BadRequest || err.status === HttpStatusCode.NotFound) {
          this.logger.debug('[RouteState] Route move validation error', {
            routeId,
            status: err.status,
            type: err.error?.type,
          });
        } else {
          this.logger.error('[RouteState] Error moving route', {
            status: err.status,
            type: err.error?.type,
          });
        }
        this.notificationService.error('ERRORS.MOVE_ROUTE_FAILED');
      },
    });
  }

  private handleFolderDeleted(folderId: number): void {
    if (this._routes().some((r) => r.folderId === folderId)) {
      this._routes.update((routes) =>
        routes.map((r) => (r.folderId === folderId ? { ...r, folderId: null } : r)),
      );
    }
    if (this._selectedRoute()?.folderId === folderId) {
      this._selectedRoute.update((current) => (current ? { ...current, folderId: null } : null));
    }
  }

  public renameRoute(routeId: number, newName: string): void {
    const trimmed = newName.trim();

    const currentName = this.findRoute(routeId)?.name;
    if (currentName === trimmed) {
      return;
    }

    this.routeService.renameRoute(routeId, { name: trimmed }).subscribe({
      next: () => {
        this.logger.info('[RouteState] Route renamed successfully', {
          routeId,
          newName: trimmed,
        });
        this._routes.update((routes) =>
          routes.map((r) => (r.id === routeId ? { ...r, name: trimmed } : r)),
        );
        if (this._selectedRoute()?.id === routeId) {
          this._selectedRoute.update((current) => (current ? { ...current, name: trimmed } : null));
        }
        this.notificationService.success('SUCCESS.RENAME_ROUTE');
      },
      error: (err) => {
        if (err.status === HttpStatusCode.BadRequest || err.status === HttpStatusCode.NotFound) {
          this.logger.debug('[RouteState] Route rename validation error', {
            routeId,
            status: err.status,
            type: err.error?.type,
          });
        } else {
          this.logger.error('[RouteState] Error renaming route', {
            status: err.status,
            type: err.error?.type,
          });
        }
        this.notificationService.error('ERRORS.RENAME_ROUTE_FAILED');
      },
    });
  }

  private fetchElevationProfile(routeId: number): void {
    this.fetchElevationSubscription?.unsubscribe();
    this._isLoadingElevation.set(true);
    this._elevationProfile.set(null);
    this._hasElevationError.set(false);

    this.fetchElevationSubscription = this.routeService
      .getElevationProfile(routeId)
      .pipe(finalize(() => this._isLoadingElevation.set(false)))
      .subscribe({
        next: (points) => {
          this._hasElevationError.set(false);
          this._elevationProfile.set(points);
        },
        error: (err) => {
          this.logger.error('[RouteState] Error fetching elevation profile', {
            routeId,
            status: err.status,
            type: err.error?.type,
          });
          this._hasElevationError.set(true);
          this._elevationProfile.set(null);
        },
      });
  }

  private findRoute(routeId: number): RouteSummaryDto | undefined {
    return this._routes().find((r) => r.id === routeId);
  }
}
