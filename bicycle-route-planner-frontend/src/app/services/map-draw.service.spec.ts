import { TestBed } from '@angular/core/testing';

import { MapDrawService } from './map-draw.service';

describe('MapDrawService', () => {
  let service: MapDrawService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MapDrawService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
