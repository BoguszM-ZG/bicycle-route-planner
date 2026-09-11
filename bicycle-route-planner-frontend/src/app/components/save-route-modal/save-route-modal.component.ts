import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouteStateService } from '../../services/route-state.service';
import { RouteDraft, CreateRouteRequestDto } from '../../models/route.model';

@Component({
  selector: 'app-save-route-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './save-route-modal.component.html',
  styleUrl: './save-route-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SaveRouteModalComponent {
  private readonly routeStateService = inject(RouteStateService);

  public isVisible = signal<boolean>(false);
  public isLoading = this.routeStateService.isSaving;

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
  }

  public saveRoute(): void {
    if (this.routeName.trim() === '') {
      alert('Proszę wpisać nazwę trasy');
      return;
    }
    if (!this.routeDataToSave || this.isLoading()) {
      return;
    }

    const dto: CreateRouteRequestDto = {
      name: this.routeName.trim(),
      routeType: this.routeDataToSave.routeType || 'cycling',
      targetDistance: this.routeDataToSave.distanceMeters || 0,
      distanceMeters: this.routeDataToSave.distanceMeters || 0,
      elevationGain: this.routeDataToSave.elevationGain || 0,
      elevationLoss: this.routeDataToSave.elevationLoss || 0,
      startPoint: this.routeDataToSave.startPoint || '',
      routeGeom: this.routeDataToSave.geometryWkt || '',
      folderId: null,
    };

    this.routeStateService.createRoute(dto, () => {
      alert('Trasa została zapisana pomyślnie!');
      this.close();
    });
  }
}
