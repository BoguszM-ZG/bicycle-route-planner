import { AfterViewInit, Component, inject, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import * as L from 'leaflet';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { TranslateService } from '@ngx-translate/core';
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

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [SidebarComponent, SaveRouteModalComponent, SaveZoneModalComponent, MapControlsComponent, MapModePanelComponent, MapSearchComponent],
  templateUrl: './map.component.html',
  styleUrl: './map.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapComponent implements AfterViewInit {
  private map!: L.Map;
  private userLocationMarker?: L.Marker;

  private readonly authService = inject(AuthService);
  private readonly mapRenderService = inject(MapRenderService);
  public readonly mapRoutingService = inject(MapRoutingService);
  private readonly mapDrawService = inject(MapDrawService);

  @ViewChild('saveRouteModal') saveRouteModal!: SaveRouteModalComponent;
  @ViewChild('saveZoneModal') saveZoneModal!: SaveZoneModalComponent;

  private readonly notificationService = inject(NotificationService);
  private readonly translate = inject(TranslateService);

  ngAfterViewInit(): void {
    this.mapRenderService.initMap('map');
    this.map = this.mapRenderService.getMap()!;
    this.mapRoutingService.initRouting(this.map);
    this.mapDrawService.init(this.map);

    this.mapDrawService.zoneDrawn$.subscribe((wktString) => {
      this.saveZoneModal.open(wktString);
      this.mapRoutingService.routingMode.set('direct');
    });
  }

  public clearRoute(): void {
    this.mapRoutingService.clearRoute();
    this.mapRenderService.clearRenderedRoute();
  }

  public saveRoute(): void {
    const draft = this.mapRoutingService.currentRouteDraft();

    if (!draft?.geometryWkt) {
      this.notificationService.warning(this.translate.instant('MAP.DRAW_ROUTE_FIRST'));
      return;
    }

    if (!this.authService.isAuthenticated()) {
      this.notificationService.warning(this.translate.instant('MAP.LOGIN_REQUIRED'));
      return;
    }

    this.saveRouteModal.open(draft);
  }

  public locateMe(): void {
    if (!this.map) return;

    this.map.locate({ setView: true, maxZoom: 15 });

    this.map.once('locationfound', (e) => {
      if (this.userLocationMarker) {
        this.userLocationMarker.setLatLng(e.latlng);
      } else {
        this.userLocationMarker = L.marker(e.latlng).addTo(this.map).bindPopup(this.translate.instant('MAP.LOCATION_FOUND'));

        this.userLocationMarker.on('click', () => {
          if (this.userLocationMarker) {
            this.userLocationMarker.remove();
            this.userLocationMarker = undefined;
          }
        });
      }
      this.userLocationMarker?.openPopup();
    });
    this.map.once('locationerror', (e) => {
      console.error('Error downloading location:', e);
      this.notificationService.error(this.translate.instant('MAP.LOCATION_ERROR'));
    });
  }
}
