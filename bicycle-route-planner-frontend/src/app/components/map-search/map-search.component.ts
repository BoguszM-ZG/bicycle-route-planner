import {
  Component,
  ChangeDetectionStrategy,
  signal,
  inject,
  ElementRef,
  viewChild,
  OnInit,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import {
  debounceTime,
  distinctUntilChanged,
  switchMap,
  tap,
} from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';

import { SearchResult, GeocodingSearchParams } from '../../models/map-search.model';
import { MapEngineService } from '../../services/map-engine.service';
import { MapRouteLayerService } from '../../services/map-route-layer.service';
import { MapSearchService } from '../../services/map-search.service';
import { LoggerService } from '../../services/logger.service';
import { MAP_CONSTANTS } from '../../constants/map.constants';

@Component({
  selector: 'app-map-search',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './map-search.component.html',
  styleUrl: './map-search.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapSearchComponent implements OnInit {
  private readonly mapSearchService = inject(MapSearchService);
  private readonly mapEngine = inject(MapEngineService);
  private readonly mapRouteLayer = inject(MapRouteLayerService);
  private readonly destroyRef = inject(DestroyRef);

  public readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');
  public readonly searchControl = new FormControl<string>('', { nonNullable: true });

  public readonly results = signal<SearchResult[]>([]);
  public readonly isSearching = signal<boolean>(false);
  public readonly hasNoResults = signal<boolean>(false);
  private shouldResetDistinct = false;

  ngOnInit(): void {
    this.setupSearchPipeline();
  }

  private setupSearchPipeline(): void {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(MAP_CONSTANTS.SEARCH_CONFIG.DEBOUNCE_MS),
        distinctUntilChanged((prev, curr) => {
          if (this.shouldResetDistinct) {
            this.shouldResetDistinct = false;
            return false;
          }
          return prev === curr;
        }),
        tap((query) => this.handlePreSearchState(query)),
        switchMap((query) => {
          if (query.length < MAP_CONSTANTS.SEARCH_CONFIG.MIN_CHARS) {
            return of([]);
          }
          return this.executeSearch(query);
        }),
        tap((results) => this.handlePostSearchState(results)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((results) => {
        if (this.searchControl.value.length >= MAP_CONSTANTS.SEARCH_CONFIG.MIN_CHARS) {
          this.results.set(results);
        } else {
          this.results.set([]);
        }
      });
  }

  private executeSearch(query: string): Observable<SearchResult[]> {
    const center = this.mapEngine.getCenter();

    const searchParams: GeocodingSearchParams = {
      query,
      lat: center?.lat,
      lon: center?.lng,
    };

    return this.mapSearchService.searchLocation(searchParams);
  }

  private handlePreSearchState(query: string): void {
    this.hasNoResults.set(false);
    if (query.length < MAP_CONSTANTS.SEARCH_CONFIG.MIN_CHARS) {
      this.results.set([]);
      this.isSearching.set(false);
    } else {
      this.isSearching.set(true);
    }
  }

  private handlePostSearchState(results: SearchResult[]): void {
    this.isSearching.set(false);
    if (
      results.length === 0 &&
      this.searchControl.value.length >= MAP_CONSTANTS.SEARCH_CONFIG.MIN_CHARS
    ) {
      this.hasNoResults.set(true);
    }
  }

  public clearSearch(): void {
    this.shouldResetDistinct = true;
    this.searchControl.setValue('');
    this.results.set([]);
    this.isSearching.set(false);
    this.hasNoResults.set(false);
    this.mapRouteLayer.setSearchPin(null);
    this.searchInput()?.nativeElement.focus();
  }

  public selectResult(result: SearchResult): void {
    this.shouldResetDistinct = true;
    this.searchControl.setValue(result.shortLabel, { emitEvent: false });
    this.results.set([]);
    this.isSearching.set(false);
    this.hasNoResults.set(false);

    this.mapEngine.flyTo([result.y, result.x], MAP_CONSTANTS.SEARCH_CONFIG.MAP_ZOOM, {
      animate: true,
      duration: MAP_CONSTANTS.SEARCH_CONFIG.ANIMATION_DURATION_SEC,
    });

    this.mapRouteLayer.setSearchPin({ lat: result.y, lng: result.x });
  }
}

