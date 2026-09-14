import {Injectable} from '@angular/core';
import * as L from 'leaflet';
import { RouteStats } from '../models/route.model';

@Injectable({
  providedIn: 'root',
})
export class RouteCalculationService {
  public calculateRouteStats(coordinates: L.LatLng[]): RouteStats {
    let lastKnownAlt: number | undefined = undefined;
    for (const pt of coordinates) {
      if (pt.alt !== undefined && !Number.isNaN(pt.alt) && pt.alt !== 0) {
        lastKnownAlt = pt.alt;
      } else if (lastKnownAlt !== undefined) {
        pt.alt = lastKnownAlt;
      }
    }

    const elevation = this.calculateElevation(coordinates);
    const wkt = this.generateWkt(coordinates);
    return {
      geometryWkt: wkt.geometryWkt,
      startPoint: wkt.startPoint,
      elevationGain: elevation.gain,
      elevationLoss: elevation.loss
    };
  }


  private calculateElevation(coordinates: L.LatLng[]): { gain: number; loss: number } {
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

  private generateWkt(coordinates: L.LatLng[]): {geometryWkt: string, startPoint: string} {
    const wktPoints = coordinates.map(
      pt => `${pt.lng} ${pt.lat} ${pt.alt ?? 0}`
    ).join(', ');

    let startPoint = '';

    if(coordinates.length > 0) {
      const start = coordinates[0];
      startPoint = `POINT(${start.lng.toFixed(5)} ${start.lat.toFixed(5)})`
    }

    return {
      geometryWkt: `LINESTRING Z(${wktPoints})`,
      startPoint
    };
  }


}
