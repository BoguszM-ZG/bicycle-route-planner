import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GeoResponse, GeocodingSearchParams } from '../models/map-search.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class GeocodingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/geocoding/search`;

  public search(params: GeocodingSearchParams): Observable<GeoResponse> {
    let httpParams = new HttpParams().set('q', (params.query ?? '').trim());
    if (Number.isFinite(params.lat) && Number.isFinite(params.lon)) {
      httpParams = httpParams.set('lat', params.lat!.toString()).set('lon', params.lon!.toString());
    }
    return this.http.get<GeoResponse>(this.apiUrl, { params: httpParams });
  }
}
