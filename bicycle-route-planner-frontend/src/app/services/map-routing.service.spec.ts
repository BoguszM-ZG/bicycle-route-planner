import { TestBed } from '@angular/core/testing';
import { describe, beforeEach, it, expect } from 'vitest';
import { MapRoutingService } from './map-routing.service';

describe('MapRoutingService', () => {
  let service: MapRoutingService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MapRoutingService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
