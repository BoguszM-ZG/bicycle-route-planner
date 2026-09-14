import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ElevationChartComponent } from './elevation-chart.component';

describe('ElevationChartComponent', () => {
  let component: ElevationChartComponent;
  let fixture: ComponentFixture<ElevationChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ElevationChartComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ElevationChartComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
