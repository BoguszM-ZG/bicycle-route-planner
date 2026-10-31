import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ElevationChartComponent } from '../elevation-chart/elevation-chart.component';
import { RouteStateService } from '../../../services/route-state.service';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-route-details',
  imports: [DecimalPipe, DatePipe, ElevationChartComponent, TranslatePipe],
  templateUrl: './route-details.component.html',
  styleUrl: './route-details.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RouteDetailsComponent {

  private readonly routeStateService = inject(RouteStateService);

  public readonly routeDetail = this.routeStateService.selectedRoute;
  public readonly isLoadingDetails = this.routeStateService.isLoadingDetails;
}
