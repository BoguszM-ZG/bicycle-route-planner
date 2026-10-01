import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
  computed,
  signal,
} from '@angular/core';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { RouteStateService } from '../../../services/route-state.service';
import { FolderStateService } from '../../../services/folder-state.service';
import { RouteSummaryDto } from '../../../models/route.model';

import { RouteItemComponent } from '../route-item/route-item.component';
import { FolderItemComponent } from '../folder-item/folder-item.component';

@Component({
  selector: 'app-route-panel',
  standalone: true,
  imports: [RouteItemComponent, FolderItemComponent, ReactiveFormsModule, TranslatePipe],
  templateUrl: './route-panel.component.html',
  // Możesz przenieść style tworzenia folderów z sidebar.component.css do route-panel.component.css
  styleUrl: './route-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoutePanelComponent implements OnInit {
  // 1. Zależności (Tylko te dotyczące tras i folderów)
  private readonly routeStateService = inject(RouteStateService);
  private readonly folderStateService = inject(FolderStateService);

  // 2. Signale i Stan
  public readonly isLoadingRoutes = this.routeStateService.isLoadingList;
  public readonly folders = this.folderStateService.folders;
  public readonly selectedRouteDetails = this.routeStateService.selectedRoute;

  public readonly selectedRouteId = computed(() => this.selectedRouteDetails()?.id ?? null);
  public readonly rootRoutes = computed(() =>
    this.routeStateService.routes().filter((r) => r.folderId == null),
  );
  public readonly rootFolders = computed(() =>
    this.folderStateService.folders().filter((f) => f.parentFolderId == null),
  );

  // 3. Formularze i stan UI
  public readonly isCreatingFolder = signal<boolean>(false);
  public readonly newFolderName = new FormControl('', {
    validators: [Validators.required, Validators.maxLength(50)],
    nonNullable: true,
  });

  // 4. Inicjalizacja (Pobieranie danych)
  ngOnInit(): void {
    this.routeStateService.fetchRoutes();
    this.folderStateService.fetchFolders();
  }

  // 5. Akcje (Actions)
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
