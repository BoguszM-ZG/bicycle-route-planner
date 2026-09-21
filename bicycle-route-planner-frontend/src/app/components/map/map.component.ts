import { AfterViewInit, Component, inject, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import * as L from 'leaflet';
import { AuthService } from '../../services/auth.service';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { MapRenderService } from '../../services/map-render.service';
import { SaveRouteModalComponent } from '../save-route-modal/save-route-modal.component';
import { SaveZoneModalComponent } from '../save-zone-modal/save-zone-modal.component';
import { MapControlsComponent } from '../map-controls/map-controls.component';

import { MapRoutingService } from '../../services/map-routing.service';
import { MapDrawService } from '../../services/map-draw.service';
import { MapModePanelComponent } from './map-mode-panel/map-mode-panel.component';
import { MapSearchComponent } from '../map-search/map-search.component';

const defaultIcon = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});
L.Marker.prototype.options.icon = defaultIcon;

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
      alert('Najpierw wyznacz trasę, żeby ją zapisać.');
      return;
    }

    if (!this.authService.isAuthenticated()) {
      alert('Musisz być zalogowany, aby zapisać trasę.');
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
        this.userLocationMarker = L.marker(e.latlng).addTo(this.map).bindPopup('Tu jesteś!');
        
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
      alert('Failed to retrieve location. Make sure you have granted location access in your browser.',
      );
    });
  }
}
