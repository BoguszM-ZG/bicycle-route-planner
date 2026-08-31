import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AvoidanceZonePanelComponent } from './avoidance-zone-panel.component';

describe('AvoidanceZonePanelComponent', () => {
  let component: AvoidanceZonePanelComponent;
  let fixture: ComponentFixture<AvoidanceZonePanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AvoidanceZonePanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AvoidanceZonePanelComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
