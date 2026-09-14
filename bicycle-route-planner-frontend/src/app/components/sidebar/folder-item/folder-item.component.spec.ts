import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FolderItemComponent } from './folder-item.component';

describe('FolderItemComponent', () => {
  let component: FolderItemComponent;
  let fixture: ComponentFixture<FolderItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FolderItemComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(FolderItemComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
