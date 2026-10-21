import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import {
  CreateRouteRequestDto,
  ElevationPointDto,
  RenameRouteRequestDto,
  RouteDetailDto,
  RouteSummaryDto,
  UpdateRouteFolderDto,
} from '../models/route.model';
import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class RouteService {
  private readonly apiUrl = `${environment.apiUrl}/routes`;
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);

  public getRoutes(): Observable<RouteSummaryDto[]> {
    return this.http.get<RouteSummaryDto[]>(this.apiUrl);
  }

  public getRouteDetails(id: number): Observable<RouteDetailDto> {
    return this.http.get<RouteDetailDto>(`${this.apiUrl}/${id}`);
  }

  public addRoute(routeData: CreateRouteRequestDto): Observable<RouteDetailDto> {
    return this.http.post<RouteDetailDto>(this.apiUrl, routeData);
  }

  public deleteRoute(id: number): Observable<void> {
    return this.http.delete(`${this.apiUrl}/${id}`, { responseType: 'text' }).pipe(map(() => void 0));
  }

  public exportGpx(routeId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${routeId}/export/gpx`, {
      responseType: 'blob',
    });
  }

  public getElevationProfile(routeId: number): Observable<ElevationPointDto[]> {
    return this.http.get<ElevationPointDto[]>(`${this.apiUrl}/${routeId}/elevation`).pipe(
      catchError((err) => {

        this.logger.error(
          `[RouteService] Error loading elevation profile for route ID: ${routeId}`,
          err,
        );

        return throwError(() => err);
      }),
    );
  }

  public updateRouteFolder(routeId: number, request: UpdateRouteFolderDto): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${routeId}/folder`, request);
  }

  public renameRoute(routeId: number, request: RenameRouteRequestDto): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${routeId}/name`, request);
  }
}
