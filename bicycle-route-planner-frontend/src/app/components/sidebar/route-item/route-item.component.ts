import { Component, input, signal, inject, ChangeDetectionStrategy } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouteExportFormat, RouteSummaryDto } from '../../../models/route.model';
import { RouteDetailsComponent } from '../route-details/route-details.component';
import { FolderDto } from '../../../models/folder.model';
import { FormControl, Validators, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { RouteStateService } from '../../../services/route-state.service';

@Component({
  selector: 'app-route-item',
  imports: [DecimalPipe, RouteDetailsComponent, ReactiveFormsModule, TranslatePipe],
  templateUrl: './route-item.component.html',
  styleUrl: './route-item.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RouteItemComponent {
  private readonly routeStateService = inject(RouteStateService);

  public readonly route = input.required<RouteSummaryDto>();
  public readonly isSelected = input<boolean>(false);
  public readonly allFolders = input<FolderDto[]>([]);

  public readonly isMoving = signal<boolean>(false);
  public readonly isConfirmingDelete = signal<boolean>(false);
  public readonly isRenaming = signal<boolean>(false);
  public readonly isExporting = signal<boolean>(false);
  public readonly renameControl = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(150), Validators.pattern(/\S/)],
  });

  public selectRoute(): void {
    this.routeStateService.selectRoute(this.route().id);
  }

  public promptDelete(): void {
    this.isConfirmingDelete.set(true);
  }

  public cancelDelete(): void {
    this.isConfirmingDelete.set(false);
  }

  public deleteRoute(): void {
    this.routeStateService.deleteRoute(this.route().id);
    this.isConfirmingDelete.set(false);
  }

  public toggleExport(): void {
    this.isExporting.update((v) => !v);
  }

  public exportRoute(format: RouteExportFormat): void {
    this.routeStateService.downloadRoute(this.route().id, format);
    this.isExporting.set(false);
  }

  public toggleMove(): void {
    this.isMoving.update((v) => !v);
  }

  public moveToFolder(targetFolderId: number | null): void {
    this.routeStateService.moveRouteToFolder(this.route().id, targetFolderId);
    this.isMoving.set(false);
  }

  public toggleRename(): void {
    if (!this.isRenaming()) {
      this.renameControl.setValue(this.route().name);
    }
    this.isRenaming.update((v) => !v);
  }

  public saveRename(): void {
    if (this.renameControl.invalid) {
      return;
    }

    const newName = this.renameControl.value.trim();
    if (newName !== this.route().name) {
      this.routeStateService.renameRoute(this.route().id, newName);
    }
    this.isRenaming.set(false);
  }
}
