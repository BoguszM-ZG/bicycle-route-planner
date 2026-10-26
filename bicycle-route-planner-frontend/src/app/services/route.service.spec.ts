import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RouteService } from './route.service';
import { environment } from '../../environments/environment';

describe('RouteService', () => {
  let service: RouteService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [RouteService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RouteService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should request elevation profile without extra params', () => {
    const routeId = 1;
    service.getElevationProfile(routeId).subscribe();

    const req = httpTesting.expectOne(`${environment.apiUrl}/routes/${routeId}/elevation`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
