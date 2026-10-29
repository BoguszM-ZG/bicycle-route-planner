import { Component, output, ChangeDetectionStrategy, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { MapEngineService } from '../../services/map-engine.service';

@Component({
  selector: 'app-map-controls',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './map-controls.component.html',
  styleUrl: './map-controls.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapControlsComponent {
  public readonly mapEngine = inject(MapEngineService);

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
    if (this.mapEngine.isLocating()) {
      return;
    }
    this.mapEngine.locateUser();
    this.locate.emit();
  }
}

