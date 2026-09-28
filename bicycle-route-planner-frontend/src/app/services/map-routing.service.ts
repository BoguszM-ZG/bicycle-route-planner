import { Injectable, NgZone, inject, signal, computed, effect } from '@angular/core';
import  * as L from 'leaflet';
import 'leaflet-routing-machine';
import { OrsService} from './ors.service';
import { RouteCalculationService} from './route-calculation.service';
import { RouteDraft, OrsGeoJsonResponse } from '../models/route.model';
import * as wellknown from 'wellknown';
import { AvoidanceZoneStateService } from './avoidance-zone-state.service';
import { MAP_CONSTANTS } from '../constants/map.constants';

import { LoggerService } from './logger.service';
import { AuthService } from './auth.service';
import { NotificationService } from './notification.service';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
  providedIn: 'root',
})
export class MapRoutingService {
  private readonly ngZone = inject(NgZone);
  private readonly orsService = inject(OrsService);
  private readonly routeCalculationService = inject(RouteCalculationService);
  private readonly avoidanceZoneStateService = inject(AvoidanceZoneStateService);
  private readonly logger = inject(LoggerService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly translateService = inject(TranslateService);

  private routingControl?: L.Routing.Control;
  private roundTripLayer?: L.Polyline;
  private roundTripMarker?: L.Marker;
  private map?: L.Map;

  public currentRouteDraft = signal<RouteDraft | null>(null);
  public routingMode = signal<'direct' | 'roundTrip' | 'avoidanceZone'>('direct');
  public roundTripDistanceMeters = signal<number>(20000);
  public roundTripDistanceKm = computed(() => this.roundTripDistanceMeters() / 1000);

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
    this.map = map;

    this.setupRoutingControl(map);
    this.setupMapEventListeners(map);
  }

  private setupRoutingControl(map: L.Map): void {
    const customOrsRouter: L.Routing.IRouter = {
      route: (waypoints: any[], callback: any, context: any, _options: any) => {
        this.fetchRoute(waypoints, callback, context);
      },
    };

    const routePlan = L.Routing.plan([], {
      createMarker: (_i, waypoint, _n) => L.marker(waypoint.latLng, { draggable: true }),
    });

    this.routingControl = L.Routing.control({
      router: customOrsRouter as any,
      plan: routePlan,
      lineOptions: {
        styles: [MAP_CONSTANTS.STYLES.DRAFT_ROUTE],
        extendToWaypoints: true,
        missingRouteTolerance: 0,
      },
      waypointMode: 'snap',
      addWaypoints: true,
      routeWhileDragging: true,
      show: false,
    }).addTo(map);

    this.routingControl.on('routingerror', (e: any) => {
      this.logger.error('Route error', e);
      this.ngZone.run(() => {
        this.notificationService.error(this.translateService.instant('ERRORS.ROUTE_NOT_FOUND'));
      });
    });
  }

  private fetchRoute(waypoints: any[], callback: any, context: any): void {
    const coords = waypoints.map((w: any) => [w.latLng.lng, w.latLng.lat]);
    const avoidPolygonsGeoJson = this.getAvoidanceZones();
    this.logger.debug('Fetching route from ORS with waypoints:', coords);

    this.orsService.getRouteGeoJson(coords, avoidPolygonsGeoJson).subscribe({
      next: (data) => this.handleRouteSuccess(data, waypoints, callback, context),
      error: (err) => this.handleRouteError(err, callback, context),
    });
  }

  private setupMapEventListeners(map: L.Map): void {
    let isDrawingMode = false;
    map.on('draw:drawstart', () => (isDrawingMode = true));
    map.on('draw:drawstop', () => (isDrawingMode = false));

    map.on('click', (e: any) => this.handleMapClick(e, map, isDrawingMode));
  }

  private handleMapClick(e: any, map: L.Map, isDrawingMode: boolean): void {
    if (isDrawingMode || this.routingMode() === 'avoidanceZone') {
      return;
    }

    if (this.routingMode() === 'roundTrip') {
      this.generateRoundTrip(e.latlng, this.roundTripDistanceMeters(), map);
      return;
    }

    if (this.routingControl) {
      const waypoints = this.routingControl.getWaypoints().filter((w: any) => w.latLng);
      this.logger.debug('Adding new waypoint at:', e.latlng);
      waypoints.push(L.Routing.waypoint(e.latlng));
      this.routingControl.setWaypoints(waypoints);
    }
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
          this.logger.error('Brak danych o trasie');
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

        this.roundTripMarker = L.marker(startPoint).addTo(map);
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

  private handleRouteSuccess(
    data: OrsGeoJsonResponse,
    waypoints: any[],
    callback: any,
    context: any,
  ): void {
    this.ngZone.run(() => {
      if (data.error || !data.features || data.features.length === 0) {
        callback.call(context, { status: -1, message: data.error?.message || 'Brak wyników.' });
        return;
      }
      const feature = data.features[0];
      const geometry = feature.geometry.coordinates;
      const summary = feature.properties?.summary;

      const routeCoordinates = geometry.map((coord: number[]) =>
        L.latLng(coord[1], coord[0], coord[2]),
      );

      this.updateRouteData(routeCoordinates, summary?.distance ?? 0, 'POINT_TO_POINT');

      callback.call(context, null, [
        {
          name: 'Trasa 3D',
          summary: { totalDistance: summary?.distance ?? 0, totalTime: summary?.duration ?? 0 },
          coordinates: routeCoordinates,
          waypoints: waypoints,
          inputWaypoints: waypoints,
          instructions: [],
        },
      ]);
    });
  }

  private handleRouteError(err: any, callback: any, context: any): void {
    this.ngZone.run(() => callback.call(context, err));
  }

  private updateRouteData(
    coordinates: L.LatLng[],
    totalDistance: number,
    routeType: string,
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
    this.routingControl?.setWaypoints([]);
    if (this.map) {
      if (this.roundTripLayer) {
        this.map.removeLayer(this.roundTripLayer);
        this.roundTripLayer = undefined;
      }
      if (this.roundTripMarker) {
        this.map.removeLayer(this.roundTripMarker);
        this.roundTripMarker = undefined;
      }
    }

    this.currentRouteDraft.set(null);
  }
}
