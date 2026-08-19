import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from 'rxjs';
import { RouteDetailDto} from '../models/route.model';

@Injectable({
  providedIn: 'root',
})
export class RouteStateService {
  private readonly selectedRouteSubject = new BehaviorSubject<RouteDetailDto | null>(null);

  public selectedRoute$: Observable<RouteDetailDto | null> =
    this.selectedRouteSubject.asObservable();

  public selectRoute(route: RouteDetailDto | null): void {
    this.selectedRouteSubject.next(route);
  }
}
