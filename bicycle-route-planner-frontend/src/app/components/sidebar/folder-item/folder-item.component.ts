import { Component, ChangeDetectionStrategy, signal, input, inject, computed } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { FolderStateService } from '../../../services/folder-state.service';
import { RouteStateService } from '../../../services/route-state.service';
import { FolderDto } from '../../../models/folder.model';
import { RouteItemComponent } from '../route-item/route-item.component';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-folder-item',
  imports: [RouteItemComponent, ReactiveFormsModule, TranslatePipe],
  templateUrl: './folder-item.component.html',
  styleUrl: './folder-item.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FolderItemComponent {
  private readonly folderStateService = inject(FolderStateService);
  private readonly routeStateService = inject(RouteStateService);

  public readonly folder = input.required<FolderDto>();

  public readonly allFolders = this.folderStateService.folders;
  public readonly selectedRouteId = this.routeStateService.selectedRouteId;
  public readonly folderRoutes = computed(() =>
    this.routeStateService.routes().filter((r) => r.folderId === this.folder().id),
  );

  public readonly isExpanded = signal<boolean>(false);
  public readonly isRenaming = signal<boolean>(false);
  public readonly isConfirmingDelete = signal<boolean>(false);
  public readonly renameControl = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)],
  });

  public toggleExpanded(): void {
    this.isExpanded.update((v) => !v);
  }

  public promptDelete(): void {
    this.isConfirmingDelete.set(true);
  }

  public cancelDelete(): void {
    this.isConfirmingDelete.set(false);
  }

  public confirmDelete(): void {
    this.folderStateService.deleteFolder(this.folder().id);
    this.isConfirmingDelete.set(false);
  }

  public toggleRename(): void {
    if (!this.isRenaming()) {
      this.renameControl.setValue(this.folder().name);
    }
    this.isRenaming.update((v) => !v);
  }

  public confirmRename(): void {
    if (this.renameControl.invalid) {
      return;
    }

    const newName = this.renameControl.value.trim();
    if (newName !== this.folder().name) {
      this.folderStateService.renameFolder(this.folder().id, newName);
    }
    this.isRenaming.set(false);
  }
}
