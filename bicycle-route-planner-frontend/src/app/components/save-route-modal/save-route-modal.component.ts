import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouteService} from '../../services/route.service';
import { RouteDraft} from '../../models/route.model';

@Component({
  selector: 'app-save-route-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './save-route-modal.component.html',
  styleUrl: './save-route-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SaveRouteModalComponent {
  private readonly routeService = inject(RouteService);
  public isVisible = signal<boolean>(false);
  public isLoading = signal<boolean>(false);

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
    if (!this.routeDataToSave) {
      return;
    }

    this.isLoading.set(true);

    this.routeService
      .addRoute({
        id: 0,
        name: this.routeName.trim(),
        routeType: this.routeDataToSave.routeType || 'cycling',
        targetDistance: this.routeDataToSave.distanceMeters || 0,
        distanceMeters: this.routeDataToSave.distanceMeters || 0,
        elevationGain: this.routeDataToSave.elevationGain || 0,
        elevationLoss: this.routeDataToSave.elevationLoss || 0,
        startPoint: this.routeDataToSave.startPoint || '',
        routeGeom: this.routeDataToSave.geometryWkt || '',
        createdAt: new Date().toISOString(),
      })
      .subscribe({
        next: () => {
          this.routeService.routeAdded$.next();

          alert('Trasa została zapisana pomyślnie!');
          this.isLoading.set(false);
          this.close();
        },

        error: (err) => {
          console.error('Błąd podczas zapisywania trasy:', err);
          alert('Wystąpił błąd podczas zapisywania trasy. Spróbuj ponownie.');
          this.isLoading.set(false);
        },
      });
  }
}
