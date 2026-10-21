import { ErrorHandler, Injectable, Injector, NgZone, inject } from '@angular/core';
import { LoggerService } from '../../services/logger.service';
import { NotificationService } from '../../services/notification.service';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly injector = inject(Injector);
  private readonly zone = inject(NgZone);

  public handleError(error: unknown): void {
    const unwrappedError = this.findOriginalError(error);

    const logger = this.injector.get(LoggerService);
    const notificationService = this.injector.get(NotificationService);

    const message =
      unwrappedError instanceof Error
        ? unwrappedError.message
        : typeof unwrappedError === 'object' && unwrappedError !== null && 'message' in unwrappedError
          ? String((unwrappedError as { message: unknown }).message)
          : String(unwrappedError);

    logger.error('Unhandled application error:', message, unwrappedError);

    if (unwrappedError instanceof HttpErrorResponse) {
      return;
    }

    this.zone.run(() => {
      notificationService.error('ERRORS.GLOBAL_UNKNOWN_ERROR');
    });
  }

  private findOriginalError(error: unknown): unknown {
    let currentError: unknown = error;
    while (
      typeof currentError === 'object' &&
      currentError !== null &&
      'originalError' in currentError &&
      (currentError as { originalError: unknown }).originalError
    ) {
      currentError = (currentError as { originalError: unknown }).originalError;
    }
    if (
      typeof currentError === 'object' &&
      currentError !== null &&
      'rejection' in currentError &&
      (currentError as { rejection: unknown }).rejection
    ) {
      return (currentError as { rejection: unknown }).rejection;
    }
    return currentError;
  }
}
