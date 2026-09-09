import { Component, input, output, signal, ChangeDetectionStrategy, inject } from '@angular/core';
import { DecimalPipe} from '@angular/common';
import { RouteSummaryDto } from '../../../models/route.model';
import { RouteDetailsComponent} from '../route-details/route-details.component';
import { RouteService } from '../../../services/route.service';

@Component({
  selector: 'app-route-item',
  standalone: true,
  imports: [DecimalPipe, RouteDetailsComponent],
  templateUrl: './route-item.component.html',
  styleUrl: './route-item.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RouteItemComponent {
  private readonly routeService = inject(RouteService);
  public route = input.required<RouteSummaryDto>();
  public routeSelected = output<RouteSummaryDto>();
  public routeDeleted = output<number>();
  public isSelected = input<boolean>(false);

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
      this.routeService.downloadGpx(this.route().id!);
    }
  }
}
