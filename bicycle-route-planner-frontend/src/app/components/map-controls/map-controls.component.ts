import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-map-controls',
  imports: [TranslatePipe],
  templateUrl: './map-controls.component.html',
  styleUrl: './map-controls.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapControlsComponent {
  public readonly isLocating = input(false);

  public readonly save = output<void>();
  public readonly clear = output<void>();
  public readonly locate = output<void>();

  public onSaveClick(): void {
    this.save.emit();
  }

  public onClearClick(): void {
    this.clear.emit();
  }

  public onLocateClick(): void {
    this.locate.emit();
  }
}

