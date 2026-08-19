import { describe, beforeEach, it, expect } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SaveRouteModalComponent } from './save-route-modal.component';

describe('SaveRouteModalComponent', () => {
  let component: SaveRouteModalComponent;
  let fixture: ComponentFixture<SaveRouteModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SaveRouteModalComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SaveRouteModalComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
