import { Injectable, signal, inject, NgZone } from '@angular/core';
import { Notification, NotificationType } from '../models/notification.model';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly ngZone = inject(NgZone); 

  private readonly _notifications = signal<Notification[]>([]);
  public readonly notifications = this._notifications.asReadonly();

  private idCounter = 0;

  show(
    messageKey: string, 
    type: NotificationType = 'info', 
    durationMs: number = 4000,
    translationParams?: Record<string, unknown>
  ) {
    const id = this.idCounter++;
    
    this._notifications.update((n) => [...n, { 
      id, 
      message: messageKey,
      type,
      translationParams
    }]);

    if (durationMs > 0) {
      this.ngZone.runOutsideAngular(() => {
        setTimeout(() => {
          this.remove(id);
        }, durationMs);
      });
    }
  }

  success(messageKey: string, params?: Record<string, unknown>, durationMs?: number) {
    this.show(messageKey, 'success', durationMs, params);
  }
  
  error(messageKey: string, params?: Record<string, unknown>, durationMs: number = 6000) {
    this.show(messageKey, 'error', durationMs, params);
  }
  
  info(messageKey: string, params?: Record<string, unknown>, durationMs?: number) {
    this.show(messageKey, 'info', durationMs, params);
  }
  
  warning(messageKey: string, params?: Record<string, unknown>, durationMs?: number) {
    this.show(messageKey, 'warning', durationMs, params);
  }

  remove(id: number) {
    this._notifications.update((n) => n.filter((x) => x.id !== id));
  }
}
