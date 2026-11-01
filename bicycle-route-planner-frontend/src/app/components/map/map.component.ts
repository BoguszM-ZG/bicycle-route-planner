import {
  AfterViewInit,
  Component,
  inject,
  ChangeDetectionStrategy,
  viewChild,
  DestroyRef,
  effect,
  untracked,
} from '@angular/core';
import { NotificationService } from '../../services/notification.service';
import { TranslatePipe } from '@ngx-translate/core';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { SaveRouteModalComponent } from '../save-route-modal/save-route-modal.component';
import { SaveZoneModalComponent } from '../save-zone-modal/save-zone-modal.component';
import { MapControlsComponent } from '../map-controls/map-controls.component';
import { MapModePanelComponent } from './map-mode-panel/map-mode-panel.component';
import { MapSearchComponent } from '../map-search/map-search.component';
import { LoggerService } from '../../services/logger.service';
import { MapEngineService } from '../../services/map-engine.service';
import { RoutePlanningStateService } from '../../services/route-planning-state.service';
import { MapRouteLayerService } from '../../services/map-route-layer.service';
import { MapDrawService } from '../../services/map-draw.service';
import { RouteStateService } from '../../services/route-state.service';
import { AvoidanceZoneStateService } from '../../services/avoidance-zone-state.service';

@Component({
  selector: 'app-map',
  imports: [
    SidebarComponent,
    SaveRouteModalComponent,
    SaveZoneModalComponent,
    MapControlsComponent,
    MapModePanelComponent,
    MapSearchComponent,
    TranslatePipe,
  ],
  templateUrl: './map.component.html',
  styleUrl: './map.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapComponent implements AfterViewInit {
  private readonly mapEngine = inject(MapEngineService);
  private readonly routePlanningState = inject(RoutePlanningStateService);
  private readonly mapRouteLayer = inject(MapRouteLayerService);
  //dont delete
  private readonly mapDraw = inject(MapDrawService);
  private readonly routeState = inject(RouteStateService);
  private readonly avoidanceZoneStateService = inject(AvoidanceZoneStateService);
  private readonly notificationService = inject(NotificationService);
  private readonly logger = inject(LoggerService);
  private readonly destroyRef = inject(DestroyRef);

  public readonly saveRouteModal = viewChild.required<SaveRouteModalComponent>('saveRouteModal');
  public readonly saveZoneModal = viewChild.required<SaveZoneModalComponent>('saveZoneModal');
  public readonly isLocating = this.mapEngine.isLocating;

  constructor() {
    effect(() => {
      const draft = this.avoidanceZoneStateService.currentZoneDraft();
      if (!draft) {
        return;
      }
      const modal = this.saveZoneModal();
      if (!untracked(() => modal.isVisible())) {
        untracked(() => modal.open());
      }
    });
  }

  ngAfterViewInit(): void {
    this.mapEngine.initMap('map');

    this.destroyRef.onDestroy(() => {
      this.mapEngine.destroyMap();
    });
  }

  public clearRoute(): void {
    this.mapRouteLayer.clearRoute();
    this.routeState.clearSelectedRoute();
  }

  public saveRoute(): void {
    const draft = this.routePlanningState.currentRouteDraft();

    if (!draft?.distanceMeters) {
      this.logger.debug(
        '[Map] User tried to save a route, but no route is drawn or distance is zero',
      );
      this.notificationService.warning('MAP.DRAW_ROUTE_FIRST');
      return;
    }

    this.saveRouteModal().open(draft);
  }

  public locateMe(): void {
    this.mapEngine.locateUser();
  }
}
