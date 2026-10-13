import { ComponentFixture, TestBed } from '@angular/core/testing';
import { commonTestProviders } from '../../../testing/test-providers';

import { AvoidanceZoneItemComponent } from './avoidance-zone-item.component';

describe('AvoidanceZoneItemComponent', () => {
  let component: AvoidanceZoneItemComponent;
  let fixture: ComponentFixture<AvoidanceZoneItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AvoidanceZoneItemComponent],
      providers: [...commonTestProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(AvoidanceZoneItemComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('zone', {
      id: 1,
      name: 'Test Zone',
      zoneGeom: 'POLYGON((0 0, 0 1, 1 1, 1 0, 0 0))',
      isActive: true,
    });
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
