import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
  computed,
  signal,
} from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { RouteSummaryDto } from '../../models/route.model';
import { RouteStateService } from '../../services/route-state.service';
import { RouteItemComponent} from './route-item/route-item.component';
import { AvoidanceZonePanelComponent } from './avoidance-zone-panel/avoidance-zone-panel.component';
import { FolderStateService } from '../../services/folder-state.service';
import { FolderItemComponent } from './folder-item/folder-item.component';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouteItemComponent, AvoidanceZonePanelComponent, FolderItemComponent, ReactiveFormsModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent implements OnInit {
  private readonly authService = inject(AuthService);
  public readonly routeStateService = inject(RouteStateService);
  private readonly folderStateService = inject(FolderStateService);

  public currentUser = this.authService.currentUser;

  public savedRoutes = this.routeStateService.routes;
  public isLoadingRoutes = this.routeStateService.isLoadingList;
  public selectedRouteDetails = this.routeStateService.selectedRoute;

  public selectedRouteId = computed(() => this.selectedRouteDetails()?.id ?? null);

  public folders = this.folderStateService.folders;

  public rootRoutes = computed(() => {
    return this.savedRoutes().filter((r) => r.folderId == null);
  });

  public rootFolders = computed(() => {
    return this.folderStateService.folders().filter((f) => f.parentFolderId == null);
  });

  public isCreatingFolder = signal<boolean>(false);

  public newFolderName = new FormControl('', {
    validators: [Validators.required, Validators.maxLength(50)],
    nonNullable: true,
  });

  ngOnInit() {
    this.routeStateService.fetchRoutes();
    this.folderStateService.fetchFolders();
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

  public toggleCreateFolder(): void {
    this.isCreatingFolder.update((v) => !v);
    this.newFolderName.reset();
  }

  public submitNewFolder(): void {
    if (this.newFolderName.invalid) return;

    const folderName = this.newFolderName.value.trim();
    if (!folderName) return;

    this.folderStateService.createFolder({ name: folderName });
    this.toggleCreateFolder();
  }
}
