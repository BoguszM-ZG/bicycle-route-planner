import { Injectable, inject} from '@angular/core';
import * as L from 'leaflet';
import { parse } from 'wellknown';
import { RouteStateService} from './route-state.service';
import { RouteDetailDto} from '../models/route.model';

@Injectable({
  providedIn: 'root',
})

export class MapRenderService {
  private readonly routeStateService = inject(RouteStateService);

  private map?: L.Map;
  private currentGeoJsonLayer?: L.GeoJSON;

  public initMap(elementId: string): void {
    if (this.map) return;

    this.map = L.map(elementId).setView([52.069, 19.47], 6);
    L.tileLayer('https://{s}.tile.osm.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '@ OpenStreetMap',
    }).addTo(this.map);

    this.listenToSelectedRoute();
  }

  public getMap(): L.Map | undefined {
    return this.map;
  }

  private listenToSelectedRoute(): void {
    this.routeStateService.selectedRoute$.subscribe(routeState => {
      if (routeState) {
        this.renderSavedRoute(routeState);
      } else {
        this.clearRenderedRoute();
      }
    })

  }

  private renderSavedRoute(routeState: RouteDetailDto) {
    if (!this.map) return;

    this.clearRenderedRoute();

    const geoJsonGeometry = parse(routeState.routeGeom);

    if (geoJsonGeometry) {
      this.currentGeoJsonLayer = L.geoJson(geoJsonGeometry as any, {
        style: { color: '#e74c3c', weight: 6, opacity: 0.9 },
      }).addTo(this.map);

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
