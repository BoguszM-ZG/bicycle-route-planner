import { Injectable, signal } from '@angular/core';
import { Notification, NotificationType } from '../models/notification.model';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private static readonly DEFAULT_DURATION_MS = 4000;
  private static readonly ERROR_DURATION_MS = 6000;
  private static readonly MAX_VISIBLE_NOTIFICATIONS = 5;
  private readonly _notifications = signal<Notification[]>([]);
  public readonly notifications = this._notifications.asReadonly();
  private readonly activeTimers = new Map<number, ReturnType<typeof setTimeout>>();
  private idCounter = 0;

  public success(messageKey: string, params?: Record<string, unknown>): void {
    this.show(messageKey, 'success', params);
  }

  public error(messageKey: string, params?: Record<string, unknown>): void {
    this.show(messageKey, 'error', params, NotificationService.ERROR_DURATION_MS);
  }

  public info(messageKey: string, params?: Record<string, unknown>): void {
    this.show(messageKey, 'info', params);
  }

  public warning(messageKey: string, params?: Record<string, unknown>): void {
    this.show(messageKey, 'warning', params);
  }

  public remove(id: number): void {
    const timer = this.activeTimers.get(id);
    if (timer) {
      clearTimeout(timer);
      this.activeTimers.delete(id);
    }
    this._notifications.update((n) => n.filter((x) => x.id !== id));
  }

  private show(
    messageKey: string,
    type: NotificationType,
    translationParams?: Record<string, unknown>,
    durationMs: number = NotificationService.DEFAULT_DURATION_MS,
  ): void {
    const id = this.idCounter++;

    this._notifications.update((n) => {
      const updated = [
        ...n,
        {
          id,
          message: messageKey,
          type,
          translationParams,
        },
      ];
      if (updated.length > NotificationService.MAX_VISIBLE_NOTIFICATIONS) {
        const excess = updated.length - NotificationService.MAX_VISIBLE_NOTIFICATIONS;
        const evicted = updated.splice(0, excess);
        for (const item of evicted) {
          const timer = this.activeTimers.get(item.id);
          if (timer) {
            clearTimeout(timer);
            this.activeTimers.delete(item.id);
          }
        }
      }
      return updated;
    });

    const timer = setTimeout(() => {
      this.activeTimers.delete(id);
      this.remove(id);
    }, durationMs);
    this.activeTimers.set(id, timer);
  }
}
