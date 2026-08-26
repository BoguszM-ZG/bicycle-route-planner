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
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: environment.orsApiKey,
    });

    const body = {
      coordinates: coords,
      elevation: true,
      instructions: false,
      options: avoidPolygons ? { avoid_polygons: avoidPolygons } : undefined
    };

    if (avoidPolygons) {
      body.options = {
        avoid_polygons: avoidPolygons,
      };
    }

    return this.http.post<OrsGeoJsonResponse>(environment.orsApiUrl, body, { headers });
  }
}
