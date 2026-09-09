import { Injectable, inject, signal } from '@angular/core';
import { AvoidanceZoneDto, CreateAvoidanceZoneRequestDto } from '../models/avoidance-zone.model';
import { AvoidanceZoneService } from './avoidance-zone.service';

@Injectable({
  providedIn: 'root',
})
export class AvoidanceZoneStateService {

  private readonly apiService = inject(AvoidanceZoneService);

  public zones = signal<AvoidanceZoneDto[]>([]);

  public fetchZones():void {
    this.apiService.getMyAvoidanceZones().subscribe({
      next: (fetchedZones) => {
        this.zones.set(fetchedZones);
      },
      error: (err) => console.error('Error occurred while fetching zones.', err)
    });
  }

  public saveZone(name: string, wktGeom: string): void {

    const newZone: CreateAvoidanceZoneRequestDto = {name: name, zoneGeom: wktGeom};

    this.apiService.saveAvoidanceZone(newZone).subscribe({
      next: (savedZone) => {
        this.zones.update((zones) => {
          return [...zones, savedZone];
        });
      },
      error: (err) => console.error('Error occurred while saving zone', err)
    });
  }

  public deleteZone(id: number): void {
    this.apiService.deleteAvoidanceZone(id).subscribe({
      next: () =>{
        this.zones.update((zones) => {
          return zones.filter((z) => z.id !== id);
        });
      },
      error: (err) => console.error('Error deleting zone', err)
    })
  }

  public toggleActive(zone: AvoidanceZoneDto): void {

    if(!zone.id) return;

    const newStatus = !zone.isActive;

    this.apiService.toggleAvoidanceZoneActiveStatus(zone.id, newStatus).subscribe({
      next: () => {
        this.zones.update((zones) => {
          return zones.map((z) => {
            if (z.id === zone.id) {
              return { ...z, isActive: newStatus };
            }
            return z;
          });
        });
      },
      error: (err) => console.error('Error toggling zone active status', err)
    });
  }





}
