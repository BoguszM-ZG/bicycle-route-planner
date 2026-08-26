import { Injectable, NgZone, inject, signal } from '@angular/core';
import  * as L from 'leaflet';
import 'leaflet-routing-machine';
import { OrsService} from './ors.service';
import { RouteCalculationService} from './route-calculation.service';
import { RouteDraft, OrsGeoJsonResponse } from '../models/route.model';
import * as wellknown from 'wellknown';
import { AvoidanceZoneStateService } from './avoidance-zone-state.service';

@Injectable({
  providedIn: 'root',
})
export class MapRoutingService {
  private readonly ngZone = inject(NgZone);
  private readonly orsService = inject(OrsService);
  private readonly routeCalculationService = inject(RouteCalculationService);
  private readonly avoidanceZoneStateService = inject(AvoidanceZoneStateService);

  private routingControl?: L.Routing.Control;

  public currentRouteDraft = signal<RouteDraft | null>(null);

  public initRouting(map: L.Map): void {
    const customOrsRouter: L.Routing.IRouter = {

      route: (waypoints: any[], callback: any, context: any, _options: any) => {

        const coords = waypoints.map((w: any) => [w.latLng.lng, w.latLng.lat]);
        const allZones = this.avoidanceZoneStateService.zones();

        const activeZones = allZones.filter(zone => zone.isActive);

        const avoidPolygonsGeoJson = activeZones.length > 0
          ? {
              type: 'MultiPolygon',
              coordinates: activeZones.map(zone => (wellknown.parse(zone.zoneGeom) as any).coordinates)
            }
          : undefined;





        this.orsService.getRouteGeoJson(coords, avoidPolygonsGeoJson).subscribe({
          next: (data) => this.handleRouteSuccess(data, waypoints, callback, context),
          error: (err) => this.handleRouteError(err, callback, context),
        });
      },
    };

    const routePlan = L.Routing.plan([], {
      createMarker: (_i, waypoint, _n) => L.marker(waypoint.latLng, { draggable: true }),
    });

    this.routingControl = L.Routing.control({
      router: customOrsRouter as any,
      plan: routePlan,
      lineOptions: {
        styles: [{ color: '#e74c3c', weight: 5, opacity: 0.8 }],
        extendToWaypoints: true,
        missingRouteTolerance: 0,
      },
      waypointMode: 'snap',
      addWaypoints: true,
      routeWhileDragging: true,
      show: false,
    }).addTo(map);

    this.routingControl.on('routingerror', (e: any) => console.error('Błąd wyznaczania trasy', e));

    let isDrawingMode = false;
    map.on('draw:drawstart', () => isDrawingMode = true);
    map.on('draw:drawstop', () => isDrawingMode = false);

    map.on('click', (e: any) => {
      if (isDrawingMode) return;

      if (this.routingControl) {
        const waypoints = this.routingControl.getWaypoints().filter((w: any) => w.latLng);
        waypoints.push(L.Routing.waypoint(e.latlng));
        this.routingControl.setWaypoints(waypoints);
      }
    });
  }

  private handleRouteSuccess(data: OrsGeoJsonResponse, waypoints: any[], callback: any, context: any): void {
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

      this.updateRouteData(routeCoordinates, summary?.distance ?? 0);

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

  private updateRouteData(coordinates: L.LatLng[], totalDistance: number): void {
    const stats = this.routeCalculationService.calculateRouteStats(coordinates);

    this.currentRouteDraft.set({
      name: '',
      routeType: 'cycling',
      geometryWkt: stats.geometryWkt,
      startPoint: stats.startPoint,
      elevationGain: stats.elevationGain,
      elevationLoss: stats.elevationLoss,
      distanceMeters: totalDistance,
    });
  }

  public clearRoute(): void {
    this.routingControl?.setWaypoints([]);
    this.currentRouteDraft.set(null);
  }
}
