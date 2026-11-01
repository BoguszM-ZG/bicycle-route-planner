import { Injectable, inject, signal, effect, untracked } from '@angular/core';
import { HttpStatusCode } from '@angular/common/http';
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
  private readonly _isSaving = signal<boolean>(false);
  private readonly _currentZoneDraft = signal<string | null>(null);

  public readonly zones = this._zones.asReadonly();
  public readonly isLoading = this._isLoading.asReadonly();
  public readonly isSaving = this._isSaving.asReadonly();
  public readonly currentZoneDraft = this._currentZoneDraft.asReadonly();

  constructor() {
    effect(() => {
      if (this.authService.isAuthenticated()) {
        untracked(() => this.fetchZones());
      } else {
        this.logger.debug(
          '[AvoidanceZoneStateService] User logged out, clearing AvoidanceZoneStateService',
        );
        untracked(() => this.clearState());
      }
    });
  }

  private clearState(): void {
    this.fetchZonesSubscription?.unsubscribe();
    this._zones.set([]);
    this._isLoading.set(false);
    this._isSaving.set(false);
    this._currentZoneDraft.set(null);
  }

  public setZoneDraft(wkt: string | null): void {
    this._currentZoneDraft.set(wkt);
  }

  private fetchZones(): void {
    this.fetchZonesSubscription?.unsubscribe();
    this._isLoading.set(true);

    this.fetchZonesSubscription = this.avoidanceZoneService
      .getMyAvoidanceZones()
      .pipe(finalize(() => this._isLoading.set(false)))
      .subscribe({
        next: (fetchedZones) => this._zones.set(fetchedZones),
        error: (err) => {
          this.logger.error('[AvoidanceZoneStateService] Error fetching zones', {
            status: err.status,
            type: err.error?.type,
          });
          this.notificationService.error('ERRORS.LOAD_ZONES_FAILED');
        },
      });
  }

  public saveZone(name: string, wktGeom: string, onSuccess?: () => void): void {
    if (this._isSaving()) {
      return;
    }

    this._isSaving.set(true);
    const newZone: CreateAvoidanceZoneRequestDto = { name, zoneGeom: wktGeom };

    this.avoidanceZoneService
      .saveAvoidanceZone(newZone)
      .pipe(finalize(() => this._isSaving.set(false)))
      .subscribe({
        next: (savedZone) => {
          this._isSaving.set(false);
          this.logger.info('[AvoidanceZoneStateService] Zone successfully saved', { name });
          this._zones.update((currentZones) => [...currentZones, savedZone]);
          this.notificationService.success('SUCCESS.SAVE_ZONE');
          onSuccess?.();
        },
        error: (err) => {
          if (err.status === HttpStatusCode.BadRequest || err.status === HttpStatusCode.Conflict) {
            this.logger.debug('[AvoidanceZoneStateService] Zone creation validation error', {
              status: err.status,
              type: err.error?.type,
            });
          } else {
            this.logger.error('[AvoidanceZoneStateService] Error saving zone', {
              status: err.status,
              type: err.error?.type,
            });
          }
          const errorType = typeof err.error?.type === 'string' ? err.error.type : '';
          this.notificationService.error(
            errorType.includes('avoidance-zone-limit-exceeded')
              ? 'ERRORS.ZONE_LIMIT_EXCEEDED'
              : 'ERRORS.SAVE_ZONE_FAILED',
          );
        },
      });
  }

  public deleteZone(id: number): void {
    this.avoidanceZoneService.deleteAvoidanceZone(id).subscribe({
      next: () => {
        this.logger.info('[AvoidanceZoneStateService] Zone successfully deleted', {
          deletedZoneId: id,
        });
        this._zones.update((currentZones) => currentZones.filter((z) => z.id !== id));
        this.notificationService.success('SUCCESS.DELETE_ZONE');
      },
      error: (err) => {
        if (err.status === HttpStatusCode.NotFound) {
          this.logger.debug('[AvoidanceZoneStateService] Zone to delete not found', { id });
        } else {
          this.logger.error('[AvoidanceZoneStateService] Error deleting zone', {
            status: err.status,
            type: err.error?.type,
          });
        }
        this.notificationService.error('ERRORS.DELETE_ZONE_FAILED');
      },
    });
  }

  public toggleActive(zone: AvoidanceZoneDto): void {
    const newStatus = !zone.isActive;

    this._zones.update((currentZones) =>
      currentZones.map((z) => (z.id === zone.id ? { ...z, isActive: newStatus } : z)),
    );

    this.avoidanceZoneService.toggleAvoidanceZoneActiveStatus(zone.id, newStatus).subscribe({
      next: () => {
        this.notificationService.success('SUCCESS.TOGGLE_ZONE');
        this.logger.info('[AvoidanceZoneStateService] Toggled zone activity successfully', {
          zoneId: zone.id,
          newActiveStatus: newStatus,
        });
      },
      error: (err) => {
        if (err.status === HttpStatusCode.BadRequest || err.status === HttpStatusCode.NotFound) {
          this.logger.debug('[AvoidanceZoneStateService] Zone toggle rejected', {
            zoneId: zone.id,
            status: err.status,
            type: err.error?.type,
          });
        } else {
          this.logger.error('[AvoidanceZoneStateService] Error toggling zone status', {
            status: err.status,
            type: err.error?.type,
          });
        }
        this._zones.update((currentZones) =>
          currentZones.map((z) => (z.id === zone.id ? { ...z, isActive: zone.isActive } : z)),
        );

        const errorType = typeof err.error?.type === 'string' ? err.error.type : '';
        this.notificationService.error(
          errorType.includes('avoidance-zone-limit-exceeded')
            ? 'ERRORS.ZONE_LIMIT_EXCEEDED'
            : 'ERRORS.TOGGLE_ZONE_FAILED',
        );
      },
    });
  }
}
