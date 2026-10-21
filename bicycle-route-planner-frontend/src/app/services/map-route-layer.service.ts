import { Injectable, NgZone, inject, effect } from '@angular/core';
import * as L from 'leaflet';
import { RoutePlanningStateService } from './route-planning-state.service';
import { RouteStateService } from './route-state.service';
import { MAP_CONSTANTS } from '../constants/map.constants';
import { AuthService } from './auth.service';
import { ActiveRouteGeometry } from '../models/route.model';

export type WaypointPopupFactory = (marker: L.Marker) => HTMLElement;
export type WaypointTooltipFactory = (marker: L.Marker) => string;

@Injectable({
  providedIn: 'root',
})
export class MapRouteLayerService {
  private readonly ngZone = inject(NgZone);
  private readonly routePlanningStateService = inject(RoutePlanningStateService);
  private readonly routeStateService = inject(RouteStateService);
  private readonly authService = inject(AuthService);

  private map?: L.Map;
  private waypointMarkers: L.Marker[] = [];
  private directRouteLayer?: L.Polyline;
  private roundTripLayer?: L.Polyline;
  private roundTripMarker?: L.Marker;
  private waypointPopupFactory?: WaypointPopupFactory;
  private waypointTooltipFactory?: WaypointTooltipFactory;

  public readonly currentRouteDraft = this.routePlanningStateService.currentRouteDraft;
  public readonly routingMode = this.routePlanningStateService.routingMode;
  public readonly roundTripDistanceMeters = this.routePlanningStateService.roundTripDistanceMeters;
  public readonly roundTripDistanceKm = this.routePlanningStateService.roundTripDistanceKm;

  constructor() {
    effect(() => {
      const geometry = this.routePlanningStateService.activeRouteGeometry();
      this.renderActiveRoute(geometry);
    });

    effect(() => {
      const user = this.authService.currentUser();
      if (!user) {
        this.clearMapLayers();
      }
    });
  }

  public getWaypointMarkers(): readonly L.Marker[] {
    return this.waypointMarkers;
  }

  public getWaypoints(): readonly L.LatLng[] {
    return this.routePlanningStateService.getWaypoints();
  }

  public initRouting(
    map: L.Map,
    popupFactory?: WaypointPopupFactory,
    tooltipFactory?: WaypointTooltipFactory,
  ): void {
    if (this.map) {
      this.map
        .off('draw:drawstart', this.onDrawStart)
        .off('draw:drawstop', this.onDrawStop)
        .off('click', this.onMapClick);
      this.clearRoute();
      this.map = undefined;
    }

    this.map = map;
    if (popupFactory !== undefined) {
      this.waypointPopupFactory = popupFactory;
    }
    if (tooltipFactory !== undefined) {
      this.waypointTooltipFactory = tooltipFactory;
    }
    this.setupMapEventListeners(map);

    const currentGeometry = this.routePlanningStateService.activeRouteGeometry();
    if (currentGeometry) {
      this.renderActiveRoute(currentGeometry);
    }
  }

  public setWaypointPopupFactory(factory: WaypointPopupFactory | null): void {
    this.waypointPopupFactory = factory ?? undefined;
    if (this.waypointPopupFactory) {
      this.updateWaypointPopups();
    } else {
      this.waypointMarkers.forEach((marker) => {
        marker.closePopup();
        marker.unbindPopup();
      });
    }
  }

  public setWaypointTooltipFactory(factory: WaypointTooltipFactory | null): void {
    this.waypointTooltipFactory = factory ?? undefined;
    if (this.waypointTooltipFactory) {
      this.updateWaypointTooltips();
    } else {
      this.waypointMarkers.forEach((marker) => {
        marker.closeTooltip();
        marker.unbindTooltip();
      });
    }
  }

  public updateWaypointPopups(): void {
    if (this.waypointPopupFactory) {
      this.waypointMarkers.forEach((marker) => {
        const popupContent = this.waypointPopupFactory!(marker);
        const existingPopup = marker.getPopup();
        if (existingPopup) {
          marker.setPopupContent(popupContent);
        } else {
          marker.bindPopup(popupContent, {
            closeButton: true,
            autoClose: true,
            closeOnClick: true,
          });
        }
      });
    }
    this.updateWaypointTooltips();
  }

  public updateWaypointTooltips(): void {
    if (!this.waypointTooltipFactory) {
      return;
    }
    this.waypointMarkers.forEach((marker) => {
      const tooltipContent = this.waypointTooltipFactory!(marker);
      const existingTooltip = marker.getTooltip();
      if (existingTooltip) {
        marker.setTooltipContent(tooltipContent);
      } else {
        marker.bindTooltip(tooltipContent, {
          direction: 'top',
          offset: [0, -35],
        });
      }
    });
  }

  private isDrawingMode = false;
  private readonly onDrawStart = (): void => {
    this.isDrawingMode = true;
  };
  private readonly onDrawStop = (): void => {
    this.isDrawingMode = false;
  };
  private readonly onMapClick = (e: L.LeafletMouseEvent): void => {
    if (!this.map) return;
    this.handleMapClick(e, this.map);
  };

  private setupMapEventListeners(map: L.Map): void {
    this.isDrawingMode = false;
    map.on('draw:drawstart', this.onDrawStart);
    map.on('draw:drawstop', this.onDrawStop);
    map.on('click', this.onMapClick);
  }

