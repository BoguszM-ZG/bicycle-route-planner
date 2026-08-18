import { Component, OnInit, inject, ChangeDetectorRef, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { RouteService } from '../../services/route.service';
import { RouteDetailDto } from '../../models/route.model';
import { UserDto } from '../../models/auth.model';
import { RouteStateService } from '../../services/route-state.service';


@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly routeService = inject(RouteService);
  private readonly routeStateService = inject(RouteStateService);
  private readonly cdr = inject(ChangeDetectorRef);

  public currentUser: UserDto | null = null;
  public savedRoutes: RouteDetailDto[] = [];
  public isLoadingRoutes: boolean = false;

  @Output() routeSelected: EventEmitter<RouteDetailDto> = new EventEmitter<RouteDetailDto>();

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();

    this.fetchUserRoutes();


    this.routeService.routeAdded$.subscribe(() => {
      this.fetchUserRoutes();
    });
  }

  private fetchUserRoutes(): void {
    this.isLoadingRoutes = true;
    this.routeService.getRoutes().subscribe({
      next: (routes) => {
        console.log('Sidebar odebrał trasy z backendu!', routes);
        this.savedRoutes = routes;
        this.isLoadingRoutes = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Błąd podczas pobierania tras (Sidebar):', err);
        this.isLoadingRoutes = false;
      }
    });
  }

  public logout(): void {
    this.authService.logout();
  }

  public selectRoute(route: RouteDetailDto): void {
    console.log('Wybrana trasa w sidebarze:', route.name);
    this.routeStateService.selectRoute(route);
  }




}
