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
import { MapDrawService } from '../../services/map-draw.service';
import { MapRoutingService } from '../../services/map-routing.service';
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
  private readonly mapDrawService = inject(MapDrawService);
  private readonly routingService = inject(MapRoutingService);
  private readonly notificationService = inject(NotificationService);
  private readonly translate = inject(TranslateService);
  private readonly logger = inject(LoggerService);

  public isVisible = signal<boolean>(false);
  public isLoading = this.zoneStateService.isLoading;

  public zoneInput = viewChild<ElementRef<HTMLInputElement>>('zoneInput');

  public zoneNameControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/\S/),
      Validators.minLength(1), Validators.maxLength(100)],
  });

  private wktStringToSave: string | null = null;

  constructor() {
    effect(() => {
      if (this.isVisible()) {
        setTimeout(() => this.zoneInput()?.nativeElement.focus(), 0);
      }
    });
  }

  public open(wktString: string): void {
    this.wktStringToSave = wktString;
    this.zoneNameControl.setValue(this.translate.instant('MODALS.DEFAULT_ZONE_NAME'));
    this.isVisible.set(true);
  }

  public close(): void {
    this.isVisible.set(false);
    this.zoneNameControl.reset();
    this.wktStringToSave = null;
    this.mapDrawService.cancelDrawing();
    this.routingService.routingMode.set('direct');
  }

  public saveZone(): void {
    if (this.zoneNameControl.invalid) {
      this.zoneNameControl.markAsTouched();
      this.logger.warn(
        '[SaveZoneModal] Attempt to save an invalid form',
        this.zoneNameControl.errors,
      );
      this.notificationService.warning(this.translate.instant('MODALS.EMPTY_ZONE_NAME'));
      return;
    }
    if (!this.wktStringToSave || this.isLoading()) {
      return;
    }

    const name = this.zoneNameControl.value.trim();
    this.zoneStateService.saveZone(name, this.wktStringToSave);
    this.mapDrawService.cancelDrawing();
    this.close();
  }
}
