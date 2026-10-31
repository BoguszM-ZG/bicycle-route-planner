import { Injectable, inject, effect, untracked, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import * as L from 'leaflet';
import { parse } from 'wellknown';
import type { Position } from 'geojson';
import { TranslateService } from '@ngx-translate/core';
import { MapEngineService } from './map-engine.service';
import { RoutePlanningStateService } from './route-planning-state.service';
import { RouteStateService } from './route-state.service';
import { AuthService } from './auth.service';
import { LoggerService } from './logger.service';
import { MAP_CONSTANTS, MAP_PANES } from '../constants/map.constants';
import {
  ActiveRouteGeometry,
  RouteDetailDto,
  RouteGeometry,
  RouteType,
} from '../models/route.model';

@Injectable({
  providedIn: 'root',
})
export class MapRouteLayerService {
  private static readonly TOOLTIP_OFFSET_Y = -35;
  private static readonly ROUTE_BOUNDS_PADDING: [number, number] = [50, 50];
  private static readonly WAYPOINT_POPUP_OPTIONS: L.PopupOptions = {
    closeButton: true,
    autoClose: true,
    closeOnClick: true,
  };
  private static readonly WAYPOINT_TOOLTIP_OPTIONS: L.TooltipOptions = {
    direction: 'top',
    offset: [0, MapRouteLayerService.TOOLTIP_OFFSET_Y],
  };
  private readonly mapEngine = inject(MapEngineService);
  private readonly routePlanningStateService = inject(RoutePlanningStateService);
  private readonly routeStateService = inject(RouteStateService);
  private readonly authService = inject(AuthService);
  private readonly logger = inject(LoggerService);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  private currentMap?: L.Map;
  private readonly routesFeatureGroup = new L.FeatureGroup();
  private readonly markersFeatureGroup = new L.FeatureGroup();
  private readonly savedRouteFeatureGroup = new L.FeatureGroup();
  private readonly searchFeatureGroup = new L.FeatureGroup();

  private waypointMarkers: L.Marker[] = [];
  private directRouteLayer?: L.Polyline;
  private roundTripLayer?: L.Polyline;
  private roundTripMarker?: L.Marker;
  private searchMarker?: L.Marker;

  public readonly searchPin = signal<L.LatLngLiteral | null>(null);

  constructor() {
    effect(() => {
      const map = this.mapEngine.mapInstance();
      untracked(() => (map ? this.attachToMap(map) : this.detachFromMap()));
    });

    effect(() => {
      const geometry = this.routePlanningStateService.activeRouteGeometry();
      this.renderDraftRoute(geometry);
    });

    effect(() => {
      const waypoints = this.routePlanningStateService.waypoints();
      if (waypoints.length === 0 && this.waypointMarkers.length > 0) {
        this.clearWaypointMarkers();
      }
    });

    effect(() => {
      const selected = this.routeStateService.selectedRoute();
      if (selected) {
        this.renderSavedRoute(selected);
      } else {
        this.clearSavedRouteLayers();
      }
    });

    effect(() => {
      const pin = this.searchPin();
      this.renderSearchPin(pin);
    });

    effect(() => {
      const user = this.authService.currentUser();
      if (!user) {
        this.clearAll();
      }
    });

    this.translate.onLangChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.updateWaypointPopups();
      this.updateWaypointTooltips();
    });
  }

  private attachToMap(map: L.Map): void {
    if (this.currentMap === map) {
      return;
    }
    if (this.currentMap) {
      this.detachFromMap();
    }

    this.currentMap = map;
    this.routesFeatureGroup.addTo(map);
    this.markersFeatureGroup.addTo(map);
    this.savedRouteFeatureGroup.addTo(map);
    this.searchFeatureGroup.addTo(map);

    map.on('click', this.onMapClick);

    const currentGeometry = this.routePlanningStateService.activeRouteGeometry();
    if (currentGeometry) {
      this.renderDraftRoute(currentGeometry);
    }

    const currentWaypoints = this.routePlanningStateService.waypoints();
    if (currentWaypoints.length > 0 && this.waypointMarkers.length === 0) {
      currentWaypoints.forEach((pt) => {
        this.createAndAddWaypointMarker(pt);
      });
    }

    const currentSavedRoute = this.routeStateService.selectedRoute();
    if (currentSavedRoute) {
      this.renderSavedRoute(currentSavedRoute);
    }

    const pin = this.searchPin();
    if (pin) {
      this.renderSearchPin(pin);
    }
  }

  private detachFromMap(): void {
    if (this.currentMap) {
      this.currentMap.off('click', this.onMapClick);
      if (this.currentMap.hasLayer(this.routesFeatureGroup)) {
        this.currentMap.removeLayer(this.routesFeatureGroup);
      }
      if (this.currentMap.hasLayer(this.markersFeatureGroup)) {
        this.currentMap.removeLayer(this.markersFeatureGroup);
      }
      if (this.currentMap.hasLayer(this.savedRouteFeatureGroup)) {
        this.currentMap.removeLayer(this.savedRouteFeatureGroup);
      }
      if (this.currentMap.hasLayer(this.searchFeatureGroup)) {
        this.currentMap.removeLayer(this.searchFeatureGroup);
      }
      this.currentMap = undefined;
    }
    this.clearDraftLayers();
    this.clearSavedRouteLayers();
    this.clearSearchPinLayer();
  }

  private readonly onMapClick = (e: L.LeafletMouseEvent): void => {
    switch (this.routePlanningStateService.routingMode()) {
      case 'direct':
        this.addWaypoint(e.latlng);
        break;
      case 'roundTrip':
        this.routePlanningStateService.generateRoundTrip(e.latlng);
        break;
      case 'avoidanceZone':
        break;
    }
  };

  public addWaypoint(latLng: L.LatLng): void {
    if (this.routePlanningStateService.routingMode() === 'avoidanceZone') {
      return;
    }

    if (!this.currentMap) {
      return;
    }

    this.createAndAddWaypointMarker(latLng);
    this.routePlanningStateService.addWaypoint(latLng);
  }

  private createAndAddWaypointMarker(latLng: L.LatLng): L.Marker {
    const marker = L.marker(latLng, {
      draggable: true,
      icon: MAP_CONSTANTS.DEFAULT_ICON,
      pane: MAP_PANES.MARKERS.name,
    }).addTo(this.markersFeatureGroup);

    this.setupMarkerEvents(marker);
    this.waypointMarkers.push(marker);
    return marker;
  }

  private setupMarkerEvents(marker: L.Marker): void {
    marker.bindPopup(this.createWaypointPopup(marker), MapRouteLayerService.WAYPOINT_POPUP_OPTIONS);
    marker.bindTooltip(this.createWaypointTooltip(), MapRouteLayerService.WAYPOINT_TOOLTIP_OPTIONS);

    marker.on('popupopen', () => {
      marker.closeTooltip();
    });

    marker.on('tooltipopen', () => {
      if (marker.isPopupOpen()) {
        marker.closeTooltip();
      }
    });

    marker.on('dragstart', () => {
      marker.closeTooltip();
    });

    marker.on('contextmenu', (event: L.LeafletMouseEvent) => {
      if (event.originalEvent) {
        event.originalEvent.preventDefault();
        event.originalEvent.stopPropagation();
      }
      this.deleteWaypoint(marker);
    });

    marker.on('dragend', (event: L.LeafletEvent) => {
      const targetMarker = event.target as L.Marker;
      const targetIndex = this.waypointMarkers.indexOf(targetMarker);
      if (targetIndex === -1) {
        return;
      }

      this.routePlanningStateService.updateWaypoint(targetIndex, targetMarker.getLatLng());
    });
  }

  private createWaypointPopup(marker: L.Marker): HTMLElement {
    const container = document.createElement('div');
    container.className = 'waypoint-popup-container';
    L.DomEvent.disableClickPropagation(container);
    L.DomEvent.disableScrollPropagation(container);

    const title = document.createElement('span');
    title.className = 'waypoint-popup-title';
    title.textContent = this.translate.instant('MAP.WAYPOINT_POPUP_TITLE');
    container.appendChild(title);

    const deleteButton = document.createElement('button');
    deleteButton.type = 'button';
    deleteButton.className = 'waypoint-popup-delete-btn';
    deleteButton.textContent = this.translate.instant('MAP.DELETE_WAYPOINT');

    deleteButton.addEventListener('click', (event: MouseEvent) => {
      event.preventDefault();
      event.stopPropagation();
      this.logger.debug('[MapRouteLayer] User clicked delete waypoint button');
      this.deleteWaypoint(marker);
    });

    container.appendChild(deleteButton);
    return container;
  }

  private createWaypointTooltip(): string {
    return this.translate.instant('MAP.WAYPOINT_TOOLTIP');
  }

  private updateWaypointPopups(): void {
    this.waypointMarkers.forEach((marker) => {
      marker.setPopupContent(this.createWaypointPopup(marker));
    });
  }

  private updateWaypointTooltips(): void {
    const tooltipContent = this.createWaypointTooltip();
    this.waypointMarkers.forEach((marker) => {
      marker.setTooltipContent(tooltipContent);
    });
  }

  public deleteWaypoint(target: L.Marker | number): void {
    const index = typeof target === 'number' ? target : this.waypointMarkers.indexOf(target);
    if (index < 0 || index >= this.waypointMarkers.length) {
      return;
    }

    this.removeWaypointMarker(this.waypointMarkers[index]);
    this.waypointMarkers.splice(index, 1);
    this.routePlanningStateService.deleteWaypoint(index);
  }

  private renderDraftRoute(geometry: ActiveRouteGeometry | null): void {
    if (!geometry || !this.currentMap) {
      this.removeDraftRouteLayers();
      return;
    }

    const isLoop = geometry.routeType === 'LOOP';
    const hasCoordinates = geometry.coordinates.length > 0;

    if (!isLoop && hasCoordinates) {
      this.directRouteLayer = this.upsertRoutePolyline(
        this.directRouteLayer,
        geometry.coordinates,
        MAP_CONSTANTS.STYLES.DRAFT_ROUTE,
      );
    } else {
      this.removeDirectRouteLayer();
    }

    if (isLoop && hasCoordinates) {
      this.roundTripLayer = this.upsertRoutePolyline(
        this.roundTripLayer,
        geometry.coordinates,
        MAP_CONSTANTS.STYLES.LOOP_ROUTE,
      );
    } else {
      this.removeRoundTripLayer();
    }

    if (isLoop && geometry.startPoint) {
      if (this.roundTripMarker && this.markersFeatureGroup.hasLayer(this.roundTripMarker)) {
        this.roundTripMarker.setLatLng(geometry.startPoint);
      } else {
        this.roundTripMarker = L.marker(geometry.startPoint, {
          icon: MAP_CONSTANTS.DEFAULT_ICON,
          pane: MAP_PANES.MARKERS.name,
        }).addTo(this.markersFeatureGroup);
      }
    } else {
      this.removeRoundTripMarker();
    }
  }

  private upsertRoutePolyline(
    layer: L.Polyline | undefined,
    coordinates: L.LatLng[],
    style: L.PolylineOptions,
  ): L.Polyline {
    if (layer && this.routesFeatureGroup.hasLayer(layer)) {
      layer.setLatLngs(coordinates);
      return layer;
    }
    return L.polyline(coordinates, { ...style, pane: MAP_PANES.ROUTES.name }).addTo(
      this.routesFeatureGroup,
    );
  }

  private removeDraftRouteLayers(): void {
    this.removeDirectRouteLayer();
    this.removeRoundTripLayer();
    this.removeRoundTripMarker();
  }

  private removeDirectRouteLayer(): void {
    if (this.directRouteLayer) {
      this.routesFeatureGroup.removeLayer(this.directRouteLayer);
      this.directRouteLayer = undefined;
    }
  }

  private removeRoundTripLayer(): void {
    if (this.roundTripLayer) {
      this.routesFeatureGroup.removeLayer(this.roundTripLayer);
      this.roundTripLayer = undefined;
    }
  }

  private removeRoundTripMarker(): void {
    if (this.roundTripMarker) {
      this.markersFeatureGroup.removeLayer(this.roundTripMarker);
      this.roundTripMarker = undefined;
    }
  }

  private renderSavedRoute(routeState: RouteDetailDto): void {
    this.clearSavedRouteLayers();
    if (!this.currentMap) {
      return;
    }

    const geoJsonGeometry = parse(routeState.routeGeom) as RouteGeometry | null;
    if (!geoJsonGeometry) {
      return;
    }

    const savedRouteLayer = L.geoJson<unknown, RouteGeometry>(geoJsonGeometry, {
      style: MAP_CONSTANTS.STYLES.SAVED_ROUTE,
      pane: MAP_PANES.ROUTES.name,
    }).addTo(this.savedRouteFeatureGroup);

    this.addSavedRouteMarkers(geoJsonGeometry, routeState.routeType);
    this.mapEngine.fitBounds(
      savedRouteLayer.getBounds(),
      MapRouteLayerService.ROUTE_BOUNDS_PADDING,
    );
  }

  private addSavedRouteMarkers(geoJsonGeometry: RouteGeometry, routeType: RouteType): void {
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

    if (!startCoords || !endCoords) {
      return;
    }

    if (routeType === 'LOOP') {
      this.addSavedRouteMarker(startCoords, MAP_CONSTANTS.MARKERS.LOOP_ICON, 'MAP.MARKER_LOOP');
    } else {
      this.addSavedRouteMarker(startCoords, MAP_CONSTANTS.MARKERS.START_ICON, 'MAP.MARKER_START');
      this.addSavedRouteMarker(endCoords, MAP_CONSTANTS.MARKERS.END_ICON, 'MAP.MARKER_END');
    }
  }

  private addSavedRouteMarker(
    [lng, lat]: Position,
    icon: L.Icon | L.DivIcon,
    titleKey: string,
  ): void {
    L.marker([lat, lng], {
      icon,
      title: this.translate.instant(titleKey),
      pane: MAP_PANES.MARKERS.name,
    }).addTo(this.savedRouteFeatureGroup);
  }

  public setSearchPin(pin: L.LatLngLiteral | null): void {
    this.searchPin.set(pin);
  }

  private renderSearchPin(pin: L.LatLngLiteral | null): void {
    this.clearSearchPinLayer();

    if (!pin || !this.currentMap) {
      return;
    }

    this.searchMarker = L.marker([pin.lat, pin.lng], {
      icon: MAP_CONSTANTS.SEARCH_PIN,
      pane: MAP_PANES.SEARCH.name,
    }).addTo(this.searchFeatureGroup);

    this.searchMarker.once('click', () => {
      this.setSearchPin(null);
    });
  }

  private clearSearchPinLayer(): void {
    if (this.searchMarker) {
      this.searchFeatureGroup.removeLayer(this.searchMarker);
      this.searchMarker.off();
      this.searchMarker = undefined;
    }
  }

  private clearDraftLayers(): void {
    this.removeDraftRouteLayers();
    this.clearWaypointMarkers();
  }

  public clearWaypointMarkers(): void {
    this.waypointMarkers.forEach((marker) => this.removeWaypointMarker(marker));
    this.waypointMarkers = [];
  }

  private removeWaypointMarker(marker: L.Marker): void {
    marker.closePopup();
    marker.closeTooltip();
    this.markersFeatureGroup.removeLayer(marker);
    marker.off();
  }

  private clearSavedRouteLayers(): void {
    this.savedRouteFeatureGroup.clearLayers();
  }

  public clearRoute(): void {
    this.clearDraftLayers();
    this.clearSavedRouteLayers();
    this.routePlanningStateService.clearRoute();
  }

  private clearAll(): void {
    this.clearRoute();
    this.setSearchPin(null);
  }
}
