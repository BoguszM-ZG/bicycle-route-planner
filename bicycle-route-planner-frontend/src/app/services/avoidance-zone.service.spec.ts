import { TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach } from 'vitest';

import { AvoidanceZoneService } from './avoidance-zone.service';

describe('AvoidanceZoneService', () => {
  let service: AvoidanceZoneService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AvoidanceZoneService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
