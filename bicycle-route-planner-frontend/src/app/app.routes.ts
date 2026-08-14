import { Routes } from '@angular/router';
import { Register } from './components/register/register';
import { MapComponent } from './components/map/map.component';
import { LoginComponent } from './components/login/login.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', component: MapComponent, canActivate: [authGuard] },
  { path: 'register', component: Register },
  { path: 'login', component: LoginComponent }
];
