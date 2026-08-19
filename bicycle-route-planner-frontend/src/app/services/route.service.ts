import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { RouteDetailDto } from '../models/route.model';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root',
})
export class RouteService {
  private readonly apiUrl = `${environment.apiUrl}/routes`;
  private readonly http = inject(HttpClient);

  public routeAdded$ = new Subject<void>();



  getRoutes(): Observable<RouteDetailDto[]> {
    return this.http.get<RouteDetailDto[]>(this.apiUrl);
  }

  public addRoute(routeData: RouteDetailDto) : Observable<any> {
    return this.http.post<any>(this.apiUrl, routeData);
  }


  public deleteRoute(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { responseType: 'text' });
  }
}
