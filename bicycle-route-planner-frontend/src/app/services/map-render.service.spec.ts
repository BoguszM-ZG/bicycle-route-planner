import { TestBed } from '@angular/core/testing';

import { MapRenderService } from './map-render.service';

describe('MapRenderService', () => {
  let service: MapRenderService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MapRenderService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
