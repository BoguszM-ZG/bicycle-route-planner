import { LogLevel } from '../app/models/log-level.enum';

export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  logLevel: LogLevel.DEBUG,
};
