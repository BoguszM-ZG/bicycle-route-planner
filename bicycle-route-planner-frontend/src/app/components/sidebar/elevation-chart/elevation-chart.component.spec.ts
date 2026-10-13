import { ComponentFixture, TestBed } from '@angular/core/testing';
import { commonTestProviders } from '../../../testing/test-providers';

import { ElevationChartComponent } from './elevation-chart.component';

describe('ElevationChartComponent', () => {
  let component: ElevationChartComponent;
  let fixture: ComponentFixture<ElevationChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ElevationChartComponent],
      providers: [...commonTestProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(ElevationChartComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('routeId', 1);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
