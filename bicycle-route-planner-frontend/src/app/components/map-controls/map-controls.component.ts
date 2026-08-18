import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-map-controls',
  standalone: true,
  imports: [],
  templateUrl: './map-controls.component.html',
  styleUrl: './map-controls.component.css',
})
export class MapControlsComponent {
  @Output() save = new EventEmitter<void>();
  @Output() clear = new EventEmitter<void>();

  public onSaveClick(): void {
    this.save.emit();
  }
  public onClearClick(): void {
    this.clear.emit();
  }
}
