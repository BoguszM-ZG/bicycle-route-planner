import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map as rxMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { GeoFeature, GeoResponse, SearchResult } from '../models/map-search.model';
import { LoggerService } from './logger.service';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MapSearchService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly translate = inject(TranslateService);

  public searchLocation(query: string, lat?: number, lng?: number): Observable<SearchResult[]> {
    const url = `${environment.apiUrl}/geocoding/search`;

    let params = new HttpParams().set('q', query);
    if (lat != null && lng != null) {
      params = params.set('lat', lat).set('lon', lng);
    }

    return this.http.get<GeoResponse>(url, { params }).pipe(
      rxMap((response) => this.mapToSearchResults(response.features || [])),
      catchError((err) => {
        this.logger.error('Error in Geocoding (Backend Proxy):', err);
        return of([]);
      }),
    );
  }

  private mapToSearchResults(features: GeoFeature[]): SearchResult[] {
    const mappedResults = features.map((f) => this.mapSingleFeature(f));
    return this.deduplicateResults(mappedResults);
  }

  private mapSingleFeature(feature: GeoFeature): SearchResult {
    const { properties: props, geometry } = feature;
    const [x, y] = geometry.coordinates;

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
