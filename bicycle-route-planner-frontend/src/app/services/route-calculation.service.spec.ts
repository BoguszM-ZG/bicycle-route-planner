import { TestBed } from '@angular/core/testing';

import { RouteCalculationService } from './route-calculation.service';

describe('RouteCalculationService', () => {
  let service: RouteCalculationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RouteCalculationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
