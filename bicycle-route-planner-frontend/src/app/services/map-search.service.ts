import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map as rxMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { GeoFeature, SearchResult } from '../models/map-search.model';
import { GeocodingService } from './geocoding.service';
import { LoggerService } from './logger.service';

@Injectable({ providedIn: 'root' })
export class MapSearchService {
  private readonly geocodingService = inject(GeocodingService);
  private readonly logger = inject(LoggerService);
  private readonly translate = inject(TranslateService);

  public searchLocation(query: string, lat?: number, lng?: number): Observable<SearchResult[]> {
    const trimmed = (query ?? '').trim();
    if (!trimmed) {
      return of([]);
    }

    this.logger.debug('[MapSearch] Searching for address:', { query: trimmed });

    return this.geocodingService.search(trimmed, lat, lng).pipe(
      rxMap((response) => this.mapToSearchResults(response?.features || [])),
      catchError((err) => {
        this.logger.error('[MapSearch] Error in Geocoding (Backend Proxy):', err);
        return of([]);
      }),
    );
  }

  private mapToSearchResults(features: GeoFeature[]): SearchResult[] {
    const validFeatures = features.filter(
      (f) => Boolean(f?.geometry?.coordinates && f.geometry.coordinates.length >= 2),
    );
    const mappedResults = validFeatures.map((f) => this.mapSingleFeature(f));
    return this.deduplicateResults(mappedResults);
  }

  private mapSingleFeature(feature: GeoFeature): SearchResult {
    const props = feature.properties ?? {};
    const [x, y] = feature.geometry.coordinates;

    const mainName = props.name || props.street || props.city || this.getFallbackName();
    const details: string[] = [];

    if (props.city && props.city !== mainName) {
      details.push(props.city);
    }

    if (props.state) {
      details.push(this.normalizeStateName(props.state));
    }

    const subtitle = details.join(', ');
    const label = details.length > 0 ? `${mainName}, ${subtitle}` : mainName;

    return { x, y, label, shortLabel: mainName, subtitle };
  }

  private normalizeStateName(state: string): string {
    if (!state.includes('Voivodeship')) {
      return state;
    }

    const engState = state.replace(' Voivodeship', '').trim();
    const translationKey = `VOIVODESHIPS.${engState.toUpperCase().replace(/[\s-]+/g, '_')}`;

    const translatedState = this.translate.instant(translationKey);

    return translatedState !== translationKey ? translatedState : state;
  }

  private getFallbackName(): string {
    return this.translate.instant('MAP_SEARCH.UNKNOWN_LOCATION');
  }

  private deduplicateResults(results: SearchResult[]): SearchResult[] {
    const uniqueMap = new Map<string, SearchResult>();

    for (const result of results) {
      if (!uniqueMap.has(result.label)) {
        uniqueMap.set(result.label, result);
      }
    }

    return Array.from(uniqueMap.values());
  }
}
