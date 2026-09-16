import { Injectable, inject, effect, NgZone, signal } from "@angular/core";
import * as L from 'leaflet';
import 'leaflet-draw';
import * as wellknown from 'wellknown';
import { Subject } from 'rxjs';
import { AvoidanceZoneStateService } from "./avoidance-zone-state.service";
import { AvoidanceZoneDto} from '../models/avoidance-zone.model';

@Injectable({
  providedIn: 'root',
})
export class MapDrawService {
  private map!: L.Map;

  private readonly drawnItems: L.FeatureGroup = new L.FeatureGroup();

  private readonly avoidanceZoneStateService = inject(AvoidanceZoneStateService);
  private readonly ngZone = inject(NgZone);

  public currentZoneDraft = signal<string | null>(null);
  private tempDrawnLayer?: L.Layer;
  private currentDrawer?: any;

  constructor() {
    effect(() => {
      const zones = this.avoidanceZoneStateService.zones();
      this.renderZones(zones);
    });
  }

  public init(mapInstance: L.Map): void {
    this.map = mapInstance;
    this.map.addLayer(this.drawnItems);
    this.setupDrawEvents();
  }

  public startDrawingPolygon(): void {
    if (!this.map) return;
    this.cancelDrawing();

    const polygonOptions = {
      allowIntersection: false,
      showArea: true,
      shapeOptions: { color: 'red' },
    };

    this.currentDrawer = new (L.Draw.Polygon as any)(this.map, polygonOptions);
    this.currentDrawer.enable();
  }

  public cancelDrawing(): void {
    if (this.currentDrawer) {
      this.currentDrawer.disable();
      this.currentDrawer = undefined;
    }
    if (this.tempDrawnLayer) {
      if (this.map) this.map.removeLayer(this.tempDrawnLayer);
      this.tempDrawnLayer = undefined;
    }
    this.currentZoneDraft.set(null);
  }

  public triggerSaveModal(): void {
    const draft = this.currentZoneDraft();
    if (draft) {
      this.zoneDrawn$.next(draft);
    }
  }

  public zoneDrawn$ = new Subject<string>();

  private setupDrawEvents(): void {
    this.map.on(L.Draw.Event.CREATED, (e: any) => {
      this.ngZone.run(() => {
        this.tempDrawnLayer = e.layer;
        this.map.addLayer(this.tempDrawnLayer!);

        const layer = e.layer as L.Polygon;
        const geoJson = layer.toGeoJSON();
        const wktString = wellknown.stringify(geoJson.geometry as any);

        this.currentZoneDraft.set(wktString);
      });
    });
  }

  private renderZones(zones: AvoidanceZoneDto[]): void {
    if (!this.map) return;

    this.drawnItems.clearLayers();
    const activeZones = zones.filter((z) => z.isActive);
    activeZones.forEach((zone) => {
      const geoJsonGeom = wellknown.parse(zone.zoneGeom);

      if (geoJsonGeom) {
        const polygonLayer = L.geoJSON(geoJsonGeom as any, {
          style: { color: 'red', weight: 2, fillColor: '#ff0000', fillOpacity: 0.2 },
        });

        polygonLayer.bindPopup(`<b>Strefa omijana:</b><br>${zone.name}`);

        this.drawnItems.addLayer(polygonLayer);
      }
    });
  }

}
