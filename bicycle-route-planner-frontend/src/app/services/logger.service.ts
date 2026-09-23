import { Injectable, Injector, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { LogLevel } from '../models/log-level.enum';
import { environment } from '../../environments/environment';
import { ClientLogRequestDto } from '../models/telemetry.model';

@Injectable({
  providedIn: 'root',
})
export class LoggerService {

  private readonly logLevel: LogLevel = environment.logLevel;

  private readonly injector = inject(Injector);

  public debug(msg: string, ...optionalParams: any[]): void {
    this.logWith(LogLevel.DEBUG, msg, optionalParams);
  }

  public info(msg: string, ...optionalParams: any[]): void {
    this.logWith(LogLevel.INFO, msg, optionalParams);
  }

  public warn(msg: string, ...optionalParams: any[]): void {
    this.logWith(LogLevel.WARN, msg, optionalParams);
  }

  public error(msg: string, ...optionalParams: any[]): void {
    this.logWith(LogLevel.ERROR, msg, optionalParams);
  }


  private logWith(level: LogLevel, msg: string, optionalParams: any[]): void {
    if (level >= this.logLevel) {
      const timestamp = new Date().toISOString();

      const levelName = LogLevel[level];
      const prefix = `[${timestamp}] [${levelName}]`;

      switch (level) {
        case LogLevel.DEBUG:
          console.debug(prefix, msg, ...optionalParams);
          break;
        case LogLevel.INFO:
          console.info(prefix, msg, ...optionalParams);
          break;
        case LogLevel.WARN:
          console.warn(prefix, msg, ...optionalParams);
          break;
        case LogLevel.ERROR:
          console.error(prefix, msg, ...optionalParams);
          break;
      }

      if (level >= LogLevel.WARN) {
        this.sendToServer(levelName, msg, timestamp, optionalParams);
      }
    }
  }


  private sendToServer(
    levelName: string,
    msg: string,
    timestamp: string,
    optionalParams: any[],
  ): void {
    try {
      const http = this.injector.get(HttpClient);

      let parsedDetails: string | undefined;

      if (optionalParams.length > 0) {
        try {
          parsedDetails = JSON.stringify(optionalParams);
        } catch {
          parsedDetails = String(optionalParams);
        }
      }


      const logRequest: ClientLogRequestDto = {
        level: levelName as keyof typeof LogLevel,
        message: msg,
        timestamp: timestamp,
        details: parsedDetails,
      };

      const telemetryUrl = `${environment.apiUrl}/telemetry/logs`;
      http.post(telemetryUrl, logRequest).subscribe({
        error: () => console.error('Error sending log to server:', logRequest),
      });
    } catch (e) {
      console.error('Error in LoggerService:', e);
    }
  }
}
