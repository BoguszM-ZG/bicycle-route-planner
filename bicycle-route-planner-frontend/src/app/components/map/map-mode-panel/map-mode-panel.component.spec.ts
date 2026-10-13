import { ComponentFixture, TestBed } from '@angular/core/testing';
import { commonTestProviders } from '../../../testing/test-providers';

import { MapModePanelComponent } from './map-mode-panel.component';

describe('MapModePanelComponent', () => {
  let component: MapModePanelComponent;
  let fixture: ComponentFixture<MapModePanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MapModePanelComponent],
      providers: [...commonTestProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(MapModePanelComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
