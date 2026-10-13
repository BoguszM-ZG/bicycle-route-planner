import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { MapRenderService } from './map-render.service';

describe('MapRenderService', () => {
  let service: MapRenderService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
      ],
    });
    service = TestBed.inject(MapRenderService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
