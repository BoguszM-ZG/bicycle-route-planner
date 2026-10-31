import {
  Component,
  inject,
  ChangeDetectionStrategy,
  computed,
  signal,
} from '@angular/core';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { RouteStateService } from '../../../services/route-state.service';
import { FolderStateService } from '../../../services/folder-state.service';

import { RouteItemComponent } from '../route-item/route-item.component';
import { FolderItemComponent } from '../folder-item/folder-item.component';

@Component({
  selector: 'app-route-panel',
  imports: [RouteItemComponent, FolderItemComponent, ReactiveFormsModule, TranslatePipe],
  templateUrl: './route-panel.component.html',
  styleUrl: './route-panel.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoutePanelComponent {
  private readonly routeStateService = inject(RouteStateService);
  private readonly folderStateService = inject(FolderStateService);

  public readonly isLoadingRoutes = this.routeStateService.isLoadingList;
  public readonly folders = this.folderStateService.folders;

  public readonly selectedRouteId = this.routeStateService.selectedRouteId;
  public readonly unassignedRoutes = computed(() =>
    this.routeStateService.routes().filter((r) => r.folderId == null),
  );

  public readonly isCreatingFolder = signal<boolean>(false);
  public readonly newFolderName = new FormControl('', {
    validators: [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)],
    nonNullable: true,
  });

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
