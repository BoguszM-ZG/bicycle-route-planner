import { ErrorHandler, Injectable, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { LoggerService } from '../../services/logger.service';
import { NotificationService } from '../../services/notification.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly logger = inject(LoggerService);
  private readonly notificationService = inject(NotificationService);

  public handleError(error: unknown): void {
    if (error instanceof HttpErrorResponse) {
      this.logger.error('[GlobalError] Unhandled HTTP error', {
        status: error.status,
        type: error.error?.type,
      });
      return;
    }

    const message = error instanceof Error ? error.message : String(error);
    const originalError = error instanceof Error ? (error.stack ?? error.message) : String(error);

    this.logger.error(`[GlobalError] ${message}`, { originalError });
    this.notificationService.error('ERRORS.GLOBAL_UNKNOWN_ERROR');
  }
}
