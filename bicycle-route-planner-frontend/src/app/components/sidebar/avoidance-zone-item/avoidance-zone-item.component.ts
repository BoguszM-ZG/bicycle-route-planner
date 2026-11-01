import { Component, input, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { AvoidanceZoneDto } from '../../../models/avoidance-zone.model';
import { AvoidanceZoneStateService } from '../../../services/avoidance-zone-state.service';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-avoidance-zone-item',
  imports: [TranslatePipe],
  templateUrl: './avoidance-zone-item.component.html',
  styleUrl: './avoidance-zone-item.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AvoidanceZoneItemComponent {
  private readonly zoneStateService = inject(AvoidanceZoneStateService);

  public readonly zone = input.required<AvoidanceZoneDto>();
  public readonly isConfirmingDelete = signal<boolean>(false);

  public toggleActive(): void {
    this.zoneStateService.toggleActive(this.zone());
  }

  public promptDelete(): void {
    this.isConfirmingDelete.set(true);
  }

  public cancelDelete(): void {
    this.isConfirmingDelete.set(false);
  }

  public confirmDelete(): void {
    this.zoneStateService.deleteZone(this.zone().id);
    this.isConfirmingDelete.set(false);
  }
}
