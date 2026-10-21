import { Component, input, inject, ChangeDetectionStrategy } from '@angular/core';
import { AvoidanceZoneDto } from '../../../models/avoidance-zone.model';
import { AvoidanceZoneStateService } from '../../../services/avoidance-zone-state.service';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-avoidance-zone-item',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './avoidance-zone-item.component.html',
  styleUrl: './avoidance-zone-item.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AvoidanceZoneItemComponent {
  private readonly zoneStateService = inject(AvoidanceZoneStateService);

  public readonly zone = input.required<AvoidanceZoneDto>();

  public onToggleClick(): void {
    this.zoneStateService.toggleActive(this.zone());
  }

  public onDeleteClick(): void {
    const id = this.zone().id;
    if (id !== undefined && id !== null) {
      this.zoneStateService.deleteZone(id);
    }
  }
}
