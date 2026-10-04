import {
  AfterViewInit,
  Component,
  inject,
  ChangeDetectionStrategy,
  viewChild,
  NgZone,
  DestroyRef,
} from '@angular/core';
import * as L from 'leaflet';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { TranslatePipe } from '@ngx-translate/core';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { MapRenderService } from '../../services/map-render.service';
import { SaveRouteModalComponent } from '../save-route-modal/save-route-modal.component';
import { SaveZoneModalComponent } from '../save-zone-modal/save-zone-modal.component';
import { MapControlsComponent } from '../map-controls/map-controls.component';
import { MapRoutingService } from '../../services/map-routing.service';
import { MapDrawService } from '../../services/map-draw.service';
import { MapModePanelComponent } from './map-mode-panel/map-mode-panel.component';
import { MapSearchComponent } from '../map-search/map-search.component';
import { MAP_CONSTANTS } from '../../constants/map.constants';


L.Marker.prototype.options.icon = MAP_CONSTANTS.DEFAULT_ICON;

import { LoggerService } from '../../services/logger.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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
  public readonly mapRoutingService = inject(MapRoutingService);
  private readonly mapDrawService = inject(MapDrawService);
  private readonly notificationService = inject(NotificationService);
  private readonly logger = inject(LoggerService);
  private readonly ngZone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);

  public saveRouteModal = viewChild.required<SaveRouteModalComponent>('saveRouteModal');
  public saveZoneModal = viewChild.required<SaveZoneModalComponent>('saveZoneModal');

  ngAfterViewInit(): void {
    this.logger.info('[Map] Leaflet map successfully initialized');

    this.ngZone.runOutsideAngular(() => {
      this.mapRenderService.initMap('map');
      this.map = this.mapRenderService.getMap()!;
      this.mapRoutingService.initRouting(this.map);
      this.mapDrawService.init(this.map);
    });

    this.mapDrawService.zoneDrawn$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((wktString) => {
        this.ngZone.run(() => {
          this.saveZoneModal().open(wktString);
          this.mapRoutingService.routingMode.set('direct');
        });
      });
  }

  public clearRoute(): void {
    this.mapRoutingService.clearRoute();
    this.mapRenderService.clearRenderedRoute();
  }

  public saveRoute(): void {
    const draft = this.mapRoutingService.currentRouteDraft();

    if (!draft?.geometryWkt) {
      this.logger.debug('[Map]User tried to save a route, but no route is drawn');
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
