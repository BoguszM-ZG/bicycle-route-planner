export interface RouteDetailDto {
  id: number;
  userEmail: string;
  name: string;
  routeType: string;
  targetDistance: number;
  distanceMeters: number;
  elevationGain: number;
  elevationLoss: number;
  startPoint: string;
  routeGeom: string;
  createdAt: string;
}
