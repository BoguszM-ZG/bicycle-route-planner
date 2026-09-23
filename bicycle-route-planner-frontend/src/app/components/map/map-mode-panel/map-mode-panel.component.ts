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
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapModePanelComponent {
  public readonly mapRoutingService = inject(MapRoutingService);
  public readonly mapDrawService = inject(MapDrawService);

  public setMode(mode: 'direct' | 'roundTrip' | 'avoidanceZone'): void {
    this.mapRoutingService.routingMode.set(mode);

    if (mode === 'avoidanceZone') {
      this.mapDrawService.startDrawingPolygon();
    } else {
      this.mapDrawService.cancelDrawing();
    }
  }

  public cancelZone(): void {
    this.mapDrawService.cancelDrawing();
    this.setMode('direct');
  }

  public saveZone(): void {
    this.mapDrawService.triggerSaveModal();
  }

  public onDistanceChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const val = Number(input.value);

    if (!Number.isNaN(val)) {
      this.mapRoutingService.roundTripDistanceMeters.set(val * 1000);
    }
  }
}
