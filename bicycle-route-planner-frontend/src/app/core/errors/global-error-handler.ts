import { ErrorHandler, Injectable, Injector, inject } from '@angular/core';
import { LoggerService } from '../../services/logger.service';
import { NotificationService } from '../../services/notification.service';
import { TranslateService } from '@ngx-translate/core';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {

  private readonly injector = inject(Injector);

  public handleError(error: any): void {


    const logger = this.injector.get(LoggerService);
    const notificationService = this.injector.get(NotificationService);
    const translate = this.injector.get(TranslateService);

    const message = error.message ? error.message : error.toString();

    logger.error('Unhandled application error:', message, error);

    const translatedErrorMessage = translate.instant('ERRORS.GLOBAL_UNKNOWN_ERROR');
    notificationService.error(translatedErrorMessage);
  }
}
