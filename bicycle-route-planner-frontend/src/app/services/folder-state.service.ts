import { Injectable, inject, signal, effect, untracked } from '@angular/core';
import { HttpStatusCode } from '@angular/common/http';
import { Subscription, Subject } from 'rxjs';
import { FolderService } from './folder.service';
import { FolderDto, FolderRequestDto } from '../models/folder.model';
import { LoggerService } from './logger.service';
import { AuthService } from './auth.service';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root',
})
export class FolderStateService {
  private readonly folderService = inject(FolderService);
  private readonly logger = inject(LoggerService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  private fetchFoldersSubscription?: Subscription;

  private readonly _folders = signal<FolderDto[]>([]);
  private readonly _folderDeleted$ = new Subject<number>();

  public readonly folders = this._folders.asReadonly();
  public readonly folderDeleted$ = this._folderDeleted$.asObservable();

  constructor() {
    effect(() => {
      if (this.authService.isAuthenticated()) {
        untracked(() => this.fetchFolders());
      } else {
        this.logger.debug('[FolderState] User logged out, clearing state');
        untracked(() => this.clearState());
      }
    });
  }

  private clearState(): void {
    this.fetchFoldersSubscription?.unsubscribe();
    this._folders.set([]);
  }

  private fetchFolders(): void {
    this.fetchFoldersSubscription?.unsubscribe();

    this.fetchFoldersSubscription = this.folderService
      .getUserFolders()
      .subscribe({
        next: (fetchedFolders) => {
          this._folders.set(fetchedFolders);
        },
        error: (err) => {
          this.logger.error('[FolderState] Error occurred while fetching folders', {
            status: err.status,
            type: err.error?.type,
          });
          this.notificationService.error('ERRORS.FETCH_FOLDERS_FAILED');
        },
      });
  }

  public createFolder(name: string): void {
    const trimmed = name.trim();
    if (!trimmed) {
      return;
    }

    const request: FolderRequestDto = { name: trimmed };
    this.folderService
      .createFolder(request)
      .subscribe({
        next: (createdFolder: FolderDto) => {
          this._folders.update((currentFolders) => [...currentFolders, createdFolder]);

          this.logger.info('[FolderState] Folder successfully created', {
            folderId: createdFolder.id,
            folderName: createdFolder.name,
          });
          this.notificationService.success('SUCCESS.CREATE_FOLDER');
        },
        error: (err) => {
          if (err.status === HttpStatusCode.BadRequest || err.status === HttpStatusCode.Conflict) {
            this.logger.debug('[FolderState] Folder creation validation error', {
              status: err.status,
              type: err.error?.type,
            });
          } else {
            this.logger.error('[FolderState] Error occurred while creating folder', {
              status: err.status,
              type: err.error?.type,
            });
          }
          this.notificationService.error('ERRORS.CREATE_FOLDER_FAILED');
        },
      });
  }

  public deleteFolder(folderId: number): void {
    this.folderService.deleteFolder(folderId).subscribe({
      next: () => {
        this._folders.update((folders) => folders.filter((f) => f.id !== folderId));
        this._folderDeleted$.next(folderId);
        this.logger.info('[FolderState] Folder successfully deleted', {
          deletedFolderId: folderId,
        });
        this.notificationService.success('SUCCESS.DELETE_FOLDER');
      },
      error: (err) => {
        if (err.status === HttpStatusCode.NotFound) {
          this.logger.debug('[FolderState] Folder to delete not found', { folderId });
        } else {
          this.logger.error('[FolderState] Error deleting folder', {
            status: err.status,
            type: err.error?.type,
          });
        }
        this.notificationService.error('ERRORS.DELETE_FOLDER_FAILED');
      },
    });
  }

  public renameFolder(folderId: number, newName: string): void {
    const trimmed = newName.trim();
    if (!trimmed) {
      return;
    }
    const currentFolder = this._folders().find((f) => f.id === folderId);
    if (currentFolder?.name === trimmed) {
      return;
    }

    const request: FolderRequestDto = { name: trimmed };
    this.folderService.renameFolder(folderId, request).subscribe({
      next: () => {
        this._folders.update((folders) =>
          folders.map((f) => (f.id === folderId ? { ...f, name: trimmed } : f)),
        );
        this.logger.info('[FolderState] Folder renamed successfully', {
          folderId,
          newName: trimmed,
        });
        this.notificationService.success('SUCCESS.RENAME_FOLDER');
      },
      error: (err) => {
        if (
          err.status === HttpStatusCode.BadRequest ||
          err.status === HttpStatusCode.NotFound ||
          err.status === HttpStatusCode.Conflict
        ) {
          this.logger.debug('[FolderState] Folder rename validation error', {
            folderId,
            status: err.status,
            type: err.error?.type,
          });
        } else {
          this.logger.error('[FolderState] Error renaming folder', {
            status: err.status,
            type: err.error?.type,
          });
        }
        this.notificationService.error('ERRORS.RENAME_FOLDER_FAILED');
      },
    });
  }
}
