import { Injectable, inject}  from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment.development';
import {OrsGeoJsonResponse} from '../models/route.model';

@Injectable({
  providedIn: 'root',
})
export class OrsService {
  private readonly http = inject(HttpClient);

  public getRouteGeoJson(coords: number[][], avoidPolygons?: any): Observable<OrsGeoJsonResponse> {
    const body = {
      coordinates: coords,
      elevation: true,
      instructions: false,
      options: avoidPolygons ? { avoid_polygons: avoidPolygons } : undefined
    };

    return this.http.post<OrsGeoJsonResponse>(`${environment.apiUrl}/routing/directions`, body);
  }

  public getRoundTripGeoJson(startCoord: number[], distanceMeters: number, avoidPolygons?: any): Observable<OrsGeoJsonResponse> {
    const body = {
      coordinates: [startCoord],
      elevation: true,
      instructions: false,
      options: {
        round_trip: {
          length: distanceMeters
        },
        ...(avoidPolygons && { avoid_polygons: avoidPolygons })
      }
    };

    return this.http.post<OrsGeoJsonResponse>(`${environment.apiUrl}/routing/directions`, body);
  }

}
