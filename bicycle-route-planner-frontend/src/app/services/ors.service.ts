import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { Position } from 'geojson';
import { environment } from '../../environments/environment';
import {
  AvoidPolygonsGeoJson,
  OrsGeoJsonResponse,
  OrsRouteRequestDto,
  RoundTripParams,
} from '../models/ors.model';

@Injectable({
  providedIn: 'root',
})
export class OrsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/routing/directions`;

  public getRouteGeoJson(coords: Position[], avoidPolygons?: AvoidPolygonsGeoJson): Observable<OrsGeoJsonResponse> {
    const body: OrsRouteRequestDto = {
      coordinates: coords,
      elevation: true,
      instructions: false,
      options: avoidPolygons ? { avoid_polygons: avoidPolygons } : undefined,
    };

    return this.http.post<OrsGeoJsonResponse>(this.apiUrl, body);
  }

  public getRoundTripGeoJson(params: RoundTripParams): Observable<OrsGeoJsonResponse> {
    const body: OrsRouteRequestDto = {
      coordinates: [params.startCoord],
      elevation: true,
      instructions: false,
      options: {
        round_trip: {
          length: params.distanceMeters,
          ...(params.seed !== undefined && { seed: params.seed }),
        },
        ...(params.avoidPolygons && { avoid_polygons: params.avoidPolygons }),
      },
    };

    return this.http.post<OrsGeoJsonResponse>(this.apiUrl, body);
  }
}
