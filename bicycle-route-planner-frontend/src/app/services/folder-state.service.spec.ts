import { TestBed } from '@angular/core/testing';

import { FolderStateService } from './folder-state.service';

describe('FolderStateService', () => {
  let service: FolderStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FolderStateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
