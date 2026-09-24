import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { AvoidanceZoneDto} from '../../../models/avoidance-zone.model';

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
  public zone = input.required<AvoidanceZoneDto>();

  public toggleActive = output<AvoidanceZoneDto>();
  public deleteZone = output<number>();

  public onToggleClick(): void {
    this.toggleActive.emit(this.zone());
  }

  public onDeleteClick(): void {
    const id = this.zone().id;
    if (id !== undefined && id !== null) {
      this.deleteZone.emit(id);
    }
  }
}
