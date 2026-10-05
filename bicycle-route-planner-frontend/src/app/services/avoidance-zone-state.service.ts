import { Injectable, inject, signal, effect } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { AvoidanceZoneDto, CreateAvoidanceZoneRequestDto } from '../models/avoidance-zone.model';
import { AvoidanceZoneService } from './avoidance-zone.service';
import { LoggerService } from './logger.service';
import { AuthService } from './auth.service';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root',
})
export class AvoidanceZoneStateService {
  private readonly avoidanceZoneService = inject(AvoidanceZoneService);
  private readonly logger = inject(LoggerService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  private readonly _zones = signal<AvoidanceZoneDto[]>([]);
  private readonly _isLoading = signal<boolean>(false);

  public readonly zones = this._zones.asReadonly();
  public readonly isLoading = this._isLoading.asReadonly();

  constructor() {
    effect(() => {
      const user = this.authService.currentUser();
      if (!user) {
        this.logger.debug('User logged out, clearing AvoidanceZoneStateService');
        this.clearState();
      }
    });
  }

  private clearState(): void {
    this._zones.set([]);
    this._isLoading.set(false);
  }

  public fetchZones(): void {
    this._isLoading.set(true);

    this.avoidanceZoneService
      .getMyAvoidanceZones()
      .pipe(finalize(() => this._isLoading.set(false)))
      .subscribe({
        next: (fetchedZones) => this._zones.set(fetchedZones),
        error: (err) => {
          this.logger.error('Error fetching zones', err);
          this.notificationService.error('ERRORS.LOAD_ZONES_FAILED');
        },
      });
  }

  public saveZone(name: string, wktGeom: string): void {

    const newZone: CreateAvoidanceZoneRequestDto = { name, zoneGeom: wktGeom };

    this.avoidanceZoneService
      .saveAvoidanceZone(newZone)
      .subscribe({
        next: (savedZone) => {
          this.logger.info(`[AvoidanceZone] Zone successfully saved: ${name}`);
          this._zones.update((currentZones) => [...currentZones, savedZone]);
        },
        error: (err) => {
          this.logger.error('Error saving zone', err);
          this.notificationService.error('ERRORS.SAVE_ZONE_FAILED');
        },
      });
  }

  public deleteZone(id: number): void {

    this.avoidanceZoneService
      .deleteAvoidanceZone(id)
      .subscribe({
        next: () => {
          this.logger.info(
            '[AvoidanceZone] Zone successfully deleted.',
            { deletedZoneId: id },
          );
          this._zones.update((currentZones) => currentZones.filter((z) => z.id !== id));
        },
        error: (err) => {
          this.logger.error('Error deleting zone', err);
          this.notificationService.error('ERRORS.DELETE_ZONE_FAILED');
        },
      });
  }

  public toggleActive(zone: AvoidanceZoneDto): void {
    if (!zone.id) return;

    const newStatus = !zone.isActive;


    this._zones.update((currentZones) =>
      currentZones.map((z) => (z.id === zone.id ? { ...z, isActive: newStatus } : z)),
    );

    this.avoidanceZoneService.toggleAvoidanceZoneActiveStatus(zone.id, newStatus).subscribe({
      next: () => {
        this.logger.info('[AvoidanceZone] Toggled zone activity successfully.', {
          zoneId: zone.id,
          newActiveStatus: newStatus,
        });
      },
      error: (err) => {
        this.logger.error('Error toggling zone status', err);
        this._zones.update((currentZones) =>
          currentZones.map((z) => (z.id === zone.id ? { ...z, isActive: zone.isActive } : z)),
        );

        this.notificationService.error('ERRORS.TOGGLE_ZONE_FAILED');
        },
    });
  }
}
