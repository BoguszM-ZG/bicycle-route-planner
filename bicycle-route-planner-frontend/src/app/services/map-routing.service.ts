import { Injectable, NgZone, inject, signal, computed, effect } from '@angular/core';
import * as L from 'leaflet';
import { OrsService } from './ors.service';
import { RouteCalculationService } from './route-calculation.service';
import { RouteDraft, OrsGeoJsonResponse, RouteType } from '../models/route.model';
import * as wellknown from 'wellknown';
import { AvoidanceZoneStateService } from './avoidance-zone-state.service';
import { MAP_CONSTANTS } from '../constants/map.constants';
import { LoggerService } from './logger.service';
import { AuthService } from './auth.service';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root',
})
export class MapRoutingService {
  private static readonly DEFAULT_ROUND_TRIP_METERS = 20000;
  private static readonly MIN_WAYPOINTS_FOR_ROUTE = 2;

  private readonly ngZone = inject(NgZone);
  private readonly orsService = inject(OrsService);
  private readonly routeCalculationService = inject(RouteCalculationService);
  private readonly avoidanceZoneStateService = inject(AvoidanceZoneStateService);
  private readonly logger = inject(LoggerService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  private map?: L.Map;
  private waypoints: L.LatLng[] = [];
  private waypointMarkers: L.Marker[] = [];
  private directRouteLayer?: L.Polyline;
  private roundTripLayer?: L.Polyline;
  private roundTripMarker?: L.Marker;

  public readonly currentRouteDraft = signal<RouteDraft | null>(null);
  public readonly routingMode = signal<'direct' | 'roundTrip' | 'avoidanceZone'>('direct');
  public readonly roundTripDistanceMeters = signal<number>(MapRoutingService.DEFAULT_ROUND_TRIP_METERS);
  public readonly roundTripDistanceKm = computed(() => this.roundTripDistanceMeters() / 1000);

  constructor() {
    effect(() => {
      const user = this.authService.currentUser();
      if (!user) {
        this.logger.debug('User logged out, clearing MapRoutingService');
        this.clearRoute();
      }
    });
  }

  public initRouting(map: L.Map): void {
    if (this.map) {
      this.destroyRouting();
    }

    this.map = map;
    this.setupMapEventListeners(map);
  }

  private setupMapEventListeners(map: L.Map): void {
    let isDrawingMode = false;
    map.on('draw:drawstart', () => (isDrawingMode = true));
    map.on('draw:drawstop', () => (isDrawingMode = false));

    map.on('click', (e: L.LeafletMouseEvent) => this.handleMapClick(e, map, isDrawingMode));
  }

  private handleMapClick(e: L.LeafletMouseEvent, map: L.Map, isDrawingMode: boolean): void {
    if (isDrawingMode || this.routingMode() === 'avoidanceZone') {
      return;
    }

    if (this.routingMode() === 'roundTrip') {
      this.generateRoundTrip(e.latlng, this.roundTripDistanceMeters(), map);
      return;
    }

    this.addWaypoint(e.latlng, map);
  }

  public addWaypoint(latLng: L.LatLng, map: L.Map): void {
    this.waypoints.push(latLng);
    const index = this.waypoints.length - 1;

    const marker = L.marker(latLng, {
      draggable: true,
      icon: MAP_CONSTANTS.DEFAULT_ICON,
    }).addTo(map);

    marker.on('dragend', (event: any) => {
      const targetMarker = event.target as L.Marker;
      this.waypoints[index] = targetMarker.getLatLng();

      if (this.waypoints.length >= MapRoutingService.MIN_WAYPOINTS_FOR_ROUTE) {
        this.recalculateDirectRoute();
      }
    });

    this.waypointMarkers.push(marker);

    if (this.waypoints.length >= MapRoutingService.MIN_WAYPOINTS_FOR_ROUTE) {
      this.recalculateDirectRoute();
    }
  }

  private recalculateDirectRoute(): void {
    if (!this.map || this.waypoints.length < MapRoutingService.MIN_WAYPOINTS_FOR_ROUTE) {
      return;
    }

    const coords = this.waypoints.map((w) => [w.lng, w.lat]);
    const avoidPolygonsGeoJson = this.getAvoidanceZones();
    this.logger.debug('Fetching direct route from ORS with waypoints:', coords);

    this.orsService.getRouteGeoJson(coords, avoidPolygonsGeoJson).subscribe({
      next: (data) => this.handleDirectRouteSuccess(data),
      error: (err) => this.handleDirectRouteError(err),
    });
  }

  private handleDirectRouteSuccess(data: OrsGeoJsonResponse): void {
    this.ngZone.run(() => {
      if (!this.map || data.error || !data.features || data.features.length === 0) {
        this.logger.error('No route data found from ORS');
        this.notificationService.error('ERRORS.ROUTE_NOT_FOUND');
        return;
      }

      const feature = data.features[0];
      const geometry = feature.geometry.coordinates;
      const summary = feature.properties?.summary;

      const routeCoordinates = geometry.map((coord: number[]) =>
        L.latLng(coord[1], coord[0], coord[2]),
      );

      this.updateRouteData(routeCoordinates, summary?.distance ?? 0, 'POINT_TO_POINT');

      if (this.directRouteLayer) {
        this.map.removeLayer(this.directRouteLayer);
        this.directRouteLayer = undefined;
      }

      this.directRouteLayer = L.polyline(
        routeCoordinates,
        MAP_CONSTANTS.STYLES.DRAFT_ROUTE,
      ).addTo(this.map);
    });
  }

  private handleDirectRouteError(err: unknown): void {
    this.logger.error('Route error from ORS', err);
    this.ngZone.run(() => {
      this.notificationService.error('ERRORS.ROUTE_NOT_FOUND');
    });
  }

  public generateRoundTrip(startPoint: L.LatLng, distanceMeters: number, map: L.Map): void {
    this.logger.debug(
      'Generating round trip from ORS. Start:',
      startPoint,
      'Distance:',
      distanceMeters,
    );
    const coords = [[startPoint.lng, startPoint.lat]];
    const avoidPolygonsGeoJson = this.getAvoidanceZones() || undefined;

    this.orsService.getRoundTripGeoJson(coords[0], distanceMeters, avoidPolygonsGeoJson).subscribe({
      next: (data) => {
        if (data.error || !data.features || data.features.length === 0) {
          this.logger.error('No route data found');
          return;
        }
        const feature = data.features[0];
        const routeCoordinates = feature.geometry.coordinates.map((coord: number[]) =>
          L.latLng(coord[1], coord[0], coord[2]),
        );
        const summary = feature.properties?.summary;
        this.updateRouteData(routeCoordinates, summary?.distance ?? 0, 'LOOP', distanceMeters);

        if (this.roundTripLayer) {
          map.removeLayer(this.roundTripLayer);
          this.roundTripLayer = undefined;
        }
        if (this.roundTripMarker) {
          map.removeLayer(this.roundTripMarker);
          this.roundTripMarker = undefined;
        }

        this.roundTripLayer = L.polyline(routeCoordinates, MAP_CONSTANTS.STYLES.LOOP_ROUTE).addTo(
          map,
        );
        this.roundTripMarker = L.marker(startPoint, {
          icon: MAP_CONSTANTS.DEFAULT_ICON,
        }).addTo(map);
      },
      error: (err) => {
        this.logger.error('Round trip error', err);
        this.notificationService.error('ERRORS.ROUND_TRIP_NOT_FOUND');
      },
    });
  }

  private getAvoidanceZones(): any {
    const allZones = this.avoidanceZoneStateService.zones();
    const activeZones = allZones.filter((zone) => zone.isActive);

    if (activeZones.length === 0) {
      return undefined;
    }

    return {
      type: 'MultiPolygon',
      coordinates: activeZones.map((zone) => (wellknown.parse(zone.zoneGeom) as any).coordinates),
    };
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
      routeType: routeType,
      geometryWkt: stats.geometryWkt,
      startPoint: stats.startPoint,
      elevationGain: stats.elevationGain,
      elevationLoss: stats.elevationLoss,
      distanceMeters: totalDistance,
      targetDistance: targetDistance,
    });
  }

  public clearRoute(): void {
    if (this.map) {
      if (this.directRouteLayer) {
        this.map.removeLayer(this.directRouteLayer);
        this.directRouteLayer = undefined;
      }
      this.waypointMarkers.forEach((marker) => this.map?.removeLayer(marker));
      this.waypointMarkers = [];
      this.waypoints = [];

      if (this.roundTripLayer) {
        this.map.removeLayer(this.roundTripLayer);
        this.roundTripLayer = undefined;
      }
      if (this.roundTripMarker) {
        this.map.removeLayer(this.roundTripMarker);
        this.roundTripMarker = undefined;
      }
    } else {
      this.waypointMarkers = [];
      this.waypoints = [];
      this.directRouteLayer = undefined;
      this.roundTripLayer = undefined;
      this.roundTripMarker = undefined;
    }

    this.currentRouteDraft.set(null);
  }

  public destroyRouting(): void {
    this.clearRoute();
    this.map = undefined;
  }
}
