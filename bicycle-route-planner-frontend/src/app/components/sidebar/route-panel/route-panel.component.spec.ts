import { ComponentFixture, TestBed } from '@angular/core/testing';
import { commonTestProviders } from '../../../testing/test-providers';

import { RoutePanelComponent } from './route-panel.component';

describe('RoutePanelComponent', () => {
  let component: RoutePanelComponent;
  let fixture: ComponentFixture<RoutePanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoutePanelComponent],
      providers: [...commonTestProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(RoutePanelComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
