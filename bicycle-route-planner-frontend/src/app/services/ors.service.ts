import { Injectable, inject}  from '@angular/core';
import { HttpClient} from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AvoidPolygonsGeoJson, OrsGeoJsonResponse } from '../models/route.model';

@Injectable({
  providedIn: 'root',
})
export class OrsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/routing/directions`;

  public getRouteGeoJson(coords: number[][], avoidPolygons?: AvoidPolygonsGeoJson): Observable<OrsGeoJsonResponse> {
    const body = {
      coordinates: coords,
      elevation: true,
      instructions: false,
      options: avoidPolygons ? { avoid_polygons: avoidPolygons } : undefined
    };

    return this.http.post<OrsGeoJsonResponse>(this.apiUrl, body);
  }

  public getRoundTripGeoJson(startCoord: number[], distanceMeters: number, avoidPolygons?: AvoidPolygonsGeoJson): Observable<OrsGeoJsonResponse> {
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

    return this.http.post<OrsGeoJsonResponse>(this.apiUrl, body);
  }

}
