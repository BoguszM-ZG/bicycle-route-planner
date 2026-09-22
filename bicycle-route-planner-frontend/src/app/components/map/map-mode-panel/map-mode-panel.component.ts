import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { MapRoutingService } from '../../../services/map-routing.service';
import { MapDrawService } from '../../../services/map-draw.service';

import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-map-mode-panel',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './map-mode-panel.component.html',
  styleUrl: './map-mode-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MapModePanelComponent {

  public readonly mapRoutingService = inject(MapRoutingService);
  public readonly mapDrawService = inject(MapDrawService);

  public setMode(mode: 'direct' | 'roundTrip' | 'avoidanceZone') {
    this.mapRoutingService.routingMode.set(mode);

    if (mode === 'avoidanceZone') {
      this.mapDrawService.startDrawingPolygon();
    } else {
      this.mapDrawService.cancelDrawing();
    }
  }

  public cancelZone() {
    this.mapDrawService.cancelDrawing();
    this.setMode('direct');
  }

  public saveZone() {
    this.mapDrawService.triggerSaveModal();
  }

  public onDistanceChange(event: Event) {
    const val = (event.target as HTMLInputElement).value;
    this.mapRoutingService.roundTripDistanceMeters.set(Number(val) * 1000);
  }

}
