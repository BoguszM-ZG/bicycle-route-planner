import { TestBed } from '@angular/core/testing';
import { commonTestProviders } from '../testing/test-providers';

import { MapSearchService } from './map-search.service';

describe('MapSearchService', () => {
  let service: MapSearchService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [...commonTestProviders],
    });
    service = TestBed.inject(MapSearchService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
