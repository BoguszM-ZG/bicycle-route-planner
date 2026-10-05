import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { switchMap, tap, catchError, EMPTY } from 'rxjs';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { RouteService } from '../../../services/route.service';
import { ElevationPointDto } from '../../../models/route.model';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LoggerService } from '../../../services/logger.service';

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
      point: { radius: 0, hitRadius: 10 },
      line: { borderWidth: 2, tension: 0.4 },
    },
    scales: {
      x: {
        type: 'linear',
        title: { display: true, text: '', color: '#aaa' },
        ticks: { color: '#aaa', autoSkip: false },
        grid: { display: false },
      },
      y: {
        title: { display: true, text: '', color: '#aaa' },
        ticks: { color: '#aaa' },
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
          this.updateChartLabels();
        }),

        switchMap((id) =>
          this.routeService.getElevationProfile(id).pipe(
            catchError((err) => {
              this.isLoading.set(false);
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
    this.logger.debug('Received elevation profile. Data points count:', points.length);
    const step = Math.max(1, Math.ceil(points.length / 200));

    const dataPoints = points
      .filter((_, index) => index % step === 0 || index === points.length - 1)
      .map((p) => ({
        x: p.distance / 1000,
        y: p.elevation,
      }));

    const currentOptions = this.chartOptions();
    this.chartOptions.set({
      ...currentOptions,
      scales: {
        ...currentOptions.scales,
        x: {
          ...currentOptions.scales!['x']!,
          max: dataPoints.length > 0 ? dataPoints.at(-1)!.x : 0,
        },
      },
    });

    this.chartData.set({
      datasets: [
        {
          type: 'scatter',
          showLine: true,
          data: dataPoints,
          borderColor: '#3b82f6',
          backgroundColor: 'rgba(59, 130, 246, 0.2)',
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
