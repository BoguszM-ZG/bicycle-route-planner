import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RoutePlanningStateService } from '../../../services/route-planning-state.service';
import { MapDrawService } from '../../../services/map-draw.service';
import { MapRouteLayerService } from '../../../services/map-route-layer.service';
import { RouteStateService } from '../../../services/route-state.service';
import { RoutingMode } from '../../../models/route.model';
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
  private readonly routePlanningStateService = inject(RoutePlanningStateService);
  private readonly mapDrawService = inject(MapDrawService);
  private readonly mapRouteLayerService = inject(MapRouteLayerService);
  private readonly routeStateService = inject(RouteStateService);

  public routingMode = this.routePlanningStateService.routingMode;
  public roundTripDistanceKm = this.routePlanningStateService.roundTripDistanceKm;
  public currentZoneDraft = this.mapDrawService.currentZoneDraft;

  public setMode(mode: RoutingMode): void {
    const currentMode = this.routingMode();
    if (currentMode === mode) {
      return;
    }

    if (this.shouldClearRoute(currentMode, mode)) {
      this.mapRouteLayerService.clearRoute();
      if (this.routeStateService.selectedRoute()) {
        this.routeStateService.clearSelectedRoute();
      }
    }

    this.routePlanningStateService.routingMode.set(mode);

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
      this.routePlanningStateService.roundTripDistanceMeters.set(val * 1000);
    }
  }

  private shouldClearRoute(currentMode: RoutingMode, newMode: RoutingMode): boolean {
    const isDirectAvoidanceTransition =
      (currentMode === 'direct' && newMode === 'avoidanceZone') ||
      (currentMode === 'avoidanceZone' && newMode === 'direct');

    return !isDirectAvoidanceTransition;
  }
}
