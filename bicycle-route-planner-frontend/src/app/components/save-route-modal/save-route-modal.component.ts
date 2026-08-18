import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule} from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService} from '../../services/auth.service';
import { RouteService} from '../../services/route.service';

@Component({
  selector: 'app-save-route-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './save-route-modal.component.html',
  styleUrl: './save-route-modal.component.css',
})
export class SaveRouteModalComponent {
  private readonly routeService = inject(RouteService);
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  public isVisible: boolean = false;
  public routeName: string = '';
  public isLoading: boolean = false;
  private routeDataToSave: any = null;


  public open(routeData: any): void {
    this.routeDataToSave = routeData;
    this.routeName = 'Moja trasa rowerowa';
    this.isVisible = true;
    this.cdr.detectChanges();
  }

  public close(): void {
    this.isVisible = false;
    this.routeName = '';
    this.routeDataToSave = null;
    this.cdr.detectChanges();
  }

  public saveRoute(): void {

    if (this.routeName.trim() === '') {
      alert('Proszę wpisać nazwę trasy');
      return;
    }

    const currentUser = this.authService.getCurrentUser();
    this.isLoading = true;

    this.routeService.addRoute({
      id: 0,
      userEmail: currentUser ? currentUser.email : '',
      name: this.routeName.trim(),
      routeType: this.routeDataToSave.routeType || 'cycling',
      targetDistance: this.routeDataToSave.targetDistance,
      distanceMeters: this.routeDataToSave.distanceMeters,
      elevationGain: this.routeDataToSave.elevationGain || 0,
      elevationLoss: this.routeDataToSave.elevationLoss || 0,
      startPoint: this.routeDataToSave.startPoint,
      routeGeom: this.routeDataToSave.geometryWkt,
      createdAt: new Date().toISOString(),
    }).subscribe({
      next: () => {

        this.routeService.routeAdded$.next();

        alert('Trasa została zapisana pomyślnie!');
        this.isLoading = false;
        this.close();
    },

      error: err => {
        console.error('Błąd podczas zapisywania trasy:', err);
        alert('Wystąpił błąd podczas zapisywania trasy. Spróbuj ponownie.');
        this.isLoading = false;
      }
    })

  }



}
