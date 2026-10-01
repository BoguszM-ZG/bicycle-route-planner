import { Component, inject, ChangeDetectionStrategy, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '../../services/auth.service';
import { RoutePanelComponent } from './route-panel/route-panel.component';
import { AvoidanceZonePanelComponent } from './avoidance-zone-panel/avoidance-zone-panel.component';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RoutePanelComponent, AvoidanceZonePanelComponent, TranslatePipe],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent {
  private readonly authService = inject(AuthService);

  public readonly currentUser = this.authService.currentUser;
  public readonly activeTab = signal<'routes' | 'zones'>('routes');

  public logout(): void {
    this.authService.logout();
  }
}
