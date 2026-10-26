import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { commonTestProviders } from '../../../testing/test-providers';
import { RouteService } from '../../../services/route.service';
import { ElevationPointDto } from '../../../models/route.model';
import { ElevationChartComponent } from './elevation-chart.component';

const TEST_ROUTE_ID = 1;
const TEST_POINT_COUNT = 250;
const TEST_DISTANCE_STEP = 10;
const TEST_BASE_ELEVATION = 100;
const METERS_PER_KILOMETER = 1000;

describe('ElevationChartComponent', () => {
  let component: ElevationChartComponent;
  let fixture: ComponentFixture<ElevationChartComponent>;
  let routeService: RouteService;

  const mockPoints: ElevationPointDto[] = Array.from({ length: TEST_POINT_COUNT }, (_, i) => ({
    distance: i * TEST_DISTANCE_STEP,
    elevation: TEST_BASE_ELEVATION + (i % 20),
  }));

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ElevationChartComponent],
      providers: [...commonTestProviders],
    }).compileComponents();

    routeService = TestBed.inject(RouteService);
    vi.spyOn(routeService, 'getElevationProfile').mockReturnValue(of(mockPoints));

    fixture = TestBed.createComponent(ElevationChartComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('routeId', TEST_ROUTE_ID);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should map all elevation points 1:1 without decimation filter', () => {
    const chartData = component.chartData();
    expect(chartData).not.toBeNull();
    const dataset = chartData!.datasets[0];
    expect(dataset.data.length).toBe(TEST_POINT_COUNT);

    const firstPoint = dataset.data[0] as { x: number; y: number };
    expect(firstPoint.x).toBe(0);
    expect(firstPoint.y).toBe(TEST_BASE_ELEVATION);

    const lastPoint = dataset.data[TEST_POINT_COUNT - 1] as { x: number; y: number };
    const expectedLastX = ((TEST_POINT_COUNT - 1) * TEST_DISTANCE_STEP) / METERS_PER_KILOMETER;
    expect(lastPoint.x).toBeCloseTo(expectedLastX);
    expect(component.isLoading()).toBe(false);
  });

  it('should handle empty points list gracefully', async () => {
    vi.spyOn(routeService, 'getElevationProfile').mockReturnValue(of([]));
    fixture.componentRef.setInput('routeId', 2);
    await fixture.whenStable();

    const chartData = component.chartData();
    expect(chartData).not.toBeNull();
    expect(chartData!.datasets[0].data.length).toBe(0);
    expect(component.isLoading()).toBe(false);
  });

  it('should handle service error gracefully and clear chart data', async () => {
    vi.spyOn(routeService, 'getElevationProfile').mockReturnValue(throwError(() => new Error('Network error')));
    fixture.componentRef.setInput('routeId', 3);
    await fixture.whenStable();

    expect(component.chartData()).toBeNull();
    expect(component.isLoading()).toBe(false);
  });
});
