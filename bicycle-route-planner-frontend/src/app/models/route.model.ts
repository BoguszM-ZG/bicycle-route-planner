export interface RouteDetailDto {
  id?: number;
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

export interface RouteDraft {
  geometryWkt?: string;
  distanceMeters?: number;
  elevationGain?: number;
  elevationLoss?: number;
  name?: string;
  routeType?: string;
  startPoint?: string;
}

export interface RouteStats  {
  geometryWkt: string;
  startPoint: string;
  elevationGain: number;
  elevationLoss: number;
}

export interface OrsGeoJsonFeature {
  geometry: {
    coordinates: number[][];
  };
  properties?: {
    summary?: {
      distance: number;
      duration: number;
    };
  };
}

export interface OrsGeoJsonResponse{
    features: OrsGeoJsonFeature[];
    error?: {
      message: string;
    };
}
