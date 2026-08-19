import { Component, input, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { DecimalPipe} from '@angular/common';
import { RouteDetailDto } from '../../../models/route.model';

@Component({
  selector: 'app-route-item',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './route-item.component.html',
  styleUrl: './route-item.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RouteItemComponent {

  public route = input.required<RouteDetailDto>();
  public routeSelected = output<RouteDetailDto>();
  public routeDeleted = output<number>();

  public isConfirmingDelete = signal<boolean>(false);

  public onSelect(): void {
    if(!this.isConfirmingDelete()) {
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
}
