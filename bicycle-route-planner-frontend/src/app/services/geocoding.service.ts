import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GeoResponse } from '../models/map-search.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class GeocodingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/geocoding/search`;

  public search(query: string, lat?: number, lon?: number): Observable<GeoResponse> {
    let params = new HttpParams().set('q', (query ?? '').trim());
    if (
      lat !== undefined &&
      lon !== undefined &&
      lat !== null &&
      lon !== null &&
      !Number.isNaN(lat) &&
      !Number.isNaN(lon)
    ) {
      params = params.set('lat', lat.toString()).set('lon', lon.toString());
    }
    return this.http.get<GeoResponse>(this.apiUrl, { params });
  }
}
