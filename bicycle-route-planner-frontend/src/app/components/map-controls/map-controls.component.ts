import { Component, output, ChangeDetectionStrategy, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { MapRenderService } from '../../services/map-render.service';

@Component({
  selector: 'app-map-controls',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './map-controls.component.html',
  styleUrl: './map-controls.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapControlsComponent {
  public readonly mapRenderService = inject(MapRenderService);

  save = output<void>();
  clear = output<void>();
  locate = output<void>();

  public onSaveClick(): void {
    this.save.emit();
  }
  public onClearClick(): void {
    this.clear.emit();
  }

  public onLocateClick(): void {
    if (this.mapRenderService.isLocating()) {
      return;
    }
    this.locate.emit();
  }
}
