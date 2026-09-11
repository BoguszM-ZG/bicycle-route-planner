import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
  computed
} from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { RouteSummaryDto } from '../../models/route.model';
import { RouteStateService } from '../../services/route-state.service';
import { RouteItemComponent} from './route-item/route-item.component';
import { AvoidanceZonePanelComponent } from './avoidance-zone-panel/avoidance-zone-panel.component';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouteItemComponent, AvoidanceZonePanelComponent],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent implements OnInit {
  private readonly authService = inject(AuthService);
  public readonly routeStateService = inject(RouteStateService);

  public currentUser = this.authService.currentUser;
  
  public savedRoutes = this.routeStateService.routes;
  public isLoadingRoutes = this.routeStateService.isLoadingList;
  public selectedRouteDetails = this.routeStateService.selectedRoute;
  
  public selectedRouteId = computed(() => this.selectedRouteDetails()?.id ?? null);

  ngOnInit() {
    this.routeStateService.fetchRoutes();
  }

  public logout(): void {
    this.authService.logout();
  }

  public selectRoute(route: RouteSummaryDto): void {
    if (route.id) {
      this.routeStateService.selectRoute(route.id);
    }
  }

  public deleteRoute(routeId: number): void {
    this.routeStateService.deleteRoute(routeId);
  }
}
