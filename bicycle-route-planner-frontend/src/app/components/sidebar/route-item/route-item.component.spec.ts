import { ComponentFixture, TestBed } from '@angular/core/testing';
import { commonTestProviders } from '../../../testing/test-providers';

import { RouteItemComponent } from './route-item.component';

describe('RouteItemComponent', () => {
  let component: RouteItemComponent;
  let fixture: ComponentFixture<RouteItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouteItemComponent],
      providers: [...commonTestProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(RouteItemComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('route', {
      id: 1,
      name: 'Test Route',
      distanceMeters: 5000,
      folderId: null,
    });
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
