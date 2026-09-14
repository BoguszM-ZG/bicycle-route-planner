import { Injectable, inject } from '@angular/core';
import { HttpClient} from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment.development';
import { CreateFolderRequestDto, FolderDto, RenameFolderRequestDto } from '../models/folder.model';


@Injectable({
  providedIn: 'root',
})
export class FolderService {

  private readonly apiUrl = `${environment.apiUrl}/folders`;
  private readonly http = inject(HttpClient);

  public getUserFolders(): Observable<FolderDto[]> {
    return this.http.get<FolderDto[]>(this.apiUrl);
  }

  public createFolder(data: CreateFolderRequestDto): Observable<FolderDto> {
    return this.http.post<FolderDto>(this.apiUrl, data);
  }

  public renameFolder(folderId: number, request: RenameFolderRequestDto): Observable<FolderDto> {
    return this.http.patch<FolderDto>(`${this.apiUrl}/${folderId}`, request);
  }

  public deleteFolder(folderId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${folderId}`);
  }

}
