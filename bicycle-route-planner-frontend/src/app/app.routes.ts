import { Routes } from '@angular/router';
import { Register } from './components/register/register';
import { MapComponent } from './components/map/map.component';

export const routes: Routes = [
  { path: '', component: MapComponent },
  { path: 'register', component: Register }
];
