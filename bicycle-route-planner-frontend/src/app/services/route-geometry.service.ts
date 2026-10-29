import { Injectable } from '@angular/core';
import * as L from 'leaflet';
import type { GeometryObject, Polygon, Position } from 'geojson';
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
    if (!waypoints || waypoints.length === 0) {
      return false;
    }
    if (!avoidPolygons?.coordinates || avoidPolygons.coordinates.length === 0) {
      return false;
    }

    return waypoints.some(
      (waypoint) =>
        waypoint &&
        avoidPolygons.coordinates.some(
          (polygon) => polygon && this.isPointInPolygon(waypoint, polygon),
        ),
    );
  }

  public isPointInPolygon(point: L.LatLng, rings: Polygon['coordinates']): boolean {
    if (!point || !Number.isFinite(point.lat) || !Number.isFinite(point.lng) || !rings?.length) {
      return false;
    }

    const exterior = this.closeRing(rings[0]);
    if (!exterior) {
      return false;
    }

    const closedRings: Position[][] = [
      exterior,
      ...rings
        .slice(1)
        .map((ring) => this.closeRing(ring))
        .filter((ring): ring is Position[] => ring !== null),
    ];

    try {
      return booleanPointInPolygon([point.lng, point.lat], {
        type: 'Polygon',
        coordinates: closedRings,
      });
    } catch {
      return false;
    }
  }

  private closeRing(ring: Position[]): Position[] | null {
    if (
      !ring ||
      ring.length < 3 ||
      ring.some((pos) => !pos || !Number.isFinite(pos[0]) || !Number.isFinite(pos[1]))
    ) {
      return null;
    }

    const first = ring[0];
    const last = ring[ring.length - 1];
    const isClosed = first[0] === last[0] && first[1] === last[1];

    if (isClosed) {
      return ring.length >= 4 ? ring : null;
    }

    return [...ring, first];
  }
  public generateRouteWkt(coordinates: readonly L.LatLng[]): RouteWkt {
    if (!coordinates || coordinates.length === 0) {
      return {
        geometryWkt: stringify({ type: 'LineString', coordinates: [] }),
        startPoint: '',
      };
    }

    const validCoords =
      coordinates.length === 1
        ? [coordinates[0], coordinates[0]]
        : coordinates;
    const lineCoords: [number, number, number][] = validCoords.map((pt) => [
      pt.lng,
      pt.lat,
      typeof pt.alt === 'number' && Number.isFinite(pt.alt) ? pt.alt : 0,
    ]);

    const start = validCoords[0];

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
    try {
      const parsed = parse(zoneGeom) as GeometryObject | null;
      if (!parsed) {
        return [];
      }

      if (parsed.type === 'Polygon') {
        return parsed.coordinates.length > 0 ? [parsed.coordinates] : [];
      }

      if (parsed.type === 'MultiPolygon') {
        return parsed.coordinates.filter((poly) => poly.length > 0);
      }

      return [];
    } catch {
      return [];
    }
  }
}
