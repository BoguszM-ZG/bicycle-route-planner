import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { switchMap, tap, catchError, EMPTY } from 'rxjs';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { RouteService } from '../../../services/route.service';
import { ElevationPointDto } from '../../../models/route.model';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LoggerService } from '../../../services/logger.service';

const METERS_PER_KILOMETER = 1000;
const CHART_BORDER_COLOR = '#3b82f6';
const CHART_BACKGROUND_COLOR = 'rgba(59, 130, 246, 0.2)';
const CHART_AXIS_COLOR = '#aaa';
const CHART_LINE_BORDER_WIDTH = 2;
const CHART_LINE_TENSION = 0.4;
const CHART_POINT_RADIUS = 0;
const CHART_POINT_HIT_RADIUS = 10;

@Component({
  selector: 'app-elevation-chart',
  standalone: true,
  imports: [BaseChartDirective, TranslatePipe],
  templateUrl: './elevation-chart.component.html',
  styleUrl: './elevation-chart.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ElevationChartComponent {
  public routeId = input.required<number>();

  private readonly routeService = inject(RouteService);
  private readonly translate = inject(TranslateService);
  private readonly logger = inject(LoggerService);

  public chartData = signal<ChartConfiguration<'scatter'>['data'] | null>(null);
  public isLoading = signal<boolean>(true);

  public chartOptions = signal<ChartOptions<'scatter'>>({
    normalized: true,
    parsing: false,
    responsive: true,
    maintainAspectRatio: false,
    elements: {
      point: { radius: CHART_POINT_RADIUS, hitRadius: CHART_POINT_HIT_RADIUS },
      line: { borderWidth: CHART_LINE_BORDER_WIDTH, tension: CHART_LINE_TENSION },
    },
    scales: {
      x: {
        type: 'linear',
        title: { display: true, text: '', color: CHART_AXIS_COLOR },
        ticks: { color: CHART_AXIS_COLOR, autoSkip: false },
        grid: { display: false },
      },
      y: {
        title: { display: true, text: '', color: CHART_AXIS_COLOR },
        ticks: { color: CHART_AXIS_COLOR },
      },
    },
    plugins: {
      legend: { display: false },
      tooltip: { mode: 'nearest', intersect: false },
    },
  });

  constructor() {

    this.translate.onLangChange.pipe(takeUntilDestroyed()).subscribe(() => {
      this.updateChartLabels();
    });

    toObservable(this.routeId)
      .pipe(
        tap(() => {
          this.isLoading.set(true);
          this.chartData.set(null);
          this.updateChartLabels();
        }),

        switchMap((id) =>
          this.routeService.getElevationProfile(id).pipe(
            catchError(() => {
              this.isLoading.set(false);
              this.chartData.set(null);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((points: ElevationPointDto[]) => {
        this.renderChart(points);
      });
  }

  private renderChart(points: ElevationPointDto[]): void {
    this.logger.debug('[ElevationChartComponent] Received elevation profile', {
      pointsCount: points.length,
    });

    const dataPoints = points.map((point) => ({
      x: point.distance / METERS_PER_KILOMETER,
      y: point.elevation,
    }));

    const currentOptions = this.chartOptions();
    const hasPoints = dataPoints.length > 0;
    const lastPoint = hasPoints ? dataPoints.at(-1) : undefined;
    const maxX = lastPoint && lastPoint.x > 0 ? lastPoint.x : undefined;

    this.chartOptions.set({
      ...currentOptions,
      scales: {
        ...currentOptions.scales,
        x: {
          ...currentOptions.scales!['x']!,
          max: maxX,
        },
      },
    });

    this.chartData.set({
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
    });

    this.isLoading.set(false);
  }

  private updateChartLabels(): void {
    const currentOptions = this.chartOptions();
    this.chartOptions.set({
      ...currentOptions,
      scales: {
        ...currentOptions.scales,
        x: {
          ...currentOptions.scales!['x']!,
          title: {
            ...currentOptions.scales!['x']!.title,
            display: true,
            text: this.translate.instant('SIDEBAR.CHART_DISTANCE_KM'),
          },
        },
        y: {
          ...currentOptions.scales!['y']!,
          title: {
            ...currentOptions.scales!['y']!.title,
            display: true,
            text: this.translate.instant('SIDEBAR.CHART_ELEVATION_M'),
          },
        },
      },
    });
  }
}
