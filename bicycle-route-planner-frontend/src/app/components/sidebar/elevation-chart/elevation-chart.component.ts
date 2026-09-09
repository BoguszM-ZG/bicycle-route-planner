import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  inject,
  Input,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { RouteService } from '../../../services/route.service';
import { ElevationPointDto } from '../../../models/route.model';

@Component({
  selector: 'app-elevation-chart',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './elevation-chart.component.html',
  styleUrl: './elevation-chart.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ElevationChartComponent implements OnChanges {
  @Input({ required: true }) routeId!: number;
  private readonly routeService = inject(RouteService);
  private readonly cdr = inject(ChangeDetectorRef);

  public chartData: ChartConfiguration<'scatter'>['data'] | null = null;
  public isLoading = true;

  public chartOptions: ChartOptions<'scatter'> = {
    normalized: true,
    parsing: false,
    responsive: true,
    maintainAspectRatio: false,
    elements: {
      point: { radius: 0, hitRadius: 10 },
      line: {
        borderWidth: 2,
        tension: 0.4,
      },
    },
    scales: {
      x: {
        type: 'linear',
        title: { display: true, text: 'Dystans (km)', color: '#aaa' },
        ticks: { color: '#aaa', autoSkip: false },
        grid: { display: false },
      },
      y: {
        title: { display: true, text: 'Wysokość (m n.p.m)', color: '#aaa' },
        ticks: { color: '#aaa' },
      },
    },
    plugins: {
      legend: { display: false },
      tooltip: { mode: 'nearest', intersect: false },
    },
  };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['routeId']?.currentValue) {
      this.loadChartData();
    }

  }


  private loadChartData(): void {
    this.isLoading = true;
    this.routeService.getElevationProfile(this.routeId).subscribe({
      next: (points: ElevationPointDto[]) => {
        const step = Math.max(1, Math.ceil(points.length / 200));

        const dataPoints = points
          .filter((_, index) => index % step === 0 || index === points.length - 1)
          .map((p) => ({
            x: p.distance / 1000,
            y: p.elevation,
          }));

        this.chartOptions.scales!['x']!.max = dataPoints.at(-1)!.x;
        this.chartData = {
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
        };

        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Błąd ładowania wysokości:', err);
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }
}
