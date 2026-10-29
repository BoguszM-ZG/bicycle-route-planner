import { Injectable, inject } from '@angular/core';
import { HttpClient} from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { FolderDto, FolderRequestDto } from '../models/folder.model';


@Injectable({
  providedIn: 'root',
})
export class FolderService {

  private readonly apiUrl = `${environment.apiUrl}/folders`;
  private readonly http = inject(HttpClient);

  public getUserFolders(): Observable<FolderDto[]> {
    return this.http.get<FolderDto[]>(this.apiUrl);
  }

  public createFolder(request: FolderRequestDto): Observable<FolderDto> {
    return this.http.post<FolderDto>(this.apiUrl, request);
  }

  public renameFolder(folderId: number, request: FolderRequestDto): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${folderId}`, request);
  }

  public deleteFolder(folderId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${folderId}`);
  }

}
