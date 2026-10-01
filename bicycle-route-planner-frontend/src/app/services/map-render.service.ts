import { Injectable, inject, effect, NgZone } from '@angular/core';
import * as L from 'leaflet';
import { parse } from 'wellknown';
import { RouteStateService } from './route-state.service';
import { RouteDetailDto } from '../models/route.model';
import { MAP_CONSTANTS } from '../constants/map.constants';
import { LoggerService } from './logger.service';
import { NotificationService } from './notification.service';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
  providedIn: 'root',
})
export class MapRenderService {
  private readonly routeStateService = inject(RouteStateService);
  private readonly ngZone = inject(NgZone);
  private readonly logger = inject(LoggerService);
  private readonly notificationService = inject(NotificationService);
  private readonly translate = inject(TranslateService);

  private map?: L.Map;
  private currentGeoJsonLayer?: L.GeoJSON;
  private searchMarker?: L.Marker;
  private userLocationMarker?: L.Marker;

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
      this.destroyMap();
    }

    this.map = L.map(elementId, { zoomControl: false }).setView(
      MAP_CONSTANTS.DEFAULT_CENTER,
      MAP_CONSTANTS.DEFAULT_ZOOM,
    );
    L.tileLayer(MAP_CONSTANTS.TILE_LAYER.URL, {
      maxZoom: MAP_CONSTANTS.TILE_LAYER.MAX_ZOOM,
      attribution: MAP_CONSTANTS.TILE_LAYER.ATTRIBUTION,
    }).addTo(this.map);

    const currentRoute = this.routeStateService.selectedRoute();
    if (currentRoute) {
      this.renderSavedRoute(currentRoute);
    }
  }

  public destroyMap(): void {
    if (this.map) {
      this.clearRenderedRoute();
      this.map.remove();
      this.map = undefined;
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
        style: MAP_CONSTANTS.STYLES.SAVED_ROUTE,
      }).addTo(this.map);

      this.addRouteMarkers(geoJsonGeometry, routeState.routeType);

      this.map.fitBounds(this.currentGeoJsonLayer.getBounds(), { padding: [50, 50] });
    }
  }

  private addRouteMarkers(geoJsonGeometry: any, routeType: string): void {
    if (geoJsonGeometry.type !== 'LineString') return;

    const coords = geoJsonGeometry.coordinates as number[][];
    if (coords.length === 0) return;

    const startCoords = coords[0];
    const endCoords = coords[coords.length - 1];
    if (!startCoords || !endCoords) return;

    if (routeType === 'LOOP') {
      const loopMarker = L.marker([startCoords[1], startCoords[0]], {
        icon: MAP_CONSTANTS.MARKERS.LOOP_ICON,
        title: 'Start/End (Loop)',
      });
      this.currentGeoJsonLayer?.addLayer(loopMarker);
    } else {
      const startMarker = L.marker([startCoords[1], startCoords[0]], {
        icon: MAP_CONSTANTS.MARKERS.START_ICON,
        title: 'Start',
      });
      const endMarker = L.marker([endCoords[1], endCoords[0]], {
        icon: MAP_CONSTANTS.MARKERS.END_ICON,
        title: 'End',
      });

      this.currentGeoJsonLayer?.addLayer(startMarker);
      this.currentGeoJsonLayer?.addLayer(endMarker);
    }
  }

  public clearRenderedRoute(): void {
    if (this.currentGeoJsonLayer && this.map) {
      this.map.removeLayer(this.currentGeoJsonLayer);
      this.currentGeoJsonLayer = undefined;
    }
  }

  public getCenter(): { lat: number; lng: number } | null {
    if (!this.map) return null;
    const center = this.map.getCenter();
    return { lat: center.lat, lng: center.lng };
  }

  public focusOnSearchLocation(
    lat: number,
    lng: number,
    zoom: number,
    duration: number,
    onPinClick: () => void,
  ): void {
    if (!this.map) return;

    this.map.flyTo([lat, lng], zoom, { animate: true, duration });

    this.clearSearchMarker();

    this.searchMarker = L.marker([lat, lng], { icon: MAP_CONSTANTS.SEARCH_PIN }).addTo(this.map);

    this.searchMarker.once('click', () => {
      this.clearSearchMarker();
      onPinClick();
    });
  }

  public clearSearchMarker(): void {
    this.searchMarker?.remove();
    this.searchMarker = undefined;
  }

  public locateUser(): void {
    const map = this.map;
    if (!map) return;

    map.locate({ setView: true, maxZoom: 15 });

    map.once('locationfound', (e) => {
      this.ngZone.run(() => {
        if (this.userLocationMarker) {
          this.userLocationMarker.setLatLng(e.latlng);
        } else {
          this.userLocationMarker = L.marker(e.latlng)
            .addTo(map)
            .bindPopup(this.translate.instant('MAP.LOCATION_FOUND'));

          this.userLocationMarker.on('click', () => {
            if (this.userLocationMarker) {
              this.userLocationMarker.remove();
              this.userLocationMarker = undefined;
            }
          });
        }
        this.userLocationMarker?.openPopup();
      });
    });

    map.once('locationerror', (e) => {
      this.ngZone.run(() => {
        this.logger.warn('GPS location access denied or failed:', e);
        this.notificationService.error('MAP.LOCATION_ERROR');
      });
    });
  }
}
