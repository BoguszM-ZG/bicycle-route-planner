import { Injectable, inject, signal } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { AvoidanceZoneDto, CreateAvoidanceZoneRequestDto } from '../models/avoidance-zone.model';
import { AvoidanceZoneService } from './avoidance-zone.service';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class AvoidanceZoneStateService {
  private readonly apiService = inject(AvoidanceZoneService);
  private readonly logger = inject(LoggerService);


  private readonly _zones = signal<AvoidanceZoneDto[]>([]);
  private readonly _isLoading = signal<boolean>(false);
  private readonly _error = signal<string | null>(null);


  public readonly zones = this._zones.asReadonly();
  public readonly isLoading = this._isLoading.asReadonly();
  public readonly error = this._error.asReadonly();


  public fetchZones(): void {
    this._isLoading.set(true);
    this._error.set(null);

    this.apiService.getMyAvoidanceZones()
      .pipe(finalize(() => this._isLoading.set(false)))
      .subscribe({
        next: (fetchedZones) => this._zones.set(fetchedZones),
        error: (err) => {
          this.logger.error('Error fetching zones', err);
          this._error.set('Failed to load avoidance zones.');
        }
      });
  }

  public saveZone(name: string, wktGeom: string): void {
    this._isLoading.set(true);
    this._error.set(null);

    const newZone: CreateAvoidanceZoneRequestDto = { name, zoneGeom: wktGeom };

    this.apiService.saveAvoidanceZone(newZone)
      .pipe(finalize(() => this._isLoading.set(false)))
      .subscribe({
        next: (savedZone) => {
          this._zones.update((currentZones) => [...currentZones, savedZone]);
        },
        error: (err) => {
          this.logger.error('Error saving zone', err);
          this._error.set('Failed to save the avoidance zone.');
        }
      });
  }

  public deleteZone(id: number): void {
    this._isLoading.set(true);
    this._error.set(null);

    this.apiService.deleteAvoidanceZone(id)
      .pipe(finalize(() => this._isLoading.set(false)))
      .subscribe({
        next: () => {
          this._zones.update((currentZones) => currentZones.filter((z) => z.id !== id));
        },
        error: (err) => {
          this.logger.error('Error deleting zone', err);
          this._error.set('Failed to delete the avoidance zone.');
        }
      });
  }

  public toggleActive(zone: AvoidanceZoneDto): void {
    if (!zone.id) return;

    this._isLoading.set(true);
    this._error.set(null);
    const newStatus = !zone.isActive;

    this.apiService.toggleAvoidanceZoneActiveStatus(zone.id, newStatus)
      .pipe(finalize(() => this._isLoading.set(false)))
      .subscribe({
        next: () => {
          this._zones.update((currentZones) =>
            currentZones.map((z) => (z.id === zone.id ? { ...z, isActive: newStatus } : z))
          );
        },
        error: (err) => {
          this.logger.error('Error toggling zone status', err);
          this._error.set('Failed to toggle zone status.');
        }
      });
  }
}
