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

  public debug(msg: string, ...optionalParams: unknown[]): void {
    this.logWith(LogLevel.DEBUG, msg, optionalParams);
  }

  public info(msg: string, ...optionalParams: unknown[]): void {
    this.logWith(LogLevel.INFO, msg, optionalParams);
  }

  public error(msg: string, ...optionalParams: unknown[]): void {
    this.logWith(LogLevel.ERROR, msg, optionalParams);
  }

  private logWith(level: LogLevel, msg: string, optionalParams: unknown[]): void {
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
    optionalParams: unknown[],
  ): void {
    try {
      const http = this.injector.get(HttpClient);

      let parsedDetails: string | undefined;

      if (optionalParams.length > 0) {
        try {
          parsedDetails = this.safeStringify(optionalParams);
        } catch {
          parsedDetails = String(optionalParams);
        }
      }

      const logRequest: ClientLogRequestDto = {
        level: levelName as keyof typeof LogLevel,
        message: (msg ?? '').trim().slice(0, 490) || '[Empty message]',
        timestamp: timestamp,
        details: parsedDetails ? parsedDetails.slice(0, 1990) : undefined,
      };

      const telemetryUrl = `${environment.apiUrl}/telemetry/logs`;
      http.post(telemetryUrl, logRequest).subscribe({
        error: () => console.error('Error sending log to server:', logRequest),
      });
    } catch (e) {
      console.error('Error in LoggerService:', e);
    }
  }

  private safeStringify(data: unknown): string {
    const cache = new Set();
    return JSON.stringify(data, (_key, value) => {
      if (value instanceof Error) {
        return { name: value.name, message: value.message, stack: value.stack };
      }
      if (typeof value === 'object' && value !== null) {
        if (cache.has(value)) {
          return '[Circular Reference]';
        }
        cache.add(value);
      }
      return value;
    });
  }
}
