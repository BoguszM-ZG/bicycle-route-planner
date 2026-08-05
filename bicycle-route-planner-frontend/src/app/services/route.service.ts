import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RouteDetailDto } from '../models/route.model';
import { environment } from '../../environments/environment.development';

@Injectable({
  providedIn: 'root',
})
export class RouteService {
  private apiUrl = `${environment.apiUrl}/routes`;

  constructor(private http: HttpClient) {}

  getRoutes(): Observable<RouteDetailDto[]> {
    return this.http.get<RouteDetailDto[]>(this.apiUrl);
  }

  public addRoute(routeData: RouteDetailDto) : Observable<any> {
    return this.http.post<any>(this.apiUrl, routeData);
  }
}
