import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { ElevationChartComponent } from '../elevation-chart/elevation-chart.component';
import { RouteStateService } from '../../../services/route-state.service';


@Component({
  selector: 'app-route-details',
  imports: [DecimalPipe, DatePipe, ElevationChartComponent],
  templateUrl: './route-details.component.html',
  styleUrl: './route-details.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RouteDetailsComponent {

  private readonly routeStateService = inject(RouteStateService);

  public routeDetail = toSignal(this.routeStateService.selectedRoute$)

}
