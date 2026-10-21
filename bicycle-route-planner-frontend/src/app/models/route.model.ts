import type { Feature, FeatureCollection, LineString, MultiLineString, MultiPolygon } from 'geojson';
import type { LatLng } from 'leaflet';

export type RouteType = 'POINT_TO_POINT' | 'LOOP';
export type RoutingMode = 'direct' | 'roundTrip' | 'avoidanceZone';
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

export interface RouteStats  {
  geometryWkt: string;
  startPoint: string;
  elevationGain: number;
  elevationLoss: number;
}

export interface RenameRouteRequestDto {
  name: string;
}

export interface OrsFeatureSummary {
  distance: number;
  duration: number;
}

export interface OrsFeatureProperties {
  summary?: OrsFeatureSummary;
  [key: string]: unknown;
}

export type OrsGeoJsonFeature = Feature<LineString, OrsFeatureProperties>;

export interface OrsGeoJsonResponse extends Partial<FeatureCollection<LineString, OrsFeatureProperties>> {
  features?: OrsGeoJsonFeature[];
  error?: {
    code?: number;
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

export type AvoidPolygonsGeoJson = MultiPolygon;

export interface ActiveRouteGeometry {
  coordinates: LatLng[];
  routeType: RouteType;
  startPoint?: LatLng;
}
