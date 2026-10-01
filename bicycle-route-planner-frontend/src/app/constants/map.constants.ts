import { LatLngTuple } from 'leaflet';
import * as L from 'leaflet';

export const MAP_CONSTANTS = {
  // Początkowy widok mapy (Polska)
  DEFAULT_CENTER: [52.069, 19.47] as LatLngTuple,
  DEFAULT_ZOOM: 6,

  // Konfiguracja kafelków (OpenStreetMap)
  TILE_LAYER: {
    URL: 'https://{s}.tile.osm.org/{z}/{x}/{y}.png',
    MAX_ZOOM: 19,
    ATTRIBUTION: '@ OpenStreetMap'
  },

  // Style rysowanych tras i linii
  STYLES: {
    SAVED_ROUTE: { color: '#e74c3c', weight: 6, opacity: 0.9 },
    DRAFT_ROUTE: { color: '#e74c3c', weight: 5, opacity: 0.8 },
    LOOP_ROUTE:  { color: '#8e44ad', weight: 6 }
  },

  // Domyślna ikona znacznika (nadpisuje błędne linki lokalne Leafleta w aplikacjach z bundlerem)
  DEFAULT_ICON: L.icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41],
  }),

  // Ikona markera z wyszukiwarki (Niebieska pinezka SVG)
  SEARCH_PIN: L.divIcon({
    className: 'search-pin',
    html: `<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="#3b82f6" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3" fill="white"></circle></svg>`,
    iconSize: [32, 32],
    iconAnchor: [16, 32],
  }),

  // Markery początku, końca trasy oraz pętli
  MARKERS: {
    LOOP_ICON: L.divIcon({
      className: 'route-marker-loop',
      html: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/></svg>`,
      iconSize: [28, 28],
      iconAnchor: [14, 14]
    }),
    START_ICON: L.divIcon({
      className: 'route-marker-circle route-marker-start',
      html: 'A',
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    }),
    END_ICON: L.divIcon({
      className: 'route-marker-circle route-marker-end',
      html: 'B',
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    })
  },

  SEARCH_CONFIG: {
    DEBOUNCE_MS: 350,
    MIN_CHARS: 3,
    MAP_ZOOM: 15,
    ANIMATION_DURATION_SEC: 1.5
  }
};
