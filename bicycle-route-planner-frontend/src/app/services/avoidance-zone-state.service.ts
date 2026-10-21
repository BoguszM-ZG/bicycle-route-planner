import { Injectable, inject, signal, effect, untracked } from '@angular/core';
import { Subscription } from 'rxjs';
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

  private fetchZonesSubscription?: Subscription;

  private readonly _zones = signal<AvoidanceZoneDto[]>([]);
  private readonly _isLoading = signal<boolean>(false);

  public readonly zones = this._zones.asReadonly();
  public readonly isLoading = this._isLoading.asReadonly();

  constructor() {
    effect(() => {
      const user = this.authService.currentUser();
      if (user) {
        untracked(() => this.fetchZones());
      } else {
        this.logger.debug('[AvoidanceZoneStateService] User logged out, clearing AvoidanceZoneStateService');
        untracked(() => this.clearState());
      }
    });
  }

  private clearState(): void {
    this.fetchZonesSubscription?.unsubscribe();
    this._zones.set([]);
    this._isLoading.set(false);
  }

  public fetchZones(): void {
    this.fetchZonesSubscription?.unsubscribe();
    this._isLoading.set(true);

    this.fetchZonesSubscription = this.avoidanceZoneService
      .getMyAvoidanceZones()
      .pipe(finalize(() => this._isLoading.set(false)))
      .subscribe({
        next: (fetchedZones) => this._zones.set(fetchedZones),
        error: (err) => {
          this.logger.error('[AvoidanceZoneStateService] Error fetching zones', { error: err });
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
          this.logger.info('[AvoidanceZoneStateService] Zone successfully saved', { name });
          this._zones.update((currentZones) => [...currentZones, savedZone]);
        },
        error: (err) => {
          if (err.status === 400 || err.status === 409) {
            this.logger.debug('[AvoidanceZoneStateService] Zone creation validation error:', err.error);
          } else {
            this.logger.error('[AvoidanceZoneStateService] Error saving zone:', err);
          }
          this.notificationService.error('ERRORS.SAVE_ZONE_FAILED');
        },
      });
  }

  public deleteZone(id: number): void {
    this.avoidanceZoneService
      .deleteAvoidanceZone(id)
      .subscribe({
        next: () => {
          this.logger.info('[AvoidanceZoneStateService] Zone successfully deleted', {
            deletedZoneId: id,
          });
          this._zones.update((currentZones) => currentZones.filter((z) => z.id !== id));
        },
        error: (err) => {
          if (err.status === 404) {
            this.logger.debug('[AvoidanceZoneStateService] Zone to delete not found:', { id });
          } else {
            this.logger.error('[AvoidanceZoneStateService] Error deleting zone:', err);
          }
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
        this.logger.info('[AvoidanceZoneStateService] Toggled zone activity successfully.', {
          zoneId: zone.id,
          newActiveStatus: newStatus,
        });
      },
      error: (err) => {
        if (err.status === 404) {
          this.logger.debug('[AvoidanceZoneStateService] Zone to toggle not found:', { zoneId: zone.id });
        } else {
          this.logger.error('[AvoidanceZoneStateService] Error toggling zone status', err);
        }
        this._zones.update((currentZones) =>
          currentZones.map((z) => (z.id === zone.id ? { ...z, isActive: zone.isActive } : z)),
        );

        this.notificationService.error('ERRORS.TOGGLE_ZONE_FAILED');
      },
    });
  }
}
