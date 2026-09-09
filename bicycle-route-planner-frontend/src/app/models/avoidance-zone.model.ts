export interface AvoidanceZoneDto{
  id?:number;
  name: string;
  zoneGeom: string;
  isActive?: boolean;
}

export interface CreateAvoidanceZoneRequestDto{
  name: string;
  zoneGeom: string;
}
