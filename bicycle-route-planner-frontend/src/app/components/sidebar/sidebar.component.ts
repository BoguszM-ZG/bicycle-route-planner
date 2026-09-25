import { Component, OnInit, inject, ChangeDetectionStrategy, computed, signal, } from '@angular/core';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '../../services/auth.service';
import { RouteStateService } from '../../services/route-state.service';
import { FolderStateService } from '../../services/folder-state.service';

import { RouteSummaryDto } from '../../models/route.model';
import { RouteItemComponent } from './route-item/route-item.component';
import { AvoidanceZonePanelComponent } from './avoidance-zone-panel/avoidance-zone-panel.component';
import { FolderItemComponent } from './folder-item/folder-item.component';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    RouteItemComponent,
    AvoidanceZonePanelComponent,
    FolderItemComponent,
    ReactiveFormsModule,
    TranslatePipe,
  ],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly routeStateService = inject(RouteStateService);
  private readonly folderStateService = inject(FolderStateService);

  public currentUser = this.authService.currentUser;

  public savedRoutes = this.routeStateService.routes;
  public isLoadingRoutes = this.routeStateService.isLoadingList;
  public selectedRouteDetails = this.routeStateService.selectedRoute;
  public folders = this.folderStateService.folders;

  public selectedRouteId = computed(() => this.selectedRouteDetails()?.id ?? null);

  public rootRoutes = computed(() => {
    return this.savedRoutes().filter((r) => r.folderId == null);
  });

  public rootFolders = computed(() => {
    return this.folderStateService.folders().filter((f) => f.parentFolderId == null);
  });

  public isCreatingFolder = signal<boolean>(false);
  public activeTab = signal<'routes' | 'zones'>('routes');

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

  public selectRoute(route: RouteSummaryDto): void {
    if (route.id !== undefined && route.id !== null) {
      this.routeStateService.selectRoute(route.id);
    }
  }

  public deleteRoute(routeId: number): void {
    this.routeStateService.deleteRoute(routeId);
  }

  public exportRoute(routeId: number): void {
    this.routeStateService.exportGpx(routeId);
  }

  public moveRoute(event: { routeId: number; folderId: number | null }): void {
    this.routeStateService.moveRouteToFolder(event.routeId, event.folderId);
  }

  public renameRoute(event: { routeId: number; newName: string }): void {
    this.routeStateService.renameRoute(event.routeId, event.newName);
  }
}
