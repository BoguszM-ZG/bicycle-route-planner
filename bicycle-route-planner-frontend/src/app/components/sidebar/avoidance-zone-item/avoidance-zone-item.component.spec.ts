import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AvoidanceZoneItemComponent } from './avoidance-zone-item.component';

describe('AvoidanceZoneItemComponent', () => {
  let component: AvoidanceZoneItemComponent;
  let fixture: ComponentFixture<AvoidanceZoneItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AvoidanceZoneItemComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AvoidanceZoneItemComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
