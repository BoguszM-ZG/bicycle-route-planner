import { Injectable } from '@angular/core';
import * as L from 'leaflet';
import type { Polygon, Position } from 'geojson';
import { RouteStats, AvoidPolygonsGeoJson } from '../models/route.model';

@Injectable({
  providedIn: 'root',
})
export class RouteCalculationService {
  private static readonly MIN_POLYGON_RING_VERTICES = 3;
  private static readonly GEOMETRY_EPSILON = 1e-9;
  private static readonly GEOMETRY_EPSILON_SQ = 1e-18;

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
    if (!point || !rings || rings.length === 0) {
      return false;
    }

    for (const ring of rings) {
      if (this.isPointOnRingBoundary(point, ring)) {
        return true;
      }
    }

    if (!this.isPointInPolygonRing(point, rings[0])) {
      return false;
    }

    for (let i = 1; i < rings.length; i++) {
      if (this.isPointInPolygonRing(point, rings[i])) {
        return false;
      }
    }

    return true;
  }

  public isPointOnRingBoundary(point: L.LatLng, ring: Position[]): boolean {
    if (!point || !ring || ring.length < RouteCalculationService.MIN_POLYGON_RING_VERTICES) {
      return false;
    }

    const x = point.lng;
    const y = point.lat;

    for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
      const xi = ring[i][0];
      const yi = ring[i][1];
      const xj = ring[j][0];
      const yj = ring[j][1];

      const dx = xj - xi;
      const dy = yj - yi;
      const segmentLengthSq = dx * dx + dy * dy;

      if (segmentLengthSq <= RouteCalculationService.GEOMETRY_EPSILON_SQ) {
        if (Math.hypot(x - xi, y - yi) <= RouteCalculationService.GEOMETRY_EPSILON) {
          return true;
        }
        continue;
      }

      const crossProduct = (y - yi) * dx - (x - xi) * dy;
      if (crossProduct * crossProduct <= RouteCalculationService.GEOMETRY_EPSILON_SQ * segmentLengthSq) {
        const dotProduct = (x - xi) * dx + (y - yi) * dy;
        if (
          dotProduct >= -RouteCalculationService.GEOMETRY_EPSILON * segmentLengthSq &&
          dotProduct <= (1 + RouteCalculationService.GEOMETRY_EPSILON) * segmentLengthSq
        ) {
          return true;
        }
      }
    }

    return false;
  }

  public isPointInPolygonRing(point: L.LatLng, ring: Position[]): boolean {
    if (!point || !ring || ring.length < RouteCalculationService.MIN_POLYGON_RING_VERTICES) {
      return false;
    }

    let inside = false;
    const x = point.lng;
    const y = point.lat;

    for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
      const xi = ring[i][0];
      const yi = ring[i][1];
      const xj = ring[j][0];
      const yj = ring[j][1];

      const intersects =
        yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi;

      if (intersects) {
        inside = !inside;
      }
    }

    return inside;
  }
  public calculateRouteStats(coordinates: readonly L.LatLng[]): RouteStats {
    if (!coordinates || coordinates.length === 0) {
      return this.getEmptyStats();
    }

    const normalizedCoords = this.normalizeAltitudes(coordinates);

    const elevation = this.calculateElevation(normalizedCoords);
    const wkt = this.generateWkt(normalizedCoords);

    return {
      geometryWkt: wkt.geometryWkt,
      startPoint: wkt.startPoint,
      elevationGain: elevation.gain,
      elevationLoss: elevation.loss,
    };
  }

  private normalizeAltitudes(coordinates: readonly L.LatLng[]): L.LatLng[] {
    let lastKnownAlt = 0;

    return coordinates.map((pt) => {
      let currentAlt = pt.alt;

      if (currentAlt !== undefined && !Number.isNaN(currentAlt) && currentAlt !== 0) {
        lastKnownAlt = currentAlt;
      } else {
        currentAlt = lastKnownAlt;
      }


      return L.latLng(pt.lat, pt.lng, currentAlt);
    });
  }

  private calculateElevation(coordinates: readonly L.LatLng[]): { gain: number; loss: number } {
    let gain = 0;
    let loss = 0;

    for (let i = 1; i < coordinates.length; i++) {
      const prevAlt = coordinates[i - 1].alt ?? 0;
      const currAlt = coordinates[i].alt ?? 0;

      const diff = currAlt - prevAlt;

      if (diff > 0) {
        gain += diff;
      } else {
        loss -= diff;
      }
    }

    return { gain, loss };
  }

  private generateWkt(coordinates: readonly L.LatLng[]): {
    geometryWkt: string;
    startPoint: string;
  } {
    const validCoords = coordinates.length === 1 ? [coordinates[0], coordinates[0]] : coordinates;
    const wktPoints = validCoords.map((pt) => `${pt.lng} ${pt.lat} ${pt.alt ?? 0}`).join(', ');

    const start = validCoords[0];
    const startPoint = `POINT(${start.lng.toFixed(5)} ${start.lat.toFixed(5)})`;

    return {
      geometryWkt: `LINESTRING Z(${wktPoints})`,
      startPoint,
    };
  }

  private getEmptyStats(): RouteStats {
    return {
      geometryWkt: 'LINESTRING Z()',
      startPoint: '',
      elevationGain: 0,
      elevationLoss: 0,
    };
  }
}
