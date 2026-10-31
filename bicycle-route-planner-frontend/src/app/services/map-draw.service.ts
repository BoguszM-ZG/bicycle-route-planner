import { Injectable, inject, effect, untracked } from '@angular/core';
import * as L from 'leaflet';
import 'leaflet-draw';
import * as wellknown from 'wellknown';
import type { GeoJSONGeometry } from 'wellknown';
import type { Polygon, MultiPolygon } from 'geojson';
import { MapEngineService } from './map-engine.service';
import { AvoidanceZoneStateService } from './avoidance-zone-state.service';
import { RoutePlanningStateService } from './route-planning-state.service';
import { AvoidanceZoneDto } from '../models/avoidance-zone.model';
import { TranslateService } from '@ngx-translate/core';
import { LoggerService } from './logger.service';
import { MAP_CONSTANTS, MAP_PANES } from '../constants/map.constants';

@Injectable({
  providedIn: 'root',
})
export class MapDrawService {
  private static readonly DRAW_POLYGON_OPTIONS = {
    allowIntersection: false,
    showArea: false,
    shapeOptions: {
      color: 'red',
      pane: MAP_PANES.ZONES.name,
    },
  };

  private readonly mapEngine = inject(MapEngineService);
  private readonly avoidanceZoneStateService = inject(AvoidanceZoneStateService);
  private readonly routePlanningStateService = inject(RoutePlanningStateService);
  private readonly translate = inject(TranslateService);
  private readonly logger = inject(LoggerService);

  private readonly drawnItems = new L.FeatureGroup();
  private readonly tempDrawFeatureGroup = new L.FeatureGroup();
  private map?: L.Map;
  private currentDrawer?: L.Draw.Polygon;

  constructor() {
    effect(() => {
      const map = this.mapEngine.mapInstance();
      untracked(() => (map ? this.attachToMap(map) : this.detachFromMap()));
    });

    effect(() => {
      const zones = this.avoidanceZoneStateService.zones();
      this.renderZones(zones);
    });

    effect(() => {
      const mode = this.routePlanningStateService.routingMode();
      if (!this.map) {
        return;
      }

      if (mode === 'avoidanceZone') {
        this.startDrawingPolygon(this.map);
      } else {
        this.cancelDrawing();
      }
    });
  }

  private attachToMap(mapInstance: L.Map): void {
    if (this.map === mapInstance) {
      return;
    }
    if (this.map) {
      this.detachFromMap();
    }

    this.map = mapInstance;
    this.map.addLayer(this.drawnItems);
    this.map.addLayer(this.tempDrawFeatureGroup);
    this.setupDrawEvents(mapInstance);

    this.renderZones(this.avoidanceZoneStateService.zones());

    if (this.routePlanningStateService.routingMode() === 'avoidanceZone') {
      this.startDrawingPolygon(mapInstance);
    }
  }

  private detachFromMap(): void {
    this.cancelDrawing();
    if (this.map) {
      this.map.off(L.Draw.Event.CREATED, this.onDrawCreated);
      if (this.map.hasLayer(this.drawnItems)) {
        this.map.removeLayer(this.drawnItems);
      }
      if (this.map.hasLayer(this.tempDrawFeatureGroup)) {
        this.map.removeLayer(this.tempDrawFeatureGroup);
      }
      this.map = undefined;
    }
    this.drawnItems.clearLayers();
    this.tempDrawFeatureGroup.clearLayers();
  }

  private startDrawingPolygon(targetMap?: L.Map): void {
    const map = targetMap ?? this.map ?? this.mapEngine.mapInstance();
    if (!map) {
      return;
    }
    this.cancelDrawing();

    try {
      this.currentDrawer = new L.Draw.Polygon(
        map as unknown as L.DrawMap,
        MapDrawService.DRAW_POLYGON_OPTIONS,
      );
      this.currentDrawer.enable();
    } catch (err) {
      this.logger.debug('[MapDraw] Could not enable polygon drawer:', { error: err });
    }
  }

  private cancelDrawing(): void {
    if (this.currentDrawer) {
      this.currentDrawer.disable();
      this.currentDrawer = undefined;
    }
    this.tempDrawFeatureGroup.clearLayers();
    this.avoidanceZoneStateService.currentZoneDraft.set(null);
  }

  private readonly onDrawCreated = (event: L.LeafletEvent): void => {
    const layer = (event as L.DrawEvents.Created).layer as L.Polygon;
    this.tempDrawFeatureGroup.clearLayers();
    this.tempDrawFeatureGroup.addLayer(layer);

    const geoJson = layer.toGeoJSON();
    this.logger.debug('[AvoidanceZone] New polygon drawn on map.');
    const wktString = wellknown.stringify(geoJson.geometry as unknown as GeoJSONGeometry);

    this.avoidanceZoneStateService.currentZoneDraft.set(wktString);
  };

  private setupDrawEvents(map: L.Map): void {
    map.on(L.Draw.Event.CREATED, this.onDrawCreated);
  }

  private renderZones(zones: AvoidanceZoneDto[]): void {
    if (!this.map) {
      return;
    }

    this.drawnItems.clearLayers();
    const activeZones = zones.filter((z) => z.isActive);
    activeZones.forEach((zone) => {
      const geoJsonGeom = wellknown.parse(zone.zoneGeom) as (Polygon | MultiPolygon) | null;

      if (geoJsonGeom) {
        const polygonLayer = L.geoJSON<unknown, Polygon | MultiPolygon>(geoJsonGeom, {
          pane: MAP_PANES.ZONES.name,
          style: MAP_CONSTANTS.STYLES.AVOIDANCE_ZONE,
        });

        polygonLayer.bindPopup(() => this.createZonePopup(zone.name));

        this.drawnItems.addLayer(polygonLayer);
      }
    });
  }

  private createZonePopup(zoneName: string): HTMLElement {
    const container = document.createElement('div');
    const titleElement = document.createElement('b');
    titleElement.textContent = this.translate.instant('ZONE.ZONE_POPUP_TITLE');
    container.appendChild(titleElement);
    container.appendChild(document.createElement('br'));
    container.appendChild(document.createTextNode(zoneName));
    return container;
  }
}
