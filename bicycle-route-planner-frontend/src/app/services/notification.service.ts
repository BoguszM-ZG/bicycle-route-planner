import { Injectable, signal } from '@angular/core';
import { Notification, NotificationType } from '../models/notification.model';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly _notifications = signal<Notification[]>([]);
  public readonly notifications = this._notifications.asReadonly();

  private idCounter = 0;

  show(message: string, type: NotificationType = 'info', durationMs: number = 4000) {
    const id = this.idCounter++;
    this._notifications.update((n) => [...n, { id, message, type }]);

    if (durationMs > 0) {
      setTimeout(() => {
        this.remove(id);
      }, durationMs);
    }
  }

  success(message: string, durationMs?: number) {
    this.show(message, 'success', durationMs);
  }
  error(message: string, durationMs: number = 6000) {
    this.show(message, 'error', durationMs);
  }
  info(message: string, durationMs?: number) {
    this.show(message, 'info', durationMs);
  }
  warning(message: string, durationMs?: number) {
    this.show(message, 'warning', durationMs);
  }

  remove(id: number) {
    this._notifications.update((n) => n.filter((x) => x.id !== id));
  }
}
