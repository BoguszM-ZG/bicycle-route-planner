import type { LineString, MultiLineString } from 'geojson';
import type { LatLng } from 'leaflet';

export type RouteType = 'POINT_TO_POINT' | 'LOOP';
export type RoutingMode = 'direct' | 'roundTrip' | 'avoidanceZone';
export type RouteExportFormat = 'gpx' | 'geojson';
export type RouteGeometry = LineString | MultiLineString;

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

export interface RouteWkt {
  geometryWkt: string;
  startPoint: string;
}

export type RouteStats = RouteWkt;

export interface RenameRouteRequestDto {
  name: string;
}

export interface ElevationPointDto {
  distance: number;
  elevation: number;
}

export interface UpdateRouteFolderDto {
  folderId: number | null;
}

export interface ActiveRouteGeometry {
  coordinates: LatLng[];
  routeType: RouteType;
  startPoint?: LatLng;
}
