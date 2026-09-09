import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import {
  CreateRouteRequestDto,
  ElevationPointDto,
  RouteDetailDto,
  RouteSummaryDto,
} from '../models/route.model';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root',
})
export class RouteService {
  private readonly apiUrl = `${environment.apiUrl}/routes`;
  private readonly http = inject(HttpClient);

  public routeAdded$ = new Subject<void>();

  getRoutes(): Observable<RouteSummaryDto[]> {
    return this.http.get<RouteSummaryDto[]>(this.apiUrl);
  }

  getRouteDetails(id: number): Observable<RouteDetailDto> {
    return this.http.get<RouteDetailDto>(`${this.apiUrl}/${id}`);
  }

  public addRoute(routeData: CreateRouteRequestDto): Observable<RouteDetailDto> {
    return this.http.post<RouteDetailDto>(this.apiUrl, routeData);
  }

  public deleteRoute(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { responseType: 'text' });
  }

  public downloadGpx(routeId: number): void {
    this.http
      .get(`${this.apiUrl}/${routeId}/export/gpx`, { observe: 'response', responseType: 'blob' })
      .subscribe({
        next: (response) => {
          const contentDisposition = response.headers.get('content-disposition');
          let fileName = `trasa_${routeId}.gpx`;

          const matches = /filename="?([^";]+)"?/.exec(contentDisposition!);
          fileName = matches?.[1] ?? fileName;

          const blobUrl = window.URL.createObjectURL(response.body!);
          const anchor = document.createElement('a');
          anchor.href = blobUrl;
          anchor.download = fileName;

          document.body.appendChild(anchor);
          anchor.click();
          anchor.remove();
          window.URL.revokeObjectURL(blobUrl);
        },
        error: (err) => {
          console.error('Error during downloading GPX file:', err);
          alert('Error downloading GPX file');
        },
      });
  }

  public getElevationProfile(routeId: number): Observable<ElevationPointDto[]> {
    return this.http.get<ElevationPointDto[]>(`${this.apiUrl}/${routeId}/elevation`);
  }
}
