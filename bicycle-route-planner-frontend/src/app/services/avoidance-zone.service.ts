import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable} from 'rxjs';
import { environment} from '../../environments/environment';
import {
  AvoidanceZoneDto,
  CreateAvoidanceZoneRequestDto,
  UpdateAvoidanceZoneStatusDto,
} from '../models/avoidance-zone.model';

@Injectable({
  providedIn: 'root',
})
export class AvoidanceZoneService {
  private readonly apiUrl = `${environment.apiUrl}/avoidance-zones`;
  private readonly http = inject(HttpClient);

  public getMyAvoidanceZones(): Observable<AvoidanceZoneDto[]> {
    return this.http.get<AvoidanceZoneDto[]>(this.apiUrl);
  }

  public saveAvoidanceZone(zone: CreateAvoidanceZoneRequestDto): Observable<AvoidanceZoneDto> {
    return this.http.post<AvoidanceZoneDto>(this.apiUrl, zone);
  }

  public deleteAvoidanceZone(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  public toggleAvoidanceZoneActiveStatus(id: number, isActive: boolean): Observable<void> {
    const payload: UpdateAvoidanceZoneStatusDto = { isActive };
    return this.http.patch<void>(`${this.apiUrl}/${id}/active`, payload);
  }
}
