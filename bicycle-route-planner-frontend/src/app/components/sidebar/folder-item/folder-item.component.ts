import { Component, ChangeDetectionStrategy, signal, input, inject, computed } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { FolderStateService } from '../../../services/folder-state.service';
import { RouteStateService } from '../../../services/route-state.service';
import { RouteItemComponent } from '../route-item/route-item.component';
import { TranslatePipe } from '@ngx-translate/core';


@Component({
  standalone: true,
  selector: 'app-folder-item',
  imports: [RouteItemComponent, ReactiveFormsModule, TranslatePipe],
  templateUrl: './folder-item.component.html',
  styleUrl: './folder-item.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FolderItemComponent {
  public folderId = input.required<number>();
  private readonly folderStateService = inject(FolderStateService);
  public readonly routeStateService = inject(RouteStateService);
  public isExpanded = signal<boolean>(false);
  public allFolders = this.folderStateService.folders;

  public isRenaming = signal<boolean>(false);
  public isConfirmingDelete = signal<boolean>(false);
  public renameControl = new FormControl('', [Validators.required, Validators.maxLength(50)]);

  public toggleExpanded(): void {
    if (this.isRenaming() || this.isConfirmingDelete()) return;
    this.isExpanded.set(!this.isExpanded());
  }

  public promptDelete(event: Event): void {
    event.stopPropagation();
    this.isConfirmingDelete.set(true);
  }

  public cancelDelete(event: Event): void {
    event.stopPropagation();
    this.isConfirmingDelete.set(false);
  }

  public confirmDelete(event: Event): void {
    event.stopPropagation();


    this.routeStateService.handleFolderDeleted(this.folderId());

    this.folderStateService.deleteFolder(this.folderId());
  }

  public toggleRename(event: Event): void {
    event.stopPropagation();
    if (!this.isRenaming()) {
      this.renameControl.setValue(this.folderData()?.name || '');
    }
    this.isRenaming.set(!this.isRenaming());
  }

  public confirmRename(event: Event): void {
    event.stopPropagation();
    if (this.renameControl.valid && this.renameControl.value) {
      this.folderStateService.renameFolder(this.folderId(), this.renameControl.value);
      this.isRenaming.set(false);
    }
  }

  public folderData = computed(() => {
    return this.folderStateService.folders().find((f) => f.id === this.folderId());
  });

  public subFolders = computed(() => {
    return this.folderStateService.folders().filter((f) => f.parentFolderId === this.folderId());
  });

  public folderRoutes = computed(() => {
    return this.routeStateService.routes().filter((r) => r.folderId === this.folderId());
  });
}
