import { TestBed } from '@angular/core/testing';
import { GlobalErrorHandler } from './global-error-handler';
import { commonTestProviders } from '../../testing/test-providers';

describe('GlobalErrorHandler', () => {
  let handler: GlobalErrorHandler;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GlobalErrorHandler, ...commonTestProviders],
    });
    handler = TestBed.inject(GlobalErrorHandler);
  });

  it('should create an instance', () => {
    expect(handler).toBeTruthy();
  });
});
