import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RoutePlanningStateService } from '../../../services/route-planning-state.service';
import { RoutingMode } from '../../../models/route.model';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-map-mode-panel',
  imports: [TranslatePipe],
  templateUrl: './map-mode-panel.component.html',
  styleUrl: './map-mode-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapModePanelComponent {

  private readonly routePlanningState = inject(RoutePlanningStateService);

  public readonly routingMode = this.routePlanningState.routingMode;
  public readonly roundTripDistanceKm = this.routePlanningState.roundTripDistanceKm;

  public setMode(mode: RoutingMode): void {
    this.routePlanningState.setRoutingMode(mode);
  }

  public cancelZone(): void {
    this.setMode('direct');
  }

  public setDistanceKm(km: number): void {
    this.routePlanningState.setRoundTripDistance(km * 1000);
  }
}

