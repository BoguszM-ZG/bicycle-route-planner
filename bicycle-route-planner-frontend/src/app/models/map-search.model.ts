export interface GeoFeature {
  properties: {
    name?: string;
    street?: string;
    city?: string;
    state?: string;
  };
  geometry: {
    coordinates: [number, number];
  };
}

export interface GeoResponse {
  features?: GeoFeature[];
}

export interface SearchResult {
  x: number;
  y: number;
  label: string;
  shortLabel: string;
  subtitle: string;
}
