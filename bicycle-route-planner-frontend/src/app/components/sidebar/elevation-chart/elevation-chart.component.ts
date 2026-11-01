import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { RouteStateService } from '../../../services/route-state.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-elevation-chart',
  imports: [BaseChartDirective, TranslatePipe],
  templateUrl: './elevation-chart.component.html',
  styleUrl: './elevation-chart.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ElevationChartComponent {
  private static readonly AXIS_COLOR = '#6b7280';

  private readonly routeStateService = inject(RouteStateService);
  private readonly translate = inject(TranslateService);

  public readonly isLoading = this.routeStateService.isLoadingElevation;
  public readonly hasElevationError = this.routeStateService.hasElevationError;
  public readonly elevationProfile = this.routeStateService.elevationProfile;

  public readonly chartData = computed<ChartConfiguration<'scatter'>['data'] | null>(() => {
    const points = this.elevationProfile();
    if (!points) {
      return null;
    }

    return {
      datasets: [
        {
          showLine: true,
          data: points.map((point) => ({ x: point.distance / 1000, y: point.elevation })),
          borderColor: '#3b82f6',
          backgroundColor: 'rgba(59, 130, 246, 0.2)',
          fill: true,
        },
      ],
    };
  });

  public readonly chartOptions = computed<ChartOptions<'scatter'>>(() => {
    this.translate.currentLang();
    const lastDistance = this.elevationProfile()?.at(-1)?.distance;
    const maxX = lastDistance ? lastDistance / 1000 : undefined;
    const axisColor = ElevationChartComponent.AXIS_COLOR;

    return {
      normalized: true,
      parsing: false,
      responsive: true,
      maintainAspectRatio: false,
      elements: {
        point: { radius: 0, hitRadius: 10 },
        line: { borderWidth: 2, cubicInterpolationMode: 'monotone' },
      },
      scales: {
        x: {
          type: 'linear',
          max: maxX,
          title: {
            display: true,
            text: this.translate.instant('SIDEBAR.CHART_DISTANCE_KM'),
            color: axisColor,
          },
          ticks: { color: axisColor },
          grid: { display: false },
        },
        y: {
          title: {
            display: true,
            text: this.translate.instant('SIDEBAR.CHART_ELEVATION_M'),
            color: axisColor,
          },
          ticks: { color: axisColor },
        },
      },
      plugins: {
        legend: { display: false },
        tooltip: { mode: 'nearest', intersect: false },
      },
    };
  });
}
