import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouteStateService } from '../../services/route-state.service';
import { NotificationService } from '../../services/notification.service';
import { RouteDraft, CreateRouteRequestDto } from '../../models/route.model';
import { FolderStateService } from '../../services/folder-state.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-save-route-modal',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './save-route-modal.component.html',
  styleUrl: './save-route-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SaveRouteModalComponent {
  private readonly routeStateService = inject(RouteStateService);
  private readonly folderStateService = inject(FolderStateService);
  private readonly notificationService = inject(NotificationService);
  private readonly translate = inject(TranslateService);

  public isVisible = signal<boolean>(false);
  public isLoading = this.routeStateService.isSaving;
  public folders = this.folderStateService.folders;
  public selectedFolderId: number | null = null;

  public routeName: string = '';
  private routeDataToSave: RouteDraft | null = null;

  public open(routeData: RouteDraft): void {
    this.routeDataToSave = routeData;
    this.routeName = 'Moja trasa rowerowa';
    this.isVisible.set(true);
  }

  public close(): void {
    this.isVisible.set(false);
    this.routeName = '';
    this.routeDataToSave = null;
    this.selectedFolderId = null;
  }

  public saveRoute(): void {
    if (this.routeName.trim() === '') {
      this.notificationService.warning(this.translate.instant('MODALS.EMPTY_ROUTE_NAME'));
      return;
    }
    if (!this.routeDataToSave || this.isLoading()) {
      return;
    }

    const dto: CreateRouteRequestDto = {
      name: this.routeName.trim(),
      routeType: this.routeDataToSave.routeType || 'cycling',
      targetDistance: this.routeDataToSave.targetDistance || 0,
      distanceMeters: this.routeDataToSave.distanceMeters || 0,
      elevationGain: this.routeDataToSave.elevationGain || 0,
      elevationLoss: this.routeDataToSave.elevationLoss || 0,
      startPoint: this.routeDataToSave.startPoint || '',
      routeGeom: this.routeDataToSave.geometryWkt || '',
      folderId: this.selectedFolderId,
    };

    this.routeStateService.createRoute(dto, () => {
      this.notificationService.success(this.translate.instant('MODALS.ROUTE_SAVED'));
      this.close();
    });
  }
}
