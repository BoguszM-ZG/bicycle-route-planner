import { Injectable, inject, effect } from "@angular/core";
import * as L from 'leaflet';
import 'leaflet-draw';
import * as wellknown from 'wellknown';
import { AvoidanceZoneStateService } from "./avoidance-zone-state.service";
import { AvoidanceZoneDto} from '../models/avoidance-zone.model';

@Injectable({
  providedIn: 'root',
})
export class MapDrawService {
  private map!: L.Map;

  private readonly drawnItems: L.FeatureGroup = new L.FeatureGroup();
  private drawControl!: L.Control.Draw;

  private readonly avoidanceZoneStateService = inject(AvoidanceZoneStateService);

  constructor() {
    effect(() => {
      const zones = this.avoidanceZoneStateService.zones();
      this.renderZones(zones);
    });
  }

  public init(mapInstance: L.Map): void {
    this.map = mapInstance;
    this.map.addLayer(this.drawnItems);

    this.drawControl = new L.Control.Draw({
      draw: {
        polygon: {
          allowIntersection: false,
          showArea: true,
          shapeOptions: { color: 'red' },
        },
        polyline: false,
        circle: false,
        rectangle: false,
        marker: false,
        circlemarker: false,
      },
      edit: {
        featureGroup: this.drawnItems,
        edit: false,
        remove: false,
      },
    });

    this.map.addControl(this.drawControl);
    this.setupDrawEvents();
  }

  private setupDrawEvents(): void {
    this.map.on(L.Draw.Event.CREATED, (e: any) => {
      const layer = e.layer as L.Polygon;
      const geoJson = layer.toGeoJSON();

      const wktString = wellknown.stringify(geoJson.geometry as any);

      const zoneName = prompt('Podaj nazwę dla tej strefy omijalnej');

      if (zoneName && zoneName.trim() !== '') {
        this.avoidanceZoneStateService.saveZone(zoneName, wktString);
      }
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
