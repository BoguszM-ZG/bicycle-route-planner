import { LogLevel } from '../app/models/log-level.enum';

export const environment = {
  production: true,
  apiUrl: 'http://localhost:8080/api',
  logLevel: LogLevel.ERROR,
};
