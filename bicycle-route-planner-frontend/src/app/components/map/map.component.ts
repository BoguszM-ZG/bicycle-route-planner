import { AfterViewInit, Component } from '@angular/core';
import * as L from 'leaflet';
import 'leaflet-routing-machine';
import { RouteService } from '../../services/route.service';
import { parse } from 'wellknown';
import { environment} from '../../../environments/environment.development';


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

  private currentRouteData: {
    geometryWkt?: string;
    distanceMeters?: number;
  } = {};

  constructor(private routeService: RouteService) {}

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
    const customOrsRouter = {
      route: (waypoints: any[], callback: any, context: any, options: any) => {
        const coords = waypoints.map((w: any) => [w.latLng.lng, w.latLng.lat]);

        fetch('https://api.openrouteservice.org/v2/directions/cycling-road/geojson', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': environment.orsApiKey
          },
          body: JSON.stringify({
            coordinates: coords,
            elevation: true,
            instructions: false
          })
        })
          .then(res => res.json())
          .then(data => {
            if (data.error) {
              callback.call(context, { status: -1, message: data.error.message });
              return;
            }

            const feature = data.features[0];
            const geometry = feature.geometry.coordinates;
            const summary = feature.properties.summary;

            const routeCoordinates = geometry.map((coord: number[]) =>
              L.latLng(coord[1], coord[0], coord[2])
            );

            const result = [{
              name: "Trasa 3D",
              summary: {
                totalDistance: summary.distance,
                totalTime: summary.duration
              },
              coordinates: routeCoordinates,
              waypoints: waypoints,
              inputWaypoints: waypoints,
              instructions: []
            }];

            callback.call(context, null, result);
          })
          .catch(err => {
            callback.call(context, err);
          });

        return { abort: () => {} };
      }
    };

    const routePlan = L.Routing.plan([], {
      createMarker: (i, waypoint, n) => {
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


    this.routingControl.on('routesfound', (e: any) => {
      const routes = e.routes;
      const summary = routes[0].summary;
      const coordinates = routes[0].coordinates;

      console.log(`Znaleziono trasę 3D! Dystans: ${(summary.totalDistance / 1000).toFixed(2)} km`);


      const wktPoints = coordinates
        .map((pt: any) => `${pt.lng} ${pt.lat} ${pt.alt || 0}`)
        .join(', ');


      this.currentRouteData.geometryWkt = `LINESTRING Z(${wktPoints})`;
      this.currentRouteData.distanceMeters = summary.totalDistance;

      console.log('Gotowy WKT 3D:', this.currentRouteData.geometryWkt);
    });

    this.map.on('click', (e: any) => {
      if (this.routingControl) {
        const waypoints = this.routingControl.getWaypoints().filter((w: any) => w.latLng);
        waypoints.push(L.Routing.waypoint(e.latlng));
        this.routingControl.setWaypoints(waypoints);
      }
    });
  }
}
