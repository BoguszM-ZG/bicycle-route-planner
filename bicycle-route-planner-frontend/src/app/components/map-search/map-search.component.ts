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
  filter,
  tap,
  catchError,
} from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';

import { SearchResult } from '../../models/map-search.model';
import { MapRenderService } from '../../services/map-render.service';
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
  private readonly mapRenderService = inject(MapRenderService);
  private readonly logger = inject(LoggerService);
  private readonly destroyRef = inject(DestroyRef);


  public readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');
  public readonly searchControl = new FormControl<string>('', { nonNullable: true });

  public readonly results = signal<SearchResult[]>([]);
  public readonly isSearching = signal<boolean>(false);
  public readonly hasNoResults = signal<boolean>(false);


  ngOnInit(): void {
    this.setupSearchPipeline();
  }


  private setupSearchPipeline(): void {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(MAP_CONSTANTS.SEARCH_CONFIG.DEBOUNCE_MS),
        distinctUntilChanged(),
        tap((query) => this.handlePreSearchState(query)),
        filter((query) => query.length >= MAP_CONSTANTS.SEARCH_CONFIG.MIN_CHARS),
        switchMap((query) => this.executeSearch(query)),
        tap((results) => this.handlePostSearchState(results)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((results) => this.results.set(results));
  }

  private executeSearch(query: string): Observable<SearchResult[]> {
    const center = this.mapRenderService.getCenter();

    return this.mapSearchService.searchLocation(query, center?.lat, center?.lng).pipe(
      catchError((err) => {
        this.logger.error('[MapSearch] Unable to execute search', err);
        return of([]);
      }),
    );
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
    this.searchControl.setValue('');
    this.results.set([]);
    this.isSearching.set(false);
    this.hasNoResults.set(false);
    this.mapRenderService.clearSearchMarker();
    this.searchInput()?.nativeElement.focus();
  }

  public selectResult(result: SearchResult): void {
    this.searchControl.setValue(result.shortLabel, { emitEvent: false });
    this.results.set([]);
    this.hasNoResults.set(false);

    this.mapRenderService.focusOnSearchLocation(
      result.y,
      result.x,
      MAP_CONSTANTS.SEARCH_CONFIG.MAP_ZOOM,
      MAP_CONSTANTS.SEARCH_CONFIG.ANIMATION_DURATION_SEC,
      () => this.searchControl.setValue('', { emitEvent: false }),
    );
  }
}
