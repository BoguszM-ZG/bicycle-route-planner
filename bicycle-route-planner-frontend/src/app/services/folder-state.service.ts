import { Injectable, inject, signal } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { FolderService } from './folder.service';
import { CreateFolderRequestDto, FolderDto, RenameFolderRequestDto } from '../models/folder.model';

@Injectable({
  providedIn: 'root',
})
export class FolderStateService {
  private readonly folderService = inject(FolderService);

  private readonly _folders = signal<FolderDto[]>([]);
  private readonly _isLoading = signal<boolean>(false);
  private readonly _error = signal<string | null>(null);

  public readonly folders = this._folders.asReadonly();
  public readonly isLoading = this._isLoading.asReadonly();
  public readonly error = this._error.asReadonly();

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
          console.error('Error occurred while fetching folders.', err);
          this._error.set('Nie udało się pobrać folderów. Spróbuj ponownie później.');
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
          console.error('Error occurred while creating folder.', err);
          this._error.set('Nie udało się utworzyć folderu.');
        },
      });
  }

  public deleteFolder(folderId: number): void {
    this.folderService.deleteFolder(folderId).subscribe({
      next: () => {
        this._folders.update((folders) => folders.filter((f) => f.id !== folderId));

      },
      error: (err) => {
        console.error('Error deleting folder', err);
      },
    });
  }
  public renameFolder(folderId: number, newName: string): void {
    const request : RenameFolderRequestDto = { name: newName };
    this.folderService.renameFolder(folderId, request).subscribe({
      next: (updatedFolder) => {
        this._folders.update((folders) =>
          folders.map((f) => (f.id === folderId ? { ...f, name: updatedFolder.name } : f)),
        );
      },
      error: (err) => {
        console.error('Error renaming folder', err);
      },
    });
  }
}
