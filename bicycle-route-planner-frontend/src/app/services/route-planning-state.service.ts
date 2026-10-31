import { Injectable, inject, signal, computed, effect, untracked } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import * as L from 'leaflet';
import { Observable, Subscription } from 'rxjs';
import type { Position } from 'geojson';
import { OrsService } from './ors.service';
import { RouteGeometryService } from './route-geometry.service';
import { AvoidanceZoneStateService } from './avoidance-zone-state.service';
import { RouteStateService } from './route-state.service';
import { LoggerService } from './logger.service';
import { AuthService } from './auth.service';
import { NotificationService } from './notification.service';
import { RouteType, RoutingMode, ActiveRouteGeometry, RouteDraft } from '../models/route.model';
import { AvoidPolygonsGeoJson, OrsGeoJsonResponse } from '../models/ors.model';

@Injectable({
  providedIn: 'root',
})
export class RoutePlanningStateService {
  private static readonly MIN_WAYPOINTS_FOR_ROUTE = 2;
  private static readonly DEFAULT_ROUND_TRIP_DISTANCE_M = 20_000;
  private static readonly MAX_ROUND_TRIP_SEED = 100;

  private readonly orsService = inject(OrsService);
  private readonly routeGeometryService = inject(RouteGeometryService);
  private readonly avoidanceZoneStateService = inject(AvoidanceZoneStateService);
  private readonly routeStateService = inject(RouteStateService);
  private readonly logger = inject(LoggerService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  private pendingRequest?: Subscription;

  private roundTripSeed?: number;

  private readonly _waypoints = signal<readonly L.LatLng[]>([]);
  public readonly waypoints = this._waypoints.asReadonly();

  private readonly _currentRouteDraft = signal<RouteDraft | null>(null);
  public readonly currentRouteDraft = this._currentRouteDraft.asReadonly();

  private readonly _activeRouteGeometry = signal<ActiveRouteGeometry | null>(null);
  public readonly activeRouteGeometry = this._activeRouteGeometry.asReadonly();

  private readonly _routingMode = signal<RoutingMode>('direct');
  public readonly routingMode = this._routingMode.asReadonly();

  private readonly _roundTripDistanceMeters = signal(
    RoutePlanningStateService.DEFAULT_ROUND_TRIP_DISTANCE_M,
  );
  public readonly roundTripDistanceMeters = this._roundTripDistanceMeters.asReadonly();
  public readonly roundTripDistanceKm = computed(() => this.roundTripDistanceMeters() / 1000);

  private readonly activeZonesKey = computed(() =>
    this.avoidanceZoneStateService
      .zones()
      .filter((zone) => zone.isActive)
      .map((zone) => String(zone.id ?? zone.zoneGeom))
      .sort()
      .join(','),
  );

  constructor() {
    effect(() => {
      if (!this.authService.currentUser()) {
        this.logger.debug('[RoutePlanningState] User logged out, clearing planning state');
        this.clearRoute();
        this._routingMode.set('direct');
      }
    });

    effect(() => {
      this.activeZonesKey();
      untracked(() => this.handleAvoidanceZonesChanged());
    });

    effect(() => {
      if (this.routeStateService.selectedRoute()) {
        untracked(() => this.clearRoute());
      }
    });
  }

  public setRoutingMode(mode: RoutingMode): void {
    this._routingMode.set(mode);
  }

  public setRoundTripDistance(distanceMeters: number): void {
    if (Number.isFinite(distanceMeters) && distanceMeters > 0) {
      this._roundTripDistanceMeters.set(distanceMeters);
    }
  }

  public addWaypoint(latLng: L.LatLng): void {
    this.clearSelectedRoute();

    if (this._activeRouteGeometry()?.routeType === 'LOOP') {
      this.cancelPendingRequest();
      this.resetRouteResult();
    }

    this._waypoints.update((w) => [...w, latLng]);
    this.recalculateDirectRoute();
  }

  public updateWaypoint(index: number, latLng: L.LatLng): void {
    if (!this.isValidWaypointIndex(index)) {
      return;
    }

    this._waypoints.update((w) => w.map((point, i) => (i === index ? latLng : point)));
    this.recalculateDirectRoute();
  }

  public deleteWaypoint(index: number): void {
    if (!this.isValidWaypointIndex(index)) {
      return;
    }

    this._waypoints.update((w) => w.filter((_, i) => i !== index));
    this.logger.debug('[RoutePlanningState] Waypoint deleted', {
      remainingCount: this._waypoints().length,
    });

    if (this.hasEnoughWaypoints()) {
      this.recalculateDirectRoute();
    } else {
      this.cancelPendingRequest();
      this.resetRouteResult();
    }
  }

  public recalculateDirectRoute(): void {
    const waypoints = this._waypoints();
    if (!this.hasEnoughWaypoints()) {
      return;
    }

    this.cancelPendingRequest();
    const avoidPolygons = this.getAvoidanceZones();

    if (this.routeGeometryService.hasWaypointInAvoidanceZone(waypoints, avoidPolygons)) {
      this.rejectWaypointInAvoidanceZone('Direct route waypoint is inside an avoidance zone');
      return;
    }

    const coords: Position[] = waypoints.map((w) => [w.lng, w.lat]);
    this.logger.debug('[RoutePlanningState] Fetching direct route from ORS', {
      waypointsCount: coords.length,
    });

    this.request(
      this.orsService.getRouteGeoJson(coords, avoidPolygons),
      'ERRORS.ROUTE_NOT_FOUND',
      '[RoutePlanningState] Route error from ORS',
      (data) => this.applyRoute(data, 'POINT_TO_POINT'),
      waypoints.length,
    );
  }

  public generateRoundTrip(startPoint: L.LatLng): void {
    this.clearSelectedRoute();
    if (this._waypoints().length > 0) {
      this._waypoints.set([]);
    }

    this.requestRoundTrip(L.latLng(startPoint), this.randomSeed());
  }

  public clearRoute(): void {
    this.cancelPendingRequest();
    this._waypoints.set([]);
    this.resetRouteResult();
  }

  private requestRoundTrip(startPoint: L.LatLng, seed: number): void {
    const distance = this.roundTripDistanceMeters();
    const current = this._activeRouteGeometry();
    const isSameStartPoint =
      current?.routeType === 'LOOP' && !!current.startPoint?.equals(startPoint);

    this.cancelPendingRequest();
    const avoidPolygons = this.getAvoidanceZones();

    if (this.routeGeometryService.hasWaypointInAvoidanceZone([startPoint], avoidPolygons)) {
      this.rejectWaypointInAvoidanceZone('Round trip start point is inside an avoidance zone');
      return;
    }

    if (!isSameStartPoint) {
      this._activeRouteGeometry.set({ coordinates: [], routeType: 'LOOP', startPoint });
      this._currentRouteDraft.set(null);
    }

    this.roundTripSeed = seed;
    this.logger.debug('[RoutePlanningState] Generating round trip from ORS', {
      startPoint,
      distance,
      seed,
    });

    const request$ = this.orsService.getRoundTripGeoJson({
      startCoord: [startPoint.lng, startPoint.lat],
      distanceMeters: distance,
      avoidPolygons,
      seed,
    });

    this.request(
      request$,
      'ERRORS.ROUND_TRIP_NOT_FOUND',
      '[RoutePlanningState] Round trip error',
      (data) => this.applyRoute(data, 'LOOP', startPoint, distance),
    );
  }

  private request(
    request$: Observable<OrsGeoJsonResponse>,
    errorKey: string,
    logMessage: string,
    onSuccess: (data: OrsGeoJsonResponse) => void,
    waypointsCount?: number,
  ): void {
    this.pendingRequest = request$.subscribe({
      next: onSuccess,
      error: (err) => this.handleRoutingFailure(errorKey, logMessage, err, waypointsCount),
    });
  }

  private applyRoute(
    data: OrsGeoJsonResponse,
    routeType: RouteType,
    startPoint?: L.LatLng,
    targetDistance = 0,
  ): void {
    const feature = data.error ? undefined : data.features?.[0];
    if (!feature?.geometry?.coordinates?.length) {
      const errorKey =
        routeType === 'LOOP' ? 'ERRORS.ROUND_TRIP_NOT_FOUND' : 'ERRORS.ROUTE_NOT_FOUND';
      this.handleRoutingFailure(errorKey, '[RoutePlanningState] No route data found from ORS');
      return;
    }

    const coordinates = feature.geometry.coordinates.map(([lng, lat, alt]) =>
      L.latLng(lat, lng, alt),
    );
    const { geometryWkt, startPoint: startPointWkt } =
      this.routeGeometryService.generateRouteWkt(coordinates);
    const properties = feature.properties;

    this._currentRouteDraft.set({
      routeType,
      geometryWkt,
      startPoint: startPointWkt,
      elevationGain: this.sanitizeElevation(properties?.ascent),
      elevationLoss: this.sanitizeElevation(properties?.descent),
      distanceMeters: properties?.summary?.distance ?? 0,
      targetDistance,
    });

    this._activeRouteGeometry.set({
      coordinates,
      routeType,
      ...(startPoint && { startPoint }),
    });
  }

  private handleRoutingFailure(
    errorKey: string,
    logMessage: string,
    err?: unknown,
    waypointsCount?: number,
  ): void {
    const httpErr = err instanceof HttpErrorResponse ? err : undefined;

    this.logger.error(logMessage, {
      status: httpErr?.status,
      orsErrorCode: httpErr?.error?.error?.code ?? httpErr?.error?.code,
      ...(waypointsCount !== undefined && { waypointsCount }),
      isNetworkError: httpErr?.status === 0,
    });

    this.resetRouteResult();
    this.notificationService.error(errorKey);
  }

  private handleAvoidanceZonesChanged(): void {
    if (this.hasEnoughWaypoints()) {
      this.logger.debug('[RoutePlanningState] Avoidance zones changed, recalculating direct route');
      this.recalculateDirectRoute();
      return;
    }

    const current = this._activeRouteGeometry();
    if (current?.routeType === 'LOOP' && current.startPoint) {
      this.logger.debug('[RoutePlanningState] Avoidance zones changed, recalculating round trip');
      this.requestRoundTrip(current.startPoint, this.roundTripSeed ?? this.randomSeed());
    }
  }

  private getAvoidanceZones(): AvoidPolygonsGeoJson | undefined {
    const coordinates = this.avoidanceZoneStateService
      .zones()
      .filter((zone) => zone.isActive && !!zone.zoneGeom)
      .flatMap((zone) => this.routeGeometryService.extractPolygonCoordinates(zone.zoneGeom));

    return coordinates.length > 0 ? { type: 'MultiPolygon', coordinates } : undefined;
  }

  private clearSelectedRoute(): void {
    if (this.routeStateService.selectedRoute()) {
      this.routeStateService.clearSelectedRoute();
    }
  }

  private rejectWaypointInAvoidanceZone(logMessage: string): void {
    this.logger.debug(`[RoutePlanningState] ${logMessage}`);
    this.resetRouteResult();
    this.notificationService.warning('ERRORS.WAYPOINT_IN_AVOIDANCE_ZONE');
  }

  private resetRouteResult(): void {
    this._activeRouteGeometry.set(null);
    this._currentRouteDraft.set(null);
  }

  private cancelPendingRequest(): void {
    this.pendingRequest?.unsubscribe();
    this.pendingRequest = undefined;
  }

  private hasEnoughWaypoints(): boolean {
    return this._waypoints().length >= RoutePlanningStateService.MIN_WAYPOINTS_FOR_ROUTE;
  }

  private isValidWaypointIndex(index: number): boolean {
    return index >= 0 && index < this._waypoints().length;
  }

  private sanitizeElevation(value: unknown): number {
    return typeof value === 'number' && Number.isFinite(value) && value > 0 ? value : 0;
  }

  private randomSeed(): number {
    return Math.floor(Math.random() * RoutePlanningStateService.MAX_ROUND_TRIP_SEED) + 1;
  }
}
