import { ErrorHandler, Injectable, Injector, NgZone, inject } from '@angular/core';
import { LoggerService } from '../../services/logger.service';
import { NotificationService } from '../../services/notification.service';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly injector = inject(Injector);
  private readonly zone = inject(NgZone);

  public handleError(error: any): void {
    const unwrappedError = this.findOriginalError(error);

    const logger = this.injector.get(LoggerService);
    const notificationService = this.injector.get(NotificationService);

    const message = unwrappedError.message ? unwrappedError.message : unwrappedError.toString();


    logger.error('Unhandled application error:', message, unwrappedError);


    if (unwrappedError instanceof HttpErrorResponse) {
      return;
    }

    this.zone.run(() => {
      notificationService.error('ERRORS.GLOBAL_UNKNOWN_ERROR');
    });
  }

  private findOriginalError(error: any): any {
    while (error?.originalError) {
      error = error.originalError;
    }
    if (error?.rejection) {
      return error.rejection;
    }
    return error;
  }
}
