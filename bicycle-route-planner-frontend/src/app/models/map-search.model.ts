import type { Feature, FeatureCollection, Point } from 'geojson';

export interface GeoFeatureProperties {
  name?: string;
  street?: string;
  city?: string;
  state?: string;
  [key: string]: unknown;
}

export type GeoFeature = Feature<Point, GeoFeatureProperties>;

export interface GeoResponse extends Partial<FeatureCollection<Point, GeoFeatureProperties>> {
  features?: GeoFeature[];
}

export interface SearchResult {
  x: number;
  y: number;
  label: string;
  shortLabel: string;
  subtitle: string;
}

export interface GeocodingSearchParams {
  query: string;
  lat?: number;
  lon?: number;
}


