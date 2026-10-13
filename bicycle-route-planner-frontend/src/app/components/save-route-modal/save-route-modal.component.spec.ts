import { ComponentFixture, TestBed } from '@angular/core/testing';
import { commonTestProviders } from '../../testing/test-providers';

import { SaveRouteModalComponent } from './save-route-modal.component';

describe('SaveRouteModalComponent', () => {
  let component: SaveRouteModalComponent;
  let fixture: ComponentFixture<SaveRouteModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SaveRouteModalComponent],
      providers: [...commonTestProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(SaveRouteModalComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
