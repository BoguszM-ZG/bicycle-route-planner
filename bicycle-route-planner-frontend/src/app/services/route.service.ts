import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable} from 'rxjs';
import {
  CreateRouteRequestDto,
  ElevationPointDto,
  RenameRouteRequestDto,
  RouteDetailDto,
  RouteSummaryDto,
  UpdateRouteFolderDto,
} from '../models/route.model';
import { environment } from '../../environments/environment.development';
import { NotificationService } from './notification.service';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class RouteService {
  private readonly apiUrl = `${environment.apiUrl}/routes`;
  private readonly http = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);
  private readonly logger = inject(LoggerService);

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

  public downloadGpx(routeId: number, routeName: string): void {
    this.http
      .get(`${this.apiUrl}/${routeId}/export/gpx`, { observe: 'response', responseType: 'blob' })
      .subscribe({
        next: (response) => {
          const polishMap: { [key: string]: string } = {
            'ą': 'a', 'ć': 'c', 'ę': 'e', 'ł': 'l', 'ń': 'n', 'ó': 'o', 'ś': 's', 'ź': 'z', 'ż': 'z',
            'Ą': 'A', 'Ć': 'C', 'Ę': 'E', 'Ł': 'L', 'Ń': 'N', 'Ó': 'O', 'Ś': 'S', 'Ź': 'Z', 'Ż': 'Z'
          };
          const asciiName = routeName.replace(/[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]/g, match => polishMap[match]);
          const safeName = asciiName.replace(/[^a-zA-Z0-9-]/g, '_');
          const fileName = `${safeName}.gpx`;

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
          this.logger.error('Error during downloading GPX file:', err);
          this.notificationService.error('Nie udało się pobrać pliku GPX');
        },
      });
  }

  public getElevationProfile(routeId: number): Observable<ElevationPointDto[]> {
    return this.http.get<ElevationPointDto[]>(`${this.apiUrl}/${routeId}/elevation`);
  }

  public updateRouteFolder(routeId: number, request: UpdateRouteFolderDto): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${routeId}/folder`, request);
  }

  public renameRoute(routeId: number, request: RenameRouteRequestDto): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${routeId}/name`, request);
  }
}
