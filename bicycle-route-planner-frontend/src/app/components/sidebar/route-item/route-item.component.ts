import { Component, input, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { DecimalPipe} from '@angular/common';
import { RouteSummaryDto } from '../../../models/route.model';
import { RouteDetailsComponent} from '../route-details/route-details.component';
import { FolderDto } from '../../../models/folder.model';
import { FormControl, Validators, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-route-item',
  standalone: true,
  imports: [DecimalPipe, RouteDetailsComponent, ReactiveFormsModule, TranslatePipe],
  templateUrl: './route-item.component.html',
  styleUrl: './route-item.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RouteItemComponent {
  public route = input.required<RouteSummaryDto>();
  public isSelected = input<boolean>(false);

  public routeSelected = output<RouteSummaryDto>();
  public routeDeleted = output<number>();
  public exportGpx = output<number>();

  public allFolders = input<FolderDto[]>([]);
  public routeMoved = output<{ routeId: number; folderId: number | null }>();
  public isMoving = signal<boolean>(false);

  public isConfirmingDelete = signal<boolean>(false);

  public isRenaming = signal<boolean>(false);
  public renameControl = new FormControl('', [Validators.required, Validators.maxLength(50)]);
  public routeRenamed = output<{ routeId: number; newName: string }>();

  public onSelect(): void {
    if (!this.isConfirmingDelete()) {
      this.routeSelected.emit(this.route());
    }
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
    const id = this.route().id;
    if (id !== undefined && id !== null) {
      this.routeDeleted.emit(id);
    }
  }

  public onExportGpx(event: Event): void {
    event.stopPropagation();
    if (this.route()?.id) {
      this.exportGpx.emit(this.route().id!);
    }
  }

  public toggleMove(event: Event): void {
    event.stopPropagation();
    this.isMoving.set(!this.isMoving());
  }
  public confirmMove(event: Event, targetFolderId: number | null): void {
    event.stopPropagation();
    this.routeMoved.emit({ routeId: this.route().id!, folderId: targetFolderId });
    this.isMoving.set(false);
  }

  public toggleRename(event: Event): void {
    event.stopPropagation();
    if (!this.isRenaming()) {
      this.renameControl.setValue(this.route().name);
    }
    this.isRenaming.set(!this.isRenaming());
  }

  public confirmRename(event: Event): void {
    event.stopPropagation();
    if (this.renameControl.valid && this.renameControl.value) {
      this.routeRenamed.emit({ routeId: this.route().id!, newName: this.renameControl.value });
      this.isRenaming.set(false);
    }
  }
}
