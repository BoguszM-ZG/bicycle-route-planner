import { Component, inject, OnInit } from '@angular/core';
import { AvoidanceZoneItemComponent } from '../avoidance-zone-item/avoidance-zone-item.component';
import { AvoidanceZoneStateService } from '../../../services/avoidance-zone-state.service';
import { AvoidanceZoneDto } from '../../../models/avoidance-zone.model';

@Component({
  selector: 'app-avoidance-zone-panel',
  standalone: true,
  imports: [AvoidanceZoneItemComponent],
  templateUrl: './avoidance-zone-panel.component.html',
  styleUrl: './avoidance-zone-panel.component.css',
})
export class AvoidanceZonePanelComponent implements OnInit {

  ngOnInit(): void {
    this.zoneStateService.fetchZones();
  }

  private readonly zoneStateService = inject(AvoidanceZoneStateService);

  public zones = this.zoneStateService.zones;


  public onToggleZone(zone: AvoidanceZoneDto): void {
    this.zoneStateService.toggleActive(zone);
  }

  public onDeleteZone(id: number): void {
    this.zoneStateService.deleteZone(id);
  }







}
