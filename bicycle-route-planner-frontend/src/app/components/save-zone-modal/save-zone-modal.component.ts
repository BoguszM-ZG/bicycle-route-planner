import {
  Component,
  inject,
  signal,
  ChangeDetectionStrategy,
  ElementRef,
  effect,
  viewChild,
} from '@angular/core';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { NotificationService } from '../../services/notification.service';
import { AvoidanceZoneStateService } from '../../services/avoidance-zone-state.service';
import { RoutePlanningStateService } from '../../services/route-planning-state.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-save-zone-modal',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './save-zone-modal.component.html',
  styleUrl: './save-zone-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SaveZoneModalComponent {
  private readonly zoneStateService = inject(AvoidanceZoneStateService);
  private readonly routePlanningState = inject(RoutePlanningStateService);
  private readonly notificationService = inject(NotificationService);
  private readonly translate = inject(TranslateService);
  private readonly logger = inject(LoggerService);

  public readonly isVisible = signal<boolean>(false);
  public readonly isLoading = this.zoneStateService.isSaving;

  public readonly zoneInput = viewChild<ElementRef<HTMLInputElement>>('zoneInput');

  public readonly zoneNameControl = new FormControl('', {
    nonNullable: true,
    validators: [
      Validators.required,
      Validators.pattern(/\S/),
      Validators.minLength(1),
      Validators.maxLength(100),
    ],
  });

  private wktStringToSave: string | null = null;

  constructor() {
    effect(() => {
      if (this.isVisible()) {
        setTimeout(() => this.zoneInput()?.nativeElement.focus(), 0);
      }
    });
  }

  public open(wktString?: string): void {
    this.wktStringToSave = wktString ?? this.zoneStateService.currentZoneDraft();
    this.zoneNameControl.setValue(this.translate.instant('MODALS.DEFAULT_ZONE_NAME'));
    this.isVisible.set(true);
  }

  public close(): void {
    if (this.isLoading()) {
      return;
    }
    this.isVisible.set(false);
    this.zoneNameControl.reset();
    this.wktStringToSave = null;
    this.zoneStateService.currentZoneDraft.set(null);
    this.routePlanningState.setRoutingMode('direct');
  }

  public saveZone(): void {
    if (this.zoneNameControl.invalid) {
      this.zoneNameControl.markAsTouched();
      this.logger.debug(
        '[SaveZoneModal] Attempt to save an invalid form',
        this.zoneNameControl.errors,
      );
      this.notificationService.warning('MODALS.EMPTY_ZONE_NAME');
      return;
    }

    const wkt = this.zoneStateService.currentZoneDraft() ?? this.wktStringToSave;
    if (!wkt || this.isLoading()) {
      return;
    }

    const name = this.zoneNameControl.value.trim();
    this.zoneStateService.saveZone(name, wkt, () => this.close());
  }
}

