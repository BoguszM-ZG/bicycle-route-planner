import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { MapRoutingService } from '../../../services/map-routing.service';

@Component({
  selector: 'app-map-mode-panel',
  imports: [],
  templateUrl: './map-mode-panel.component.html',
  styleUrl: './map-mode-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MapModePanelComponent {

  public readonly mapRoutingService = inject(MapRoutingService);

  public setMode(mode: 'direct' | 'roundTrip') {
    this.mapRoutingService.routingMode.set(mode);
  }

  public onDistanceChange(event: Event) {
    const val = (event.target as HTMLInputElement).value;
    this.mapRoutingService.roundTripDistanceMeters.set(Number(val) * 1000);
  }

}
