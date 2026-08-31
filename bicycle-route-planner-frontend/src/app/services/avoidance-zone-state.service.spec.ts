import { TestBed } from '@angular/core/testing';

import { AvoidanceZoneStateService } from './avoidance-zone-state.service';

describe('AvoidanceZoneStateService', () => {
  let service: AvoidanceZoneStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AvoidanceZoneStateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
