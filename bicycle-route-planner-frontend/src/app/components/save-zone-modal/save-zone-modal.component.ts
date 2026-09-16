import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { AvoidanceZoneStateService } from '../../services/avoidance-zone-state.service';
import { MapDrawService } from '../../services/map-draw.service';
import { MapRoutingService } from '../../services/map-routing.service';

@Component({
  selector: 'app-save-zone-modal',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './save-zone-modal.component.html',
  styleUrl: './save-zone-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SaveZoneModalComponent {
  private readonly zoneStateService = inject(AvoidanceZoneStateService);
  private readonly mapDrawService = inject(MapDrawService);
  private readonly routingService = inject(MapRoutingService);

  public isVisible = signal<boolean>(false);
  public isLoading = this.zoneStateService.isLoading;

  public zoneNameControl = new FormControl('Nowa strefa omijania', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/\S/)]
  });

  private wktStringToSave: string | null = null;

  public open(wktString: string): void {
    this.wktStringToSave = wktString;
    this.zoneNameControl.setValue('Nowa strefa omijania');
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
      alert('Proszę wpisać prawidłową nazwę strefy');
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
