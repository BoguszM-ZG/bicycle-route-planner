import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SaveZoneModalComponent } from './save-zone-modal.component';

describe('SaveZoneModalComponent', () => {
  let component: SaveZoneModalComponent;
  let fixture: ComponentFixture<SaveZoneModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SaveZoneModalComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SaveZoneModalComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
