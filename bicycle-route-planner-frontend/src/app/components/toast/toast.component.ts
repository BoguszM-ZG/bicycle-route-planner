import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-toast',
  imports: [TranslatePipe],
  templateUrl: './toast.component.html',
  styleUrl: './toast.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastComponent {
  private readonly notificationService = inject(NotificationService);
  public readonly notifications = this.notificationService.notifications;

  public removeToast(id: number): void {
    this.notificationService.remove(id);
  }
}
