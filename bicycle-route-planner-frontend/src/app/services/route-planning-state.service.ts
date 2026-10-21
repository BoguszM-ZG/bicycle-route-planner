import { Injectable, NgZone, inject, signal, computed, effect, untracked } from '@angular/core';
import * as L from 'leaflet';
import { Subscription } from 'rxjs';
import * as wellknown from 'wellknown';
import type { GeometryObject, Position } from 'geojson';
import { OrsService } from './ors.service';
import { RouteCalculationService } from './route-calculation.service';
import { AvoidanceZoneStateService } from './avoidance-zone-state.service';
import { RouteStateService } from './route-state.service';
import { LoggerService } from './logger.service';
import { AuthService } from './auth.service';
import { NotificationService } from './notification.service';
import {
  RouteDraft,
  OrsGeoJsonResponse,
  RouteType,
  RoutingMode,
  AvoidPolygonsGeoJson,
  ActiveRouteGeometry,
} from '../models/route.model';

@Injectable({
  providedIn: 'root',
})
export class RoutePlanningStateService {
  private static readonly DEFAULT_ROUND_TRIP_METERS = 20000;
  private static readonly MIN_WAYPOINTS_FOR_ROUTE = 2;
  private static readonly METERS_PER_KILOMETER = 1000;

  private readonly ngZone = inject(NgZone);
  private readonly orsService = inject(OrsService);
  private readonly routeCalculationService = inject(RouteCalculationService);
  private readonly avoidanceZoneStateService = inject(AvoidanceZoneStateService);
  private readonly routeStateService = inject(RouteStateService);
  private readonly logger = inject(LoggerService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  private waypoints: L.LatLng[] = [];
  private lastRoundTripStartPoint?: L.LatLng;
  private lastRoundTripDistance?: number;
  private directRouteSubscription?: Subscription;
  private roundTripSubscription?: Subscription;
  private previousActiveZonesKey?: string;

  public readonly currentRouteDraft = signal<RouteDraft | null>(null);
  public readonly routingMode = signal<RoutingMode>('direct');
  public readonly roundTripDistanceMeters = signal<number>(
    RoutePlanningStateService.DEFAULT_ROUND_TRIP_METERS,
  );
  public readonly roundTripDistanceKm = computed(
    () => this.roundTripDistanceMeters() / RoutePlanningStateService.METERS_PER_KILOMETER,
  );
  public readonly activeRouteGeometry = signal<ActiveRouteGeometry | null>(null);

  constructor() {
    effect(() => {
      const user = this.authService.currentUser();
      if (!user) {
        this.logger.debug(
          '[RoutePlanningState] User logged out, clearing RoutePlanningStateService',
        );
        this.clearRoute();
        this.routingMode.set('direct');
      }
    });

    effect(() => {
      const activeZones = this.avoidanceZoneStateService
        .zones()
        .filter((zone) => zone.isActive);
      const activeZonesKey = activeZones
        .map((z) => `${z.id ?? ''}:${z.name ?? ''}:${z.zoneGeom}`)
        .sort((a: string, b: string) => a.localeCompare(b))
        .join('|');

      if (this.previousActiveZonesKey === undefined) {
        this.previousActiveZonesKey = activeZonesKey;
        return;
      }

      if (this.previousActiveZonesKey === activeZonesKey) {
        return;
      }

      this.previousActiveZonesKey = activeZonesKey;

      untracked(() => {
        this.handleAvoidanceZonesChanged();
      });
    });
  }

  public getWaypoints(): readonly L.LatLng[] {
    return this.waypoints;
  }

  public addWaypoint(latLng: L.LatLng): void {
    if (this.routeStateService.selectedRoute()) {
      this.routeStateService.clearSelectedRoute();
    }

    this.waypoints.push(latLng);

    if (this.waypoints.length >= RoutePlanningStateService.MIN_WAYPOINTS_FOR_ROUTE) {
      this.recalculateDirectRoute();
    }
  }

  public updateWaypoint(index: number, latLng: L.LatLng): void {
    if (index < 0 || index >= this.waypoints.length) {
      return;
    }

    this.waypoints[index] = latLng;

    if (this.waypoints.length >= RoutePlanningStateService.MIN_WAYPOINTS_FOR_ROUTE) {
      this.recalculateDirectRoute();
    }
  }

  public deleteWaypoint(index: number): void {
    if (index < 0 || index >= this.waypoints.length) {
      return;
    }

    this.waypoints.splice(index, 1);
    this.logger.debug('[RoutePlanningState] Waypoint deleted', {
      remainingCount: this.waypoints.length,
    });

    if (this.waypoints.length >= RoutePlanningStateService.MIN_WAYPOINTS_FOR_ROUTE) {
      this.recalculateDirectRoute();
    } else if (this.waypoints.length === 1) {
      this.cancelDirectRouteRequest();
      this.activeRouteGeometry.set(null);
      this.currentRouteDraft.set(null);
    } else {
      this.clearRoute();
    }
  }

  public recalculateDirectRoute(): void {
    if (this.waypoints.length < RoutePlanningStateService.MIN_WAYPOINTS_FOR_ROUTE) {
      return;
    }

    this.cancelDirectRouteRequest();

    const avoidPolygonsGeoJson = this.getAvoidanceZones();

    if (this.routeCalculationService.hasWaypointInAvoidanceZone(this.waypoints, avoidPolygonsGeoJson)) {
      this.logger.debug('[RoutePlanningState] Direct route waypoint is inside an avoidance zone');
      this.activeRouteGeometry.set(null);
      this.currentRouteDraft.set(null);
      this.notificationService.warning('ERRORS.WAYPOINT_IN_AVOIDANCE_ZONE');
      return;
    }

    const coords: Position[] = this.waypoints.map((w) => [w.lng, w.lat]);
    this.logger.debug('[RoutePlanningState] Fetching direct route from ORS', {
      waypointsCount: coords.length,
    });

    this.directRouteSubscription = this.orsService
      .getRouteGeoJson(coords, avoidPolygonsGeoJson)
      .subscribe({
        next: (data) => this.handleDirectRouteSuccess(data),
        error: (err) => this.handleDirectRouteError(err),
      });
  }

  private handleDirectRouteSuccess(data: OrsGeoJsonResponse): void {
    this.ngZone.run(() => {
      if (this.waypoints.length < RoutePlanningStateService.MIN_WAYPOINTS_FOR_ROUTE) {
        return;
      }

      if (!this.hasValidRouteFeature(data)) {
        this.logger.error('[RoutePlanningState] No route data found from ORS');
        this.activeRouteGeometry.set(null);
        this.currentRouteDraft.set(null);
        this.notificationService.error('ERRORS.ROUTE_NOT_FOUND');
        return;
      }

      const feature = data.features![0];
      const geometry = feature.geometry.coordinates;
      const summary = feature.properties?.summary;

      const routeCoordinates = geometry.map((coord: Position) =>
        L.latLng(coord[1], coord[0], coord[2]),
      );

      this.updateRouteData(routeCoordinates, summary?.distance ?? 0, 'POINT_TO_POINT');

      this.activeRouteGeometry.set({
        coordinates: routeCoordinates,
        routeType: 'POINT_TO_POINT',
      });
    });
  }

  private handleDirectRouteError(err: unknown): void {
    this.logger.error('[RoutePlanningState] Route error from ORS', { error: err });
    this.ngZone.run(() => {
      this.activeRouteGeometry.set(null);
      this.currentRouteDraft.set(null);
      this.notificationService.error('ERRORS.ROUTE_NOT_FOUND');
    });
  }

  public generateRoundTrip(startPoint: L.LatLng, distanceMeters?: number): void {
    if (this.routeStateService.selectedRoute()) {
      this.routeStateService.clearSelectedRoute();
    }

    const distance = distanceMeters ?? this.roundTripDistanceMeters();
    this.lastRoundTripStartPoint = startPoint;
    this.lastRoundTripDistance = distance;

    this.cancelRoundTripRequest();

    const avoidPolygonsGeoJson = this.getAvoidanceZones();

    if (this.routeCalculationService.hasWaypointInAvoidanceZone([startPoint], avoidPolygonsGeoJson)) {
      this.logger.debug('[RoutePlanningState] Round trip start point is inside an avoidance zone');
      this.activeRouteGeometry.set(null);
      this.currentRouteDraft.set(null);
      this.notificationService.warning('ERRORS.WAYPOINT_IN_AVOIDANCE_ZONE');
      return;
    }

    this.logger.debug('[RoutePlanningState] Generating round trip from ORS', {
      startPoint,
      distance,
    });
    const coords: Position[] = [[startPoint.lng, startPoint.lat]];

    this.roundTripSubscription = this.orsService
      .getRoundTripGeoJson(coords[0], distance, avoidPolygonsGeoJson)
      .subscribe({
        next: (data) => this.handleRoundTripSuccess(data, startPoint, distance),
        error: (err) => this.handleRoundTripError(err),
      });
  }

  private handleRoundTripSuccess(
    data: OrsGeoJsonResponse,
    startPoint: L.LatLng,
    targetDistance: number,
  ): void {
    this.ngZone.run(() => {
      if (!this.hasValidRouteFeature(data)) {
        this.logger.error('[RoutePlanningState] No route data found from ORS');
        this.activeRouteGeometry.set(null);
        this.currentRouteDraft.set(null);
        this.notificationService.error('ERRORS.ROUND_TRIP_NOT_FOUND');
        return;
      }

      const feature = data.features![0];
      const routeCoordinates = feature.geometry.coordinates.map((coord: Position) =>
        L.latLng(coord[1], coord[0], coord[2]),
      );
      const summary = feature.properties?.summary;
      this.updateRouteData(routeCoordinates, summary?.distance ?? 0, 'LOOP', targetDistance);

      this.activeRouteGeometry.set({
        coordinates: routeCoordinates,
        routeType: 'LOOP',
        startPoint,
      });
    });
  }

  private handleRoundTripError(err: unknown): void {
    this.logger.error('[RoutePlanningState] Round trip error', { error: err });
    this.ngZone.run(() => {
      this.activeRouteGeometry.set(null);
      this.currentRouteDraft.set(null);
      this.notificationService.error('ERRORS.ROUND_TRIP_NOT_FOUND');
    });
  }

  private hasValidRouteFeature(data: OrsGeoJsonResponse): boolean {
    return Boolean(
      !data.error &&
      data.features &&
      data.features.length > 0 &&
      data.features[0]?.geometry?.coordinates &&
      data.features[0].geometry.coordinates.length > 0,
    );
  }

  private getAvoidanceZones(): AvoidPolygonsGeoJson | undefined {
    const activeZones = this.avoidanceZoneStateService
      .zones()
      .filter((zone) => zone.isActive && !!zone.zoneGeom);

    if (activeZones.length === 0) {
      return undefined;
    }

    const polygonCoordinates = activeZones.flatMap((zone) =>
      this.extractPolygonCoordinates(zone.zoneGeom),
    );

    if (polygonCoordinates.length === 0) {
      return undefined;
    }

    return {
      type: 'MultiPolygon',
      coordinates: polygonCoordinates,
    };
  }

  private extractPolygonCoordinates(zoneGeom: string): Position[][][] {
    try {
      const parsed = wellknown.parse(zoneGeom) as GeometryObject | null;
      if (!parsed) {
        return [];
      }

      if (parsed.type === 'Polygon') {
        return parsed.coordinates.length > 0 ? [parsed.coordinates] : [];
      }

      if (parsed.type === 'MultiPolygon') {
        return parsed.coordinates.filter((poly) => poly.length > 0);
      }

      return [];
    } catch (err) {
      this.logger.debug('[RoutePlanningState] Failed to parse avoidance zone geometry WKT', {
        error: err,
      });
      return [];
    }
  }

  private updateRouteData(
    coordinates: L.LatLng[],
    totalDistance: number,
    routeType: RouteType,
    targetDistance: number = 0,
  ): void {
    const stats = this.routeCalculationService.calculateRouteStats(coordinates);

    this.currentRouteDraft.set({
      name: '',
      routeType,
      geometryWkt: stats.geometryWkt,
      startPoint: stats.startPoint,
      elevationGain: stats.elevationGain,
      elevationLoss: stats.elevationLoss,
      distanceMeters: totalDistance,
      targetDistance,
    });
  }

  private cancelDirectRouteRequest(): void {
    if (this.directRouteSubscription) {
      this.directRouteSubscription.unsubscribe();
      this.directRouteSubscription = undefined;
    }
  }

  private cancelRoundTripRequest(): void {
    if (this.roundTripSubscription) {
      this.roundTripSubscription.unsubscribe();
      this.roundTripSubscription = undefined;
    }
  }

  public clearRoute(): void {
    this.cancelDirectRouteRequest();
    this.cancelRoundTripRequest();

    this.waypoints = [];
    this.lastRoundTripStartPoint = undefined;
    this.lastRoundTripDistance = undefined;
    this.activeRouteGeometry.set(null);
    this.currentRouteDraft.set(null);
  }

  private handleAvoidanceZonesChanged(): void {
    const hasDirectRoute =
      this.waypoints.length >= RoutePlanningStateService.MIN_WAYPOINTS_FOR_ROUTE;
    const hasRoundTrip =
      !hasDirectRoute &&
      (this.activeRouteGeometry()?.routeType === 'LOOP' || this.lastRoundTripStartPoint !== undefined);

    if (hasDirectRoute) {
      this.logger.debug(
        '[RoutePlanningState] Active avoidance zones changed, recalculating direct route',
      );
      this.recalculateDirectRoute();
    } else if (hasRoundTrip && this.lastRoundTripStartPoint) {
      this.logger.debug(
        '[RoutePlanningState] Active avoidance zones changed, recalculating round trip',
      );
      this.generateRoundTrip(
        this.lastRoundTripStartPoint,
        this.lastRoundTripDistance ?? this.roundTripDistanceMeters(),
      );
    }
  }

  public isPointInAvoidanceZone(point: L.LatLng): boolean {
    const avoidPolygons = this.getAvoidanceZones();
    return this.routeCalculationService.hasWaypointInAvoidanceZone([point], avoidPolygons);
  }
}
