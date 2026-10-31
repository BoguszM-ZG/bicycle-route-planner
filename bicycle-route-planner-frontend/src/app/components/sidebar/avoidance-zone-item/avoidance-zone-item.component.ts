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

  public onToggleClick(): void {
    this.zoneStateService.toggleActive(this.zone());
  }

  public promptDelete(event: Event): void {
    event.stopPropagation();
    this.isConfirmingDelete.set(true);
  }

  public cancelDelete(event: Event): void {
    event.stopPropagation();
    this.isConfirmingDelete.set(false);
  }

  public confirmDelete(event: Event): void {
    event.stopPropagation();
    const id = this.zone().id;
    if (id !== undefined && id !== null) {
      this.zoneStateService.deleteZone(id);
    }
    this.isConfirmingDelete.set(false);
  }
}
