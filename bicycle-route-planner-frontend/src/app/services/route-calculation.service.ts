import { Injectable } from '@angular/core';
import * as L from 'leaflet';
import { RouteStats } from '../models/route.model';

@Injectable({
  providedIn: 'root',
})
export class RouteCalculationService {
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
