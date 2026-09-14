import { Component, input, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { DecimalPipe} from '@angular/common';
import { RouteSummaryDto } from '../../../models/route.model';
import { RouteDetailsComponent} from '../route-details/route-details.component';
import { FolderDto } from '../../../models/folder.model';

@Component({
  selector: 'app-route-item',
  standalone: true,
  imports: [DecimalPipe, RouteDetailsComponent],
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

  public allFolders = input<FolderDto[]>();
  public routeMoved = output<{ routeId: number; folderId: number | null }>();
  public isMoving = signal<boolean>(false);

  public isConfirmingDelete = signal<boolean>(false);

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
    if (this.route().id !== undefined) {
      this.routeDeleted.emit(this.route().id!);
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
}
