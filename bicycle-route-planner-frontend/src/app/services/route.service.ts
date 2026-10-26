import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
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
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/routes`;

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
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  public exportGpx(routeId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${routeId}/export/gpx`, {
      responseType: 'blob',
    });
  }

  public getElevationProfile(routeId: number): Observable<ElevationPointDto[]> {
    return this.http.get<ElevationPointDto[]>(`${this.apiUrl}/${routeId}/elevation`).pipe(
      catchError((err: HttpErrorResponse) => {
        this.logger.error('[RouteService] Error loading elevation profile', {
          routeId,
          error: err?.message ?? err,
        });

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

