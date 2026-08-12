import { AfterViewInit, Component, NgZone } from '@angular/core';
import * as L from 'leaflet';
import 'leaflet-routing-machine';
import { RouteService } from '../../services/route.service';
import { AuthService } from '../../services/auth.service';
import { parse } from 'wellknown';
import { OrsService} from '../../services/ors.service';
import {RouteCalculationService} from '../../services/route-calculation.service';


const defaultIcon = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});
L.Marker.prototype.options.icon = defaultIcon;

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [],
  templateUrl: './map.component.html',
  styleUrl: './map.component.css',
})
export class MapComponent implements AfterViewInit {
  private map!: L.Map;
  private routingControl?: L.Routing.Control;

  protected currentRouteData: {
    geometryWkt?: string;
    distanceMeters?: number;
    elevationGain?: number;
    elevationLoss?: number;
    name?: string;
    routeType?: string;
    startPoint?: string;
  } = {
    name: '',
    routeType: 'cycling',
  };

  constructor(
    private readonly routeService: RouteService,
    private readonly authService: AuthService,
    private readonly ngZone: NgZone,
    private readonly orsService: OrsService,
    private readonly routeCalculationService: RouteCalculationService,
  ) {}

  ngAfterViewInit(): void {
    this.initMap();
    this.loadExistingRoutes();
    this.initRoutingControl();
  }

  private initMap(): void {
    this.map = L.map('map').setView([52.069, 19.47], 6);

    L.tileLayer('https://{s}.tile.osm.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '@ OpenStreetMap',
    }).addTo(this.map);
  }

  private loadExistingRoutes(): void {
    this.routeService.getRoutes().subscribe({
      next: (routesData) => {
        routesData.forEach((route) => {
          const geoJsonGeometry = parse(route.routeGeom);

          if (geoJsonGeometry) {
            L.geoJson(geoJsonGeometry as any, {
              style: { color: 'blue', weight: 4 },
            }).addTo(this.map);
          }
        });
      },
      error: (err) => console.error('Error loading routes:', err),
    });
  }

  private initRoutingControl(): void {
    const customOrsRouter: L.Routing.IRouter = {
      route: (waypoints: any[], callback: any, context: any, _options: any) => {
        const coords = waypoints.map((w: any) => [w.latLng.lng, w.latLng.lat]);
        this.orsService.getRouteGeoJson(coords).subscribe({
          next: (data) => this.handleRouteSuccess(data, waypoints, callback, context),
          error: (err) => this.handleRouteError(err, callback, context),
        });
      },
    };
    const routePlan = L.Routing.plan([], {
      createMarker: (_i, waypoint, _n) => {
        return L.marker(waypoint.latLng, { draggable: true });
      },
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
    }).addTo(this.map);
    this.routingControl.on('routingerror', (e: any) => {
      console.error('Błąd wyznaczania trasy:', e);
    });
    this.map.on('click', (e: any) => {
      if (this.routingControl) {
        const waypoints = this.routingControl.getWaypoints().filter((w: any) => w.latLng);
        waypoints.push(L.Routing.waypoint(e.latlng));
        this.routingControl.setWaypoints(waypoints);
      }
    });
  }

  private handleRouteSuccess(data: any, waypoints: any[], callback: any, context: any): void {
    this.ngZone.run(() => {
      if (data.error) {
        callback.call(context, { status: -1, message: data.error.message });
        return;
      }

      if (!data.features || data.features.length === 0) {
        callback.call(context, { status: -1, message: 'Brak wyników trasy.' });
        return;
      }

      const feature = data.features[0];
      const geometry = feature.geometry.coordinates;
      const summary = feature.properties?.summary;

      const routeCoordinates = geometry.map((coord: number[]) =>
        L.latLng(coord[1], coord[0], coord[2]),
      );

      this.updateRouteData(routeCoordinates, summary?.distance ?? 0);

      const result = [
        {
          name: 'Trasa 3D',
          summary: {
            totalDistance: summary?.distance ?? 0,
            totalTime: summary?.duration ?? 0,
          },
          coordinates: routeCoordinates,
          waypoints: waypoints,
          inputWaypoints: waypoints,
          instructions: [],
        },
      ];

      callback.call(context, null, result);
    });
  }

  private handleRouteError(err: any, callback: any, context: any): void {
    this.ngZone.run(() => {
      callback.call(context, err);
    });
  }

  private updateRouteData(coordinates: L.LatLng[], totalDistance: number): void {
    const stats = this.routeCalculationService.calculateRouteStats(coordinates);

    this.currentRouteData.geometryWkt = stats.geometryWkt;
    this.currentRouteData.startPoint = stats.startPoint;
    this.currentRouteData.elevationGain = stats.elevationGain;
    this.currentRouteData.elevationLoss = stats.elevationLoss;

    this.currentRouteData.distanceMeters = totalDistance;

    console.log(
      `Gain: ${stats.elevationGain.toFixed(2)} m, Loss: ${stats.elevationLoss.toFixed(2)} m`,
    );
    console.log('Ready WKT 3D:', this.currentRouteData.geometryWkt);
  }

  public clearRoute(): void {
    this.routingControl?.setWaypoints([]);
    this.currentRouteData = {
      name: '',
      routeType: 'cycling',
    };
  }

  public saveRoute(): void {
    if (!this.currentRouteData.geometryWkt) {
      alert('Najpierw wyznacz trasę, żeby ją zapisać.');
      return;
    }

    if (!this.authService.isAuthenticated()) {
      alert('Musisz być zalogowany, aby zapisać trasę.');
      return;
    }

    const currentUser = this.authService.getCurrentUser();
    const userEmail = currentUser ? currentUser.email : '';

    this.routeService
      .addRoute({
        id: 0,
        userEmail: userEmail,
        name: this.currentRouteData.name || 'Nowa Trasa',
        routeType: this.currentRouteData.routeType || 'cycling',
        targetDistance: this.currentRouteData.distanceMeters!,
        distanceMeters: this.currentRouteData.distanceMeters!,
        elevationGain: this.currentRouteData.elevationGain || 0,
        elevationLoss: this.currentRouteData.elevationLoss || 0,
        startPoint: this.currentRouteData.startPoint!,
        routeGeom: this.currentRouteData.geometryWkt,
        createdAt: new Date().toISOString(),
      })
      .subscribe({
        next: (response) => {
          console.log('Trasa została zapisana:', response);
          alert('Trasa została pomyślnie zapisana na Twoim koncie!');
        },
        error: (err) => {
          console.error('Błąd podczas zapisywania trasy:', err);
          if (err.status === 401 || err.status === 403) {
            alert('Twoja sesja wygasła lub token jest nieważny. Zaloguj się ponownie.');
            localStorage.clear();
          } else {
            alert('Wystąpił błąd podczas zapisywania trasy.');
          }
        },
      });
  }
}
