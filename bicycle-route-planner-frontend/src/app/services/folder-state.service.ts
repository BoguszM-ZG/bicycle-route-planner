import { Injectable, inject, signal, effect } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { FolderService } from './folder.service';
import { CreateFolderRequestDto, FolderDto, RenameFolderRequestDto } from '../models/folder.model';
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

  private readonly _folders = signal<FolderDto[]>([]);
  private readonly _isLoading = signal<boolean>(false);

  public readonly folders = this._folders.asReadonly();
  public readonly isLoading = this._isLoading.asReadonly();

  constructor() {
    effect(() => {
      const user = this.authService.currentUser();
      if (!user) {
        this.logger.debug('User logged out, clearing FolderStateService');
        this.clearState();
      }
    });
  }

  private clearState(): void {
    this._folders.set([]);
    this._isLoading.set(false);
  }

  public fetchFolders(): void {
    this._isLoading.set(true);

    this.folderService
      .getUserFolders()
      .pipe(finalize(() => this._isLoading.set(false)))
      .subscribe({
        next: (fetchedFolders) => {
          this._folders.set(fetchedFolders);
        },
        error: (err) => {
          this.logger.error('Error occurred while fetching folders.', err);
          this.notificationService.error('ERRORS.FETCH_FOLDERS_FAILED');        },
      });
  }

  public createFolder(request: CreateFolderRequestDto): void {
    this._isLoading.set(true);

    this.folderService
      .createFolder(request)
      .pipe(finalize(() => this._isLoading.set(false)))
      .subscribe({
        next: (createdFolder: FolderDto) => {
          this._folders.update((currentFolders) => [...currentFolders, createdFolder]);
        },
        error: (err) => {
          this.logger.error('Error occurred while creating folder.', err);
          this.notificationService.error('ERRORS.CREATE_FOLDER_FAILED');
        },
      });
  }

  public deleteFolder(folderId: number): void {
    this.folderService.deleteFolder(folderId).subscribe({
      next: () => {
        this._folders.update((folders) => folders.filter((f) => f.id !== folderId));
      },
      error: (err) => {
        this.logger.error('Error deleting folder', err);
        this.notificationService.error('ERRORS.DELETE_FOLDER_FAILED');
      },
    });
  }
  public renameFolder(folderId: number, newName: string): void {
    const request: RenameFolderRequestDto = { name: newName };
    this.folderService.renameFolder(folderId, request).subscribe({
      next: (updatedFolder) => {
        this._folders.update((folders) =>
          folders.map((f) => (f.id === folderId ? { ...f, name: updatedFolder.name } : f)),
        );
      },
      error: (err) => {
        this.notificationService.error('ERRORS.RENAME_FOLDER_FAILED');
        this.logger.error('Error renaming folder', err);
      },
    });
  }
}
