import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { RouteStateService } from '../../../services/route-state.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

const METERS_PER_KILOMETER = 1000;
const CHART_BORDER_COLOR = '#3b82f6';
const CHART_BACKGROUND_COLOR = 'rgba(59, 130, 246, 0.2)';
const CHART_AXIS_COLOR = '#aaa';
const CHART_LINE_BORDER_WIDTH = 2;
const CHART_POINT_RADIUS = 0;
const CHART_POINT_HIT_RADIUS = 10;

@Component({
  selector: 'app-elevation-chart',
  imports: [BaseChartDirective, TranslatePipe],
  templateUrl: './elevation-chart.component.html',
  styleUrl: './elevation-chart.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ElevationChartComponent {
  private readonly routeStateService = inject(RouteStateService);
  private readonly translate = inject(TranslateService);

  public readonly isLoading = this.routeStateService.isLoadingElevation;
  public readonly hasElevationError = this.routeStateService.hasElevationError;
  public readonly elevationProfile = this.routeStateService.elevationProfile;

  public readonly chartData = computed<ChartConfiguration<'scatter'>['data'] | null>(() => {
    const points = this.routeStateService.elevationProfile();
    if (!points) {
      return null;
    }

    const dataPoints = points.map((point) => ({
      x: point.distance / METERS_PER_KILOMETER,
      y: point.elevation,
    }));

    return {
      datasets: [
        {
          type: 'scatter',
          showLine: true,
          data: dataPoints,
          borderColor: CHART_BORDER_COLOR,
          backgroundColor: CHART_BACKGROUND_COLOR,
          fill: true,
        },
      ],
    };
  });

  public readonly chartOptions = computed<ChartOptions<'scatter'>>(() => {
    this.translate.currentLang();
    const points = this.routeStateService.elevationProfile();
    const hasPoints = points && points.length > 0;
    const lastPoint = hasPoints ? points.at(-1) : undefined;
    const maxX =
      lastPoint && lastPoint.distance > 0
        ? lastPoint.distance / METERS_PER_KILOMETER
        : undefined;

    return {
      normalized: true,
      parsing: false,
      responsive: true,
      maintainAspectRatio: false,
      elements: {
        point: { radius: CHART_POINT_RADIUS, hitRadius: CHART_POINT_HIT_RADIUS },
        line: { borderWidth: CHART_LINE_BORDER_WIDTH, cubicInterpolationMode: 'monotone' },
      },
      scales: {
        x: {
          type: 'linear',
          max: maxX,
          title: {
            display: true,
            text: this.translate.instant('SIDEBAR.CHART_DISTANCE_KM'),
            color: CHART_AXIS_COLOR,
          },
          ticks: { color: CHART_AXIS_COLOR },
          grid: { display: false },
        },
        y: {
          title: {
            display: true,
            text: this.translate.instant('SIDEBAR.CHART_ELEVATION_M'),
            color: CHART_AXIS_COLOR,
          },
          ticks: { color: CHART_AXIS_COLOR },
        },
      },
      plugins: {
        legend: { display: false },
        tooltip: { mode: 'nearest', intersect: false },
      },
    };
  });
}
