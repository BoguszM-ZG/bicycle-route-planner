import type { Feature, LineString, MultiPolygon, Position } from 'geojson';

export type AvoidPolygonsGeoJson = MultiPolygon;

export interface RoundTripParams {
  startCoord: Position;
  distanceMeters: number;
  avoidPolygons?: AvoidPolygonsGeoJson;
  seed?: number;
}

export interface OrsRouteRequestDto {
  coordinates: Position[];
  elevation: boolean;
  instructions: boolean;
  options?: {
    avoid_polygons?: AvoidPolygonsGeoJson;
    round_trip?: {
      length: number;
      points?: number;
      seed?: number;
    };
    [key: string]: unknown;
  };
}

export interface OrsGeoJsonResponse {
  type?: 'FeatureCollection';
  features?: Feature<
    LineString,
    {
      summary?: {
        distance: number;
        duration?: number;
      };
      ascent?: number;
      descent?: number;
      [key: string]: unknown;
    }
  >[];
  error?: {
    code?: number;
    message: string;
  };
}
