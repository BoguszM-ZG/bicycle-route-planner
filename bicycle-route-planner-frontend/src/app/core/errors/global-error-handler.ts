import { ErrorHandler, Injectable, Injector, inject } from '@angular/core';
import { LoggerService } from '../../services/logger.service';
import { NotificationService } from '../../services/notification.service';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly injector = inject(Injector);

  public handleError(error: unknown): void {
    const unwrappedError = this.findOriginalError(error);

    const logger = this.injector.get(LoggerService);
    const notificationService = this.injector.get(NotificationService);

    const message =
      unwrappedError instanceof Error
        ? unwrappedError.message
        : typeof unwrappedError === 'object' &&
            unwrappedError !== null &&
            'message' in unwrappedError
          ? String((unwrappedError as { message: unknown }).message)
          : String(unwrappedError);

    let originalError: string;
    try {
      if (unwrappedError instanceof Error) {
        originalError = unwrappedError.stack ?? unwrappedError.message;
      } else if (typeof unwrappedError === 'object' && unwrappedError !== null) {
        originalError = JSON.stringify(unwrappedError);
      } else {
        originalError = String(unwrappedError);
      }
    } catch {
      originalError = String(unwrappedError);
    }

    logger.error(`[GlobalError] ${message}`, { originalError });

    if (unwrappedError instanceof HttpErrorResponse) {
      return;
    }

    notificationService.error('ERRORS.GLOBAL_UNKNOWN_ERROR');
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
