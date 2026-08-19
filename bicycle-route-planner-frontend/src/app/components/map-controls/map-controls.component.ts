import { Component, output, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-map-controls',
  standalone: true,
  imports: [],
  templateUrl: './map-controls.component.html',
  styleUrl: './map-controls.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MapControlsComponent {
  save = output<void>();
  clear = output<void>();

  public onSaveClick(): void {
    this.save.emit();
  }
  public onClearClick(): void {
    this.clear.emit();
  }
}
