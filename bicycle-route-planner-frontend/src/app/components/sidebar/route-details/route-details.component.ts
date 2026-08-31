import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import {RouteDetailDto} from '../../../models/route.model';


@Component({
  selector: 'app-route-details',
  imports: [DecimalPipe, DatePipe],
  templateUrl: './route-details.component.html',
  styleUrl: './route-details.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RouteDetailsComponent {
  routeDetail = input.required<RouteDetailDto>();


}
