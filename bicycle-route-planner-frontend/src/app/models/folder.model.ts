export interface FolderDto {
  id: number;
  name: string;
  parentFolderId?: number | null;
}

export interface CreateFolderRequestDto {
  name: string;
  parentFolderId?: number | null;
}

export interface RenameFolderRequestDto {
  name: string;
}
