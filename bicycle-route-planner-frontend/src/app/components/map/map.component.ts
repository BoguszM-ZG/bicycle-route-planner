import { AfterViewInit, Component } from '@angular/core';
import * as L from 'leaflet';
import { RouteService } from '../../services/route.service';
import { parse } from 'wellknown';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [],
  templateUrl: './map.component.html',
  styleUrl: './map.component.css',
})
export class MapComponent implements AfterViewInit {
  private map!: L.Map;

  constructor(private routeService: RouteService) {}

  ngAfterViewInit(): void {
    this.initMap();
    this.loadRoutes();
  }

  private initMap(): void {
    this.map = L.map('map').setView([52.069, 19.470], 6);

    L.tileLayer('https://{s}.tile.osm.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '@ OpenStreetMap',
    }).addTo(this.map)
  }

  private loadRoutes(): void {
    this.routeService.getRoutes().subscribe({
      next:(routesData) => {
        routesData.forEach(route => {
          const geoJsonGeometry = parse(route.routeGeom);

          if(geoJsonGeometry) {
            L.geoJson(geoJsonGeometry as any, {
              style: {color: 'blue', weight: 4}
              }
            ).addTo(this.map);
          }
        });
      },
      error: (err) => console.error('Error loading routes:', err)
    });
  }
}
