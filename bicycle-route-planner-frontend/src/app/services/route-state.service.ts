import { Injectable, inject, signal } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { RouteDetailDto, RouteSummaryDto, CreateRouteRequestDto } from '../models/route.model';
import { RouteService } from './route.service';

import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class RouteStateService {
  private readonly routeService = inject(RouteService);
  private readonly logger = inject(LoggerService);

  private readonly _routes = signal<RouteSummaryDto[]>([]);
  private readonly _selectedRoute = signal<RouteDetailDto | null>(null);

  private readonly _isLoadingList = signal<boolean>(false);
  private readonly _isSaving = signal<boolean>(false);
  private readonly _error = signal<string | null>(null);

  public readonly routes = this._routes.asReadonly();
  public readonly selectedRoute = this._selectedRoute.asReadonly();

  public readonly isLoadingList = this._isLoadingList.asReadonly();
  public readonly isSaving = this._isSaving.asReadonly();
  public readonly error = this._error.asReadonly();

  public fetchRoutes(): void {
    this._isLoadingList.set(true);
    this._error.set(null);

    this.routeService
      .getRoutes()
      .pipe(finalize(() => this._isLoadingList.set(false)))
      .subscribe({
        next: (routes) => this._routes.set(routes),
        error: (err) => {
          this.logger.error('Error fetching routes', err);
          this._error.set('Failed to fetch routes list.');
        },
      });
  }

  public selectRoute(routeId: number): void {
    if (this._selectedRoute()?.id === routeId) {
      this.clearSelectedRoute();
      return;
    }

    this._error.set(null);
    this._selectedRoute.set(null);

    this.routeService
      .getRouteDetails(routeId)
      .subscribe({
        next: (routeDetails) => this._selectedRoute.set(routeDetails),
        error: (err) => {
          this.logger.error('Error fetching route details', err);
          this._error.set('Failed to fetch route details.');
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
        this.logger.info('Route successfully deleted', routeId);
        if (this._selectedRoute()?.id === routeId) {
          this._selectedRoute.set(null);
        }
      },
      error: (err) => {
        this.logger.error('Error deleting route', err);
        this._error.set('Failed to delete route.');
      },
    });
  }

  public createRoute(dto: CreateRouteRequestDto, onSuccess?: () => void): void {
    this._isSaving.set(true);
    this._error.set(null);

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
          this.logger.info('Route successfully saved', newRouteDetails.name);
        },
        error: (err) => {
          this.logger.error('Error saving route', err);
          this._error.set('Failed to save route.');
        },
      });
  }

  public exportGpx(routeId: number): void {
    const route = this._routes().find((r) => r.id === routeId);
    const routeName = route ? route.name : `trasa_${routeId}`;
    this.routeService.downloadGpx(routeId, routeName);
  }

  public moveRouteToFolder(routeId: number, folderId: number | null): void {
    this.routeService.updateRouteFolder(routeId, { folderId }).subscribe({
      next: () => {
        this._routes.update((routes) =>
          routes.map((r) => (r.id === routeId ? { ...r, folderId } : r)),
        );
      },
      error: (err) => {
        this.logger.error('Error moving route', err);
        this._error.set('Failed to move route.');
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
        this._routes.update((routes) =>
          routes.map((r) => (r.id === routeId ? { ...r, name: newName } : r)),
        );
      },
      error: (err) => {
        this.logger.error('Error renaming route', err);
        this._error.set('Failed to rename route.');
      },
    });
  }
}
