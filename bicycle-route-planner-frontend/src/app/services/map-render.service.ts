import { Injectable, inject, effect } from '@angular/core';
import * as L from 'leaflet';
import { parse } from 'wellknown';
import { RouteStateService } from './route-state.service';
import { RouteDetailDto } from '../models/route.model';

@Injectable({
  providedIn: 'root',
})
export class MapRenderService {
  private readonly routeStateService = inject(RouteStateService);

  private map?: L.Map;
  private currentGeoJsonLayer?: L.GeoJSON;

  constructor() {
    effect(() => {
      const routeState = this.routeStateService.selectedRoute();
      if (routeState) {
        this.renderSavedRoute(routeState);
      } else {
        this.clearRenderedRoute();
      }
    });
  }

  public initMap(elementId: string): void {
    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }

    this.map = L.map(elementId, { zoomControl: false }).setView([52.069, 19.47], 6);
    L.tileLayer('https://{s}.tile.osm.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '@ OpenStreetMap',
    }).addTo(this.map);

    const currentRoute = this.routeStateService.selectedRoute();
    if (currentRoute) {
      this.renderSavedRoute(currentRoute);
    }
  }

  public getMap(): L.Map | undefined {
    return this.map;
  }

  private renderSavedRoute(routeState: RouteDetailDto) {
    if (!this.map) return;

    this.clearRenderedRoute();

    const geoJsonGeometry = parse(routeState.routeGeom);

    if (geoJsonGeometry) {
      this.currentGeoJsonLayer = L.geoJson(geoJsonGeometry as any, {
        style: { color: '#e74c3c', weight: 6, opacity: 0.9 },
      }).addTo(this.map);

      if (geoJsonGeometry.type === 'LineString') {
        const coords = geoJsonGeometry.coordinates as number[][];
        if (coords.length > 0) {
          const startCoords = coords[0];
          const endCoords = coords[coords.length - 1];

          if (!startCoords || !endCoords) return;

          if (routeState.routeType === 'LOOP') {
            const loopIcon = L.divIcon({
              className: 'route-marker-loop',
              html: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/></svg>`,
              iconSize: [28, 28],
              iconAnchor: [14, 14]
            });
            const loopMarker = L.marker([startCoords[1], startCoords[0]], { icon: loopIcon, title: 'Start/Koniec (Pętla)' });
            this.currentGeoJsonLayer.addLayer(loopMarker);
          } else {
            const startIcon = L.divIcon({
              className: 'route-marker-circle route-marker-start',
              html: 'A',
              iconSize: [24, 24],
              iconAnchor: [12, 12]
            });

            const endIcon = L.divIcon({
              className: 'route-marker-circle route-marker-end',
              html: 'B',
              iconSize: [24, 24],
              iconAnchor: [12, 12]
            });

            const startMarker = L.marker([startCoords[1], startCoords[0]], { icon: startIcon, title: 'Start' });
            const endMarker = L.marker([endCoords[1], endCoords[0]], { icon: endIcon, title: 'Koniec' });

            this.currentGeoJsonLayer.addLayer(startMarker);
            this.currentGeoJsonLayer.addLayer(endMarker);
          }
        }
      }

      this.map.fitBounds(this.currentGeoJsonLayer.getBounds(), { padding: [50, 50] });
    }
  }

  public clearRenderedRoute(): void {
    if(this.currentGeoJsonLayer && this.map) {
      this.map.removeLayer(this.currentGeoJsonLayer);
      this.currentGeoJsonLayer = undefined;
    }
  }
}
