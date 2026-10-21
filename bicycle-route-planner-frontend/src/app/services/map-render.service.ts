import { Injectable, inject, effect, untracked, NgZone, signal } from '@angular/core';
import * as L from 'leaflet';
import { parse } from 'wellknown';
import type { Position } from 'geojson';
import { RouteStateService } from './route-state.service';
import { MapRouteLayerService } from './map-route-layer.service';
import { RouteDetailDto, RouteGeometry, RouteType } from '../models/route.model';
import { MAP_CONSTANTS } from '../constants/map.constants';
import { LoggerService } from './logger.service';
import { NotificationService } from './notification.service';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class MapRenderService {
  private readonly routeStateService = inject(RouteStateService);
  private readonly mapRouteLayerService = inject(MapRouteLayerService);
  private readonly authService = inject(AuthService);
  private readonly ngZone = inject(NgZone);
  private readonly logger = inject(LoggerService);
  private readonly notificationService = inject(NotificationService);
  private readonly translate = inject(TranslateService);

  public readonly isLocating = signal(false);

  private map?: L.Map;
  private currentGeoJsonLayer?: L.GeoJSON<unknown, RouteGeometry>;
  private searchMarker?: L.Marker;
  private userLocationMarker?: L.Marker;

  constructor() {
    effect(() => {
      const user = this.authService.currentUser();
      if (!user) {
        this.clearUserLocationMarker();
        this.clearSearchMarker();
      }
    });

    effect(() => {
      const routeState = this.routeStateService.selectedRoute();
      if (routeState) {
        untracked(() => {
          this.mapRouteLayerService.clearRoute();
        });
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
      this.mapRouteLayerService.clearRoute();
      this.renderSavedRoute(currentRoute);
    }
  }

  public destroyMap(): void {
    this.isLocating.set(false);
    if (this.map) {
      this.map.stopLocate();
      this.map.off('locationfound').off('locationerror');
      this.clearRenderedRoute();
      this.clearSearchMarker();
      this.clearUserLocationMarker();
      this.map.remove();
      this.map = undefined;
    }
  }

  public getMap(): L.Map | undefined {
    return this.map;
  }

  private renderSavedRoute(routeState: RouteDetailDto): void {
    if (!this.map) return;

    this.clearRenderedRoute();

    const geoJsonGeometry = parse(routeState.routeGeom) as RouteGeometry | null;

    if (geoJsonGeometry) {
      this.currentGeoJsonLayer = L.geoJson<unknown, RouteGeometry>(geoJsonGeometry, {
        style: MAP_CONSTANTS.STYLES.SAVED_ROUTE,
      }).addTo(this.map);

      this.addRouteMarkers(geoJsonGeometry, routeState.routeType);

      const bounds = this.currentGeoJsonLayer.getBounds();
      if (bounds.isValid()) {
        this.map.fitBounds(bounds, { padding: [50, 50] });
      }
    }
  }

  private addRouteMarkers(geoJsonGeometry: RouteGeometry, routeType: RouteType): void {
    let startCoords: Position | undefined;
    let endCoords: Position | undefined;

    if (geoJsonGeometry.type === 'LineString') {
      const coords = geoJsonGeometry.coordinates;
      if (coords.length > 0) {
        startCoords = coords[0];
        endCoords = coords[coords.length - 1];
      }
    } else if (geoJsonGeometry.type === 'MultiLineString') {
      const validLines = geoJsonGeometry.coordinates.filter((line) => line.length > 0);
      if (validLines.length > 0) {
        startCoords = validLines[0][0];
        const lastLine = validLines[validLines.length - 1];
        endCoords = lastLine[lastLine.length - 1];
      }
    }

    if (!startCoords || !endCoords || startCoords.length < 2 || endCoords.length < 2) return;

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
    if (this.currentGeoJsonLayer) {
      if (this.map) {
        this.map.removeLayer(this.currentGeoJsonLayer);
      }
      this.currentGeoJsonLayer.clearLayers();
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

  public clearUserLocationMarker(): void {
    this.userLocationMarker?.remove();
    this.userLocationMarker = undefined;
  }

  public locateUser(): void {
    if (this.isLocating()) {
      return;
    }
    this.logger.debug('[Geolocation] Requesting user GPS location...');
    const map = this.map;
    if (!map) return;

    this.isLocating.set(true);

    map.off('locationfound').off('locationerror');

    const onLocationFound = (e: L.LocationEvent): void => {
      this.ngZone.run(() => {
        map.off('locationerror', onLocationError);
        this.isLocating.set(false);
        if (this.userLocationMarker && map.hasLayer(this.userLocationMarker)) {
          this.userLocationMarker.setLatLng(e.latlng);
        } else {
          this.clearUserLocationMarker();
          this.userLocationMarker = L.marker(e.latlng, { icon: MAP_CONSTANTS.DEFAULT_ICON })
            .addTo(map)
            .bindPopup(this.translate.instant('MAP.LOCATION_FOUND'));

          this.userLocationMarker.on('click', () => {
            this.clearUserLocationMarker();
          });
        }
        this.userLocationMarker?.openPopup();
      });
    };

    const onLocationError = (e: L.ErrorEvent): void => {
      this.ngZone.run(() => {
        map.off('locationfound', onLocationFound);
        this.isLocating.set(false);
        this.logger.debug('[Geolocation] GPS location access denied or failed:', { message: e.message });
        this.notificationService.error('MAP.LOCATION_ERROR');
      });
    };

    map.once('locationfound', onLocationFound);
    map.once('locationerror', onLocationError);

    try {
      map.locate({ setView: true, maxZoom: 15 });
    } catch (err) {
      this.ngZone.run(() => {
        map.off('locationfound', onLocationFound).off('locationerror', onLocationError);
        this.isLocating.set(false);
        this.logger.error('[Geolocation] Failed to initiate locate:', { error: err });
        this.notificationService.error('MAP.LOCATION_ERROR');
      });
    }
  }
}
