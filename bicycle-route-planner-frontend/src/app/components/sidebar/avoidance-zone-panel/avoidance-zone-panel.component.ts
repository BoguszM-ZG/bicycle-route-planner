import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { AvoidanceZoneItemComponent } from '../avoidance-zone-item/avoidance-zone-item.component';
import { AvoidanceZoneStateService } from '../../../services/avoidance-zone-state.service';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-avoidance-zone-panel',
  standalone: true,
  imports: [AvoidanceZoneItemComponent, TranslatePipe],
  templateUrl: './avoidance-zone-panel.component.html',
  styleUrl: './avoidance-zone-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AvoidanceZonePanelComponent {
  private readonly zoneStateService = inject(AvoidanceZoneStateService);

  public readonly zones = this.zoneStateService.zones;
  public readonly isLoading = this.zoneStateService.isLoading;
}

