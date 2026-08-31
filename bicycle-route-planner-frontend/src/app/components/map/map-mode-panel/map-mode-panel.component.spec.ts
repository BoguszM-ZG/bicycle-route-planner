import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MapModePanelComponent } from './map-mode-panel.component';

describe('MapModePanelComponent', () => {
  let component: MapModePanelComponent;
  let fixture: ComponentFixture<MapModePanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MapModePanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MapModePanelComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
