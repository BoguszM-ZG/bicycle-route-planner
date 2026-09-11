import { Injectable, inject, signal } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { FolderService } from './folder.service';
import { CreateFolderRequestDto, FolderDto } from '../models/folder.model';

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
}
