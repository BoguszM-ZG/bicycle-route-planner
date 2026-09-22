import { Injectable, signal } from '@angular/core';

export type NotificationType = 'success' | 'error' | 'info' | 'warning';

export interface Notification {
  id: number;
  message: string;
  type: NotificationType;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  public notifications = signal<Notification[]>([]);
  private idCounter = 0;

  show(message: string, type: NotificationType = 'info') {
    const id = this.idCounter++;
    this.notifications.update(n => [...n, { id, message, type }]);

    setTimeout(() => {
      this.remove(id);
    }, 4000);
  }

  success(message: string) { this.show(message, 'success'); }
  error(message: string) { this.show(message, 'error'); }
  info(message: string) { this.show(message, 'info'); }
  warning(message: string) { this.show(message, 'warning'); }

  remove(id: number) {
    this.notifications.update(n => n.filter(x => x.id !== id));
  }
}
