import { Injectable } from '@angular/core';
import * as L from 'leaflet';
import type { Polygon, Position } from 'geojson';
import { booleanPointInPolygon } from '@turf/boolean-point-in-polygon';
import { parse, stringify } from 'wellknown';
import { RouteWkt } from '../models/route.model';
import { AvoidPolygonsGeoJson } from '../models/ors.model';

@Injectable({
  providedIn: 'root',
})
export class RouteGeometryService {
  public hasWaypointInAvoidanceZone(
    waypoints: readonly L.LatLng[],
    avoidPolygons?: AvoidPolygonsGeoJson,
  ): boolean {
    if (!avoidPolygons) {
      return false;
    }

    return waypoints.some((waypoint) =>
      avoidPolygons.coordinates.some((rings) =>
        booleanPointInPolygon([waypoint.lng, waypoint.lat], {
          type: 'Polygon',
          coordinates: rings,
        }),
      ),
    );
  }

  public generateRouteWkt(coordinates: readonly L.LatLng[]): RouteWkt {
    const lineCoords: [number, number, number][] = coordinates.map((pt) => [
      pt.lng,
      pt.lat,
      typeof pt.alt === 'number' && Number.isFinite(pt.alt) ? pt.alt : 0,
    ]);

    const start = coordinates[0];

    return {
      geometryWkt: stringify({
        type: 'LineString',
        coordinates: lineCoords,
      }),
      startPoint: stringify({
        type: 'Point',
        coordinates: [start.lng, start.lat],
      }),
    };
  }

  public extractPolygonCoordinates(zoneGeom: string): Position[][][] {
    const parsed = parse(zoneGeom) as Polygon | null;
    return parsed ? [parsed.coordinates] : [];
  }
}
