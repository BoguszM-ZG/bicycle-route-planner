import { ComponentFixture, TestBed } from '@angular/core/testing';
import { commonTestProviders } from '../../../testing/test-providers';

import { FolderItemComponent } from './folder-item.component';

describe('FolderItemComponent', () => {
  let component: FolderItemComponent;
  let fixture: ComponentFixture<FolderItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FolderItemComponent],
      providers: [...commonTestProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(FolderItemComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('folderId', 1);
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
