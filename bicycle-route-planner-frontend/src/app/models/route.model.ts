export type RouteType = 'POINT_TO_POINT' | 'LOOP';

export interface RouteDetailDto {
  id?: number;
  name: string;
  routeType: RouteType;
  targetDistance: number;
  distanceMeters: number;
  elevationGain: number;
  elevationLoss: number;
  startPoint: string;
  routeGeom: string;
  createdAt: string;
  folderId?: number | null;
}

export interface RouteSummaryDto {
   id: number;
   name: string;
   distanceMeters: number;
   folderId?: number | null;
}

export interface CreateRouteRequestDto {
  name: string;
  routeType: RouteType;
  targetDistance: number;
  distanceMeters: number;
  elevationGain: number;
  elevationLoss: number;
  startPoint: string;
  routeGeom: string;
  folderId?: number | null;
}

export interface RouteDraft {
  geometryWkt?: string;
  distanceMeters?: number;
  targetDistance?: number;
  elevationGain?: number;
  elevationLoss?: number;
  name?: string;
  routeType?: RouteType;
  startPoint?: string;
}

export interface RouteStats  {
  geometryWkt: string;
  startPoint: string;
  elevationGain: number;
  elevationLoss: number;
}

export interface RenameRouteRequestDto {
  name: string;
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

export interface ElevationPointDto {
  distance: number;
  elevation: number;
}

export interface UpdateRouteFolderDto {
  folderId: number | null;
}

export interface AvoidPolygonsGeoJson {
  type: 'MultiPolygon';
  coordinates: number[][][][];
}
