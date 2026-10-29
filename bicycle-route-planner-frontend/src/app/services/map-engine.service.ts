import { Injectable, inject, signal, effect } from '@angular/core';
import * as L from 'leaflet';
import { MAP_CONSTANTS, MAP_PANES } from '../constants/map.constants';
import { LoggerService } from './logger.service';
import { NotificationService } from './notification.service';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class MapEngineService {
  private readonly logger = inject(LoggerService);
  private readonly notificationService = inject(NotificationService);
  private readonly translate = inject(TranslateService);
  private readonly authService = inject(AuthService);

  private readonly _mapInstance = signal<L.Map | null>(null);
  public readonly mapInstance = this._mapInstance.asReadonly();

  private readonly _isLocating = signal(false);
  public readonly isLocating = this._isLocating.asReadonly();

  private userLocationMarker?: L.Marker;

  constructor() {
    effect(() => {
      const user = this.authService.currentUser();
      if (!user) {
        this.clearUserLocationMarker();
      }
    });
  }

  public initMap(elementId: string): void {
    if (this.mapInstance()) {
      this.destroyMap();
    }

    const map = L.map(elementId, { zoomControl: false }).setView(
      MAP_CONSTANTS.DEFAULT_CENTER,
      MAP_CONSTANTS.DEFAULT_ZOOM,
    );

    L.tileLayer(MAP_CONSTANTS.TILE_LAYER.URL, {
      maxZoom: MAP_CONSTANTS.TILE_LAYER.MAX_ZOOM,
      attribution: MAP_CONSTANTS.TILE_LAYER.ATTRIBUTION,
    }).addTo(map);

    this.initPanes(map);

    this._mapInstance.set(map);

    this.logger.debug('[MapEngine] Leaflet map initialized with custom panes');
  }

  private initPanes(map: L.Map): void {
    const paneConfigs = [MAP_PANES.ZONES, MAP_PANES.ROUTES, MAP_PANES.MARKERS, MAP_PANES.SEARCH];

    for (const paneConfig of paneConfigs) {
      const pane = map.createPane(paneConfig.name);
      pane.style.zIndex = paneConfig.zIndex;
    }
  }

  public destroyMap(): void {
    const map = this.mapInstance();
    if (map) {
      map.stopLocate();
      map.off('locationfound').off('locationerror');
      this.clearUserLocationMarker();
      map.remove();
    }
    this._isLocating.set(false);
    this._mapInstance.set(null);
    this.logger.debug('[MapEngine] Map instance destroyed');
  }

  public flyTo(coords: L.LatLngExpression, zoom?: number, options?: L.ZoomPanOptions): void {
    const map = this.mapInstance();
    if (map) {
      map.flyTo(coords, zoom, options);
    }
  }

  public fitBounds(bounds: L.LatLngBounds, padding?: [number, number]): void {
    const map = this.mapInstance();
    if (!map || !bounds.isValid()) {
      return;
    }
    const options: L.FitBoundsOptions = padding ? { padding } : {};
    map.fitBounds(bounds, options);
  }

  public getCenter(): { lat: number; lng: number } | null {
    const map = this.mapInstance();
    if (!map) {
      return null;
    }
    const center = map.getCenter();
    return { lat: center.lat, lng: center.lng };
  }

  public locateUser(): void {
    if (this.isLocating()) {
      return;
    }
    this.logger.debug('[Geolocation] Requesting user GPS location...');
    const map = this.mapInstance();
    if (!map) {
      return;
    }

    this._isLocating.set(true);

    const onLocationFound = (e: L.LocationEvent): void => {
      map.off('locationerror', onLocationError);
      if (this.mapInstance() !== map) {
        return;
      }
      this._isLocating.set(false);

      if (this.userLocationMarker && map.hasLayer(this.userLocationMarker)) {
        this.userLocationMarker.setLatLng(e.latlng);
      } else {
        this.clearUserLocationMarker();
        this.userLocationMarker = L.marker(e.latlng, {
          icon: MAP_CONSTANTS.DEFAULT_ICON,
          pane: MAP_PANES.MARKERS.name,
        })
          .addTo(map)
          .bindPopup(this.translate.instant('MAP.LOCATION_FOUND'));

        this.userLocationMarker.on('click', () => {
          this.clearUserLocationMarker();
        });
      }
      this.userLocationMarker?.openPopup();
    };

    const onLocationError = (e: L.ErrorEvent): void => {
      map.off('locationfound', onLocationFound);
      if (this.mapInstance() !== map) {
        return;
      }
      this._isLocating.set(false);
      this.logger.debug('[Geolocation] GPS location access denied or failed:', {
        message: e.message,
      });
      this.notificationService.error('MAP.LOCATION_ERROR');
    };

    map.once('locationfound', onLocationFound);
    map.once('locationerror', onLocationError);

    try {
      map.locate({ setView: true, maxZoom: MAP_CONSTANTS.SEARCH_CONFIG.MAP_ZOOM });
    } catch (err) {
      map.off('locationfound', onLocationFound).off('locationerror', onLocationError);
      this._isLocating.set(false);
      this.logger.error('[Geolocation] Failed to initiate locate:', { error: err });
      this.notificationService.error('MAP.LOCATION_ERROR');
    }
  }

  public clearUserLocationMarker(): void {
    if (this.userLocationMarker) {
      this.userLocationMarker.remove();
      this.userLocationMarker.off();
      this.userLocationMarker = undefined;
    }
  }
}
