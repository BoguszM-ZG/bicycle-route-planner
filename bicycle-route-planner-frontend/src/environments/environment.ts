import { LogLevel } from '../app/models/log-level.enum';

export const environment = {
  production: true,
  apiUrl: '/api',
  logLevel: LogLevel.ERROR,
};
