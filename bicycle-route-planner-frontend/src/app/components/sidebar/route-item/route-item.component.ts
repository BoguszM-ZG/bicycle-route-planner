import {
  Component,
  input,
  signal,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouteExportFormat, RouteSummaryDto } from '../../../models/route.model';
import { RouteDetailsComponent } from '../route-details/route-details.component';
import { FolderDto } from '../../../models/folder.model';
import { FormControl, Validators, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { RouteStateService } from '../../../services/route-state.service';

@Component({
  selector: 'app-route-item',
  standalone: true,
  imports: [DecimalPipe, RouteDetailsComponent, ReactiveFormsModule, TranslatePipe],
  templateUrl: './route-item.component.html',
  styleUrl: './route-item.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RouteItemComponent {
  public static readonly MAX_NAME_LENGTH = 150;

  private readonly routeStateService = inject(RouteStateService);

  public route = input.required<RouteSummaryDto>();
  public isSelected = input<boolean>(false);
  public allFolders = input<FolderDto[]>([]);

  public isMoving = signal<boolean>(false);
  public isConfirmingDelete = signal<boolean>(false);
  public isRenaming = signal<boolean>(false);
  public isExporting = signal<boolean>(false);
  public renameControl = new FormControl<string>('', {
    nonNullable: true,
    validators: [
      Validators.required,
      Validators.maxLength(RouteItemComponent.MAX_NAME_LENGTH),
      Validators.pattern(/\S/),
    ],
  });

  public selectRoute(): void {
    if (!this.isConfirmingDelete() && this.route().id !== undefined && this.route().id !== null) {
      this.routeStateService.selectRoute(this.route().id);
    }
  }

  public onSelect(): void {
    this.selectRoute();
  }

  public promptDelete(event: Event): void {
    event.stopPropagation();
    this.isConfirmingDelete.set(true);
  }

  public cancelDelete(event: Event): void {
    event.stopPropagation();
    this.isConfirmingDelete.set(false);
  }

  public deleteRoute(event?: Event): void {
    event?.stopPropagation();
    const id = this.route().id;
    if (id !== undefined && id !== null) {
      this.routeStateService.deleteRoute(id);
    }
    this.isConfirmingDelete.set(false);
  }

  public confirmDelete(event: Event): void {
    this.deleteRoute(event);
  }

  public toggleExport(event: Event): void {
    event.stopPropagation();
    this.isExporting.update((v) => !v);
  }

  public exportRoute(format: RouteExportFormat, event: Event): void {
    event.stopPropagation();
    this.routeStateService.downloadRoute(this.route().id, format, this.route().name);
    this.isExporting.set(false);
  }

  public toggleMove(event: Event): void {
    event.stopPropagation();
    this.isMoving.update((v) => !v);
  }

  public moveToFolder(targetFolderId: number | null, event?: Event): void {
    event?.stopPropagation();
    const id = this.route().id;
    if (id !== undefined && id !== null) {
      this.routeStateService.moveRouteToFolder(id, targetFolderId);
    }
    this.isMoving.set(false);
  }

  public confirmMove(event: Event, targetFolderId: number | null): void {
    this.moveToFolder(targetFolderId, event);
  }

  public toggleRename(event: Event): void {
    event.stopPropagation();
    if (!this.isRenaming()) {
      this.renameControl.setValue(this.route().name);
    }
    this.isRenaming.update((v) => !v);
  }

  public saveRename(event?: Event): void {
    event?.stopPropagation();
    if (this.renameControl.invalid) {
      return;
    }

    const newName = this.renameControl.value.trim();
    const oldName = this.route().name;
    if (!newName || newName === oldName) {
      this.isRenaming.set(false);
      return;
    }
    const id = this.route().id;
    if (id !== undefined && id !== null) {
      this.routeStateService.renameRoute(id, newName);
    }
    this.isRenaming.set(false);
  }

  public confirmRename(event: Event): void {
    this.saveRename(event);
  }
}
