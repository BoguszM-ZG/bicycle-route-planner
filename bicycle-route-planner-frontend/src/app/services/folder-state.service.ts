import { Injectable, inject, signal, effect } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { FolderService } from './folder.service';
import { CreateFolderRequestDto, FolderDto, RenameFolderRequestDto } from '../models/folder.model';
import { LoggerService } from './logger.service';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class FolderStateService {
  private readonly folderService = inject(FolderService);
  private readonly logger = inject(LoggerService);
  private readonly authService = inject(AuthService);

  private readonly _folders = signal<FolderDto[]>([]);
  private readonly _isLoading = signal<boolean>(false);
  private readonly _error = signal<string | null>(null);

  public readonly folders = this._folders.asReadonly();
  public readonly isLoading = this._isLoading.asReadonly();
  public readonly error = this._error.asReadonly();

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
    this._error.set(null);
  }

  public fetchFolders(): void {
    this._isLoading.set(true);
    this._error.set(null);

    this.folderService
      .getUserFolders()
      .pipe(finalize(() => this._isLoading.set(false)))
      .subscribe({
        next: (fetchedFolders) => {
          this._folders.set(fetchedFolders);
        },
        error: (err) => {
          this.logger.error('Error occurred while fetching folders.', err);
          this._error.set('Error occurred while fetching folders.');
        },
      });
  }

  public createFolder(request: CreateFolderRequestDto): void {
    this._isLoading.set(true);
    this._error.set(null);

    this.folderService
      .createFolder(request)
      .pipe(finalize(() => this._isLoading.set(false)))
      .subscribe({
        next: (createdFolder: FolderDto) => {
          this._folders.update((currentFolders) => [...currentFolders, createdFolder]);
        },
        error: (err) => {
          this.logger.error('Error occurred while creating folder.', err);
          this._error.set('Error occurred while creating folder.');
        },
      });
  }

  public deleteFolder(folderId: number): void {
    this.folderService.deleteFolder(folderId).subscribe({
      next: () => {
        this._folders.update((folders) => folders.filter((f) => f.id !== folderId));
      },
      error: (err) => {
        this._error.set('Error occurred while deleting folder.');
        this.logger.error('Error deleting folder', err);
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
        this._error.set('Error occurred while renaming folder.');
        this.logger.error('Error renaming folder', err);
      },
    });
  }
}
