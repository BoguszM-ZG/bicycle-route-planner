import { LogLevel } from './log-level.enum';

export interface ClientLogRequestDto {
  level: keyof typeof LogLevel;
  message: string;
  timestamp: string;
  details?: string;
}
