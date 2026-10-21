import {
  AfterViewInit,
  Component,
  inject,
  ChangeDetectionStrategy,
  viewChild,
  NgZone,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import * as L from 'leaflet';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { MapRenderService } from '../../services/map-render.service';
import { SaveRouteModalComponent } from '../save-route-modal/save-route-modal.component';
import { SaveZoneModalComponent } from '../save-zone-modal/save-zone-modal.component';
import { MapControlsComponent } from '../map-controls/map-controls.component';
import { MapRouteLayerService } from '../../services/map-route-layer.service';
import { RoutePlanningStateService } from '../../services/route-planning-state.service';
import { RouteStateService } from '../../services/route-state.service';
import { MapDrawService } from '../../services/map-draw.service';
import { MapModePanelComponent } from './map-mode-panel/map-mode-panel.component';
import { MapSearchComponent } from '../map-search/map-search.component';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-map',
  standalone: true,
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
  private map!: L.Map;

  private readonly authService = inject(AuthService);
  private readonly mapRenderService = inject(MapRenderService);
  private readonly routeStateService = inject(RouteStateService);
  public readonly routePlanningStateService = inject(RoutePlanningStateService);
  public readonly mapRoutingService = inject(MapRouteLayerService);
  private readonly mapDrawService = inject(MapDrawService);
  private readonly notificationService = inject(NotificationService);
  private readonly logger = inject(LoggerService);
  private readonly translate = inject(TranslateService);
  private readonly ngZone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);

  public saveRouteModal = viewChild.required<SaveRouteModalComponent>('saveRouteModal');
  public saveZoneModal = viewChild.required<SaveZoneModalComponent>('saveZoneModal');

  ngAfterViewInit(): void {
    this.logger.info('[Map] Leaflet map successfully initialized');

    this.mapRoutingService.setWaypointPopupFactory((marker: L.Marker) =>
      this.createWaypointPopup(marker),
    );
    this.mapRoutingService.setWaypointTooltipFactory((marker: L.Marker) =>
      this.createWaypointTooltip(marker),
    );

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.mapRoutingService.updateWaypointPopups();
        this.mapRoutingService.updateWaypointTooltips();
      });

    this.ngZone.runOutsideAngular(() => {
      this.mapRenderService.initMap('map');
      this.map = this.mapRenderService.getMap()!;
      this.mapRoutingService.initRouting(this.map);
      this.mapDrawService.init(this.map);
    });

    this.destroyRef.onDestroy(() => {
      this.mapRoutingService.setWaypointPopupFactory(null);
      this.mapRoutingService.setWaypointTooltipFactory(null);
      this.mapRoutingService.destroyRouting();
      this.mapDrawService.destroy();
      this.mapRenderService.destroyMap();
    });

    this.mapDrawService.zoneDrawn$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((wktString) => {
        this.ngZone.run(() => {
          this.saveZoneModal().open(wktString);
        });
      });
  }

  public createWaypointPopup(marker: L.Marker): HTMLElement {
    const container = document.createElement('div');
    container.className = 'waypoint-popup-container';
    L.DomEvent.disableClickPropagation(container);
    L.DomEvent.disableScrollPropagation(container);

    const title = document.createElement('span');
    title.className = 'waypoint-popup-title';
    title.textContent = this.translate.instant('MAP.WAYPOINT_POPUP_TITLE');
    container.appendChild(title);

    const deleteButton = document.createElement('button');
    deleteButton.type = 'button';
    deleteButton.className = 'waypoint-popup-delete-btn';
    deleteButton.textContent = this.translate.instant('MAP.DELETE_WAYPOINT');

    deleteButton.addEventListener('click', (event: MouseEvent) => {
      event.preventDefault();
      event.stopPropagation();
      this.logger.debug('[Map] User clicked delete waypoint button');
      this.ngZone.run(() => {
        this.mapRoutingService.deleteWaypoint(marker);
      });
    });

    container.appendChild(deleteButton);
    return container;
  }

  public createWaypointTooltip(_marker?: L.Marker): string {
    return this.translate.instant('MAP.WAYPOINT_TOOLTIP');
  }

  public clearRoute(): void {
    this.mapRoutingService.clearRoute();
    this.routeStateService.clearSelectedRoute();
    this.mapRenderService.clearRenderedRoute();
  }

  public saveRoute(): void {
    const draft = this.routePlanningStateService.currentRouteDraft();

    if (!draft?.geometryWkt || !draft.distanceMeters || draft.distanceMeters <= 0) {
      this.logger.debug('[Map] User tried to save a route, but no route is drawn or distance is zero');
      this.notificationService.warning('MAP.DRAW_ROUTE_FIRST');
      return;
    }

    if (!this.authService.isAuthenticated()) {
      this.notificationService.warning('MAP.LOGIN_REQUIRED');
      return;
    }

    this.saveRouteModal().open(draft);
  }

  public locateMe(): void {
    this.mapRenderService.locateUser();
  }
}