  private handleMapClick(e: L.LeafletMouseEvent, map: L.Map): void {
    if (this.isDrawingMode || this.routingMode() === 'avoidanceZone') {
      return;
    }

    if (this.routingMode() === 'roundTrip') {
      this.generateRoundTrip(e.latlng, this.roundTripDistanceMeters(), map);
      return;
    }

    this.addWaypoint(e.latlng, map);
  }

  public addWaypoint(latLng: L.LatLng, map?: L.Map): void {
    const targetMap = map ?? this.map;
    if (!targetMap) {
      return;
    }

    if (this.routeStateService.selectedRoute()) {
      this.routeStateService.clearSelectedRoute();
    }

    const marker = L.marker(latLng, {
      draggable: true,
      icon: MAP_CONSTANTS.DEFAULT_ICON,
    }).addTo(targetMap);

    this.setupMarkerEvents(marker);
    this.waypointMarkers.push(marker);

    this.routePlanningStateService.addWaypoint(latLng);
  }

  private setupMarkerEvents(marker: L.Marker): void {
    if (this.waypointPopupFactory) {
      marker.bindPopup(this.waypointPopupFactory(marker), {
        closeButton: true,
        autoClose: true,
        closeOnClick: true,
      });
    }

    if (this.waypointTooltipFactory) {
      marker.bindTooltip(this.waypointTooltipFactory(marker), {
        direction: 'top',
        offset: [0, -35],
      });
    }

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
      this.ngZone.run(() => {
        this.deleteWaypoint(marker);
      });
    });

    marker.on('dragend', (event: L.LeafletEvent) => {
      this.ngZone.run(() => {
        const targetMarker = event.target as L.Marker;
        const targetIndex = this.waypointMarkers.indexOf(targetMarker);
        if (targetIndex === -1) {
          return;
        }

        this.routePlanningStateService.updateWaypoint(targetIndex, targetMarker.getLatLng());
      });
    });
  }

  public deleteWaypoint(target: L.Marker | number): void {
    const index = typeof target === 'number' ? target : this.waypointMarkers.indexOf(target);
    if (index < 0 || index >= this.waypointMarkers.length) {
      return;
    }

    const marker = this.waypointMarkers[index];
    if (marker) {
      marker.closePopup();
      marker.closeTooltip();
      if (this.map) {
        this.map.removeLayer(marker);
      }
      marker.off();
    }

    this.waypointMarkers.splice(index, 1);
    this.routePlanningStateService.deleteWaypoint(index);
  }

  public generateRoundTrip(startPoint: L.LatLng, distanceMeters?: number, _map?: L.Map): void {
    if (this.routeStateService.selectedRoute()) {
      this.routeStateService.clearSelectedRoute();
    }
    this.routePlanningStateService.generateRoundTrip(startPoint, distanceMeters);
  }

  private renderActiveRoute(geometry: ActiveRouteGeometry | null): void {
    if (!this.map) {
      return;
    }

    this.removeDirectRouteLayer();
    this.removeRoundTripLayers();

    if (!geometry) {
      return;
    }

    if (geometry.routeType === 'POINT_TO_POINT') {
      this.directRouteLayer = L.polyline(
        geometry.coordinates,
        MAP_CONSTANTS.STYLES.DRAFT_ROUTE,
      ).addTo(this.map);
    } else if (geometry.routeType === 'LOOP') {
      this.roundTripLayer = L.polyline(
        geometry.coordinates,
        MAP_CONSTANTS.STYLES.LOOP_ROUTE,
      ).addTo(this.map);
      if (geometry.startPoint) {
        this.roundTripMarker = L.marker(geometry.startPoint, {
          icon: MAP_CONSTANTS.DEFAULT_ICON,
        }).addTo(this.map);
      }
    }
  }

  private removeDirectRouteLayer(): void {
    if (this.directRouteLayer && this.map) {
      this.map.removeLayer(this.directRouteLayer);
    }
    this.directRouteLayer = undefined;
  }

  private removeRoundTripLayers(): void {
    if (this.map) {
      if (this.roundTripLayer) {
        this.map.removeLayer(this.roundTripLayer);
      }
      if (this.roundTripMarker) {
        this.map.removeLayer(this.roundTripMarker);
      }
    }
    this.roundTripMarker?.off();
    this.roundTripLayer = undefined;
    this.roundTripMarker = undefined;
  }

  public clearMapLayers(): void {
    this.removeDirectRouteLayer();
    this.removeRoundTripLayers();

    this.waypointMarkers.forEach((marker) => {
      marker.closePopup();
      marker.closeTooltip();
      if (this.map) {
        this.map.removeLayer(marker);
      }
      marker.off();
    });

    this.waypointMarkers = [];
  }

  public clearRoute(): void {
    this.clearMapLayers();
    this.routePlanningStateService.clearRoute();
  }

  public destroyRouting(): void {
    if (this.map) {
      this.map
        .off('draw:drawstart', this.onDrawStart)
        .off('draw:drawstop', this.onDrawStop)
        .off('click', this.onMapClick);
    }
    this.clearRoute();
    this.map = undefined;
    this.waypointPopupFactory = undefined;
    this.waypointTooltipFactory = undefined;
  }
}
