import { TestBed } from '@angular/core/testing';
import { commonTestProviders } from '../testing/test-providers';

import { MapDrawService } from './map-draw.service';

describe('MapDrawService', () => {
  let service: MapDrawService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [...commonTestProviders],
    });
    service = TestBed.inject(MapDrawService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
