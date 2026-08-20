import { Component, OnInit, inject, output, signal, ChangeDetectionStrategy} from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { RouteService } from '../../services/route.service';
import { RouteDetailDto } from '../../models/route.model';
import { UserDto } from '../../models/auth.model';
import { RouteStateService } from '../../services/route-state.service';
import { RouteItemComponent} from './route-item/route-item.component';



@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouteItemComponent],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly routeService = inject(RouteService);
  private readonly routeStateService = inject(RouteStateService);

  public currentUser = signal<UserDto | null>(null);
  public savedRoutes = signal<RouteDetailDto[]>([]);
  public isLoadingRoutes = signal<boolean>(false);
  public selectedRouteId = signal<number | null>(null);

  routeSelected = output<RouteDetailDto>();

  ngOnInit() {
    this.currentUser.set(this.authService.getCurrentUser());
    this.fetchUserRoutes();

    this.routeService.routeAdded$.subscribe(() => {
      this.fetchUserRoutes();
    });
  }

  private fetchUserRoutes(): void {
    this.isLoadingRoutes.set(true);
    this.routeService.getRoutes().subscribe({
      next: (routes) => {
        console.log('Sidebar odebrał trasy z backendu!', routes);
        this.savedRoutes.set(routes);
        this.isLoadingRoutes.set(false);
      },
      error: (err) => {
        console.error('Błąd podczas pobierania tras (Sidebar):', err);
        this.isLoadingRoutes.set(false);
      },
    });
  }

  public logout(): void {
    this.authService.logout();
  }

  public selectRoute(route: RouteDetailDto): void {
    if (this.selectedRouteId() === route.id) {
      this.selectedRouteId.set(null);
      this.routeStateService.selectRoute(null);
    } else {
      this.selectedRouteId.set(route.id!);
      this.routeStateService.selectRoute(route);
    }
  }

  public deleteRoute(routeId: number): void {
    this.routeService.deleteRoute(routeId).subscribe({
      next: () => {
        this.savedRoutes.set(this.savedRoutes().filter((route) => route.id !== routeId));
      },
      error: (err) => {
        console.error('Błąd podczas usuwania trasy (Sidebar):', err);
        alert('Wystąpił błąd podczas usuwania trasy. Spróbuj ponownie.');
      },
    });
  }
}
