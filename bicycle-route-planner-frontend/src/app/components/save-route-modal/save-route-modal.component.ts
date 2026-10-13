import {
  Component,
  inject,
  signal,
  ChangeDetectionStrategy,
  effect,
  ElementRef,
  viewChild,
} from '@angular/core';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { RouteStateService } from '../../services/route-state.service';
import { NotificationService } from '../../services/notification.service';
import { RouteDraft, CreateRouteRequestDto } from '../../models/route.model';
import { FolderStateService } from '../../services/folder-state.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-save-route-modal',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './save-route-modal.component.html',
  styleUrl: './save-route-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SaveRouteModalComponent {
  private readonly routeStateService = inject(RouteStateService);
  private readonly folderStateService = inject(FolderStateService);
  private readonly notificationService = inject(NotificationService);
  private readonly translate = inject(TranslateService);
  private readonly logger = inject(LoggerService);

  public isVisible = signal<boolean>(false);
  public isLoading = this.routeStateService.isSaving;
  public folders = this.folderStateService.folders;

  public routeNameControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(150)],
  });

  public selectedFolderControl = new FormControl<number | null>(null);

  private routeDataToSave: RouteDraft | null = null;

  public routeInput = viewChild<ElementRef<HTMLInputElement>>('routeInput');

  constructor() {
    effect(() => {
      if (this.isVisible()) {
        setTimeout(() => this.routeInput()?.nativeElement.focus(), 0);
      }
    });
  }

  public open(routeData: RouteDraft): void {
    this.routeDataToSave = routeData;
    this.routeNameControl.setValue(this.translate.instant('MODALS.DEFAULT_ROUTE_NAME'));
    this.isVisible.set(true);
  }

  public close(): void {
    this.isVisible.set(false);
    this.routeNameControl.reset();
    this.selectedFolderControl.reset();
    this.routeDataToSave = null;
  }

  public saveRoute(): void {
    if (this.routeNameControl.invalid) {
      this.routeNameControl.markAsTouched();
      this.logger.debug(
        '[SaveRouteModal] Attempt to save an invalid form',
        this.routeNameControl.errors,
      );
      this.notificationService.warning('MODALS.EMPTY_ROUTE_NAME');
      return;
    }

    if (!this.routeDataToSave || this.isLoading()) {
      return;
    }

    if (!this.routeDataToSave.distanceMeters || this.routeDataToSave.distanceMeters <= 0) {
      this.notificationService.warning('MAP.DRAW_ROUTE_FIRST');
      return;
    }

    const dto: CreateRouteRequestDto = {
      name: this.routeNameControl.value.trim(),
      routeType: this.routeDataToSave.routeType || 'POINT_TO_POINT',
      targetDistance: this.routeDataToSave.targetDistance || 0,
      distanceMeters: this.routeDataToSave.distanceMeters || 0,
      elevationGain: this.routeDataToSave.elevationGain || 0,
      elevationLoss: this.routeDataToSave.elevationLoss || 0,
      startPoint: this.routeDataToSave.startPoint || '',
      routeGeom: this.routeDataToSave.geometryWkt || '',
      folderId: this.selectedFolderControl.value,
    };

    this.routeStateService.createRoute(dto, () => {
      this.notificationService.success('MODALS.ROUTE_SAVED');
      this.close();
    });
  }
}
