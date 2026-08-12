import { Routes } from '@angular/router';
import { Register } from './components/register/register';
import { MapComponent } from './components/map/map.component';
import { LoginComponent } from './components/login/login.component';

export const routes: Routes = [
  { path: '', component: MapComponent },
  { path: 'register', component: Register },
  { path: 'login', component: LoginComponent }
];
