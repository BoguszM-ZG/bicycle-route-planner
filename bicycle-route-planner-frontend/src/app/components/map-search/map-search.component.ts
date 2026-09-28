import {
  Component,
  ChangeDetectionStrategy,
  signal,
  inject,
  DestroyRef,
  ElementRef,
  viewChild,

} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { debounceTime, distinctUntilChanged, switchMap, catchError, filter, tap, map as rxMap, } from 'rxjs/operators';
import { of } from 'rxjs';
import * as L from 'leaflet';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { GeoFeature, GeoResponse, SearchResult } from '../../models/map-search.model';
import { LoggerService } from '../../services/logger.service';
import { MapRenderService } from '../../services/map-render.service';
import { environment } from '../../../environments/environment';
import { MAP_CONSTANTS } from '../../constants/map.constants';

@Component({
  selector: 'app-map-search',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './map-search.component.html',
  styleUrl: './map-search.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapSearchComponent {
  public searchControl = new FormControl<string>('', { nonNullable: true });
  public results = signal<SearchResult[]>([]);
  public isSearching = signal(false);

  public searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');

  private readonly http = inject(HttpClient);
  private readonly mapRenderService = inject(MapRenderService);
  private readonly logger = inject(LoggerService);
  private readonly destroyRef = inject(DestroyRef);
  private marker?: L.Marker;
  private readonly translate = inject(TranslateService);

  public hasNoResults = signal(false);

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (this.marker) {
        this.marker.remove();
      }
    });

    this.searchControl.valueChanges
      .pipe(
        debounceTime(350),
        distinctUntilChanged(),
        takeUntilDestroyed(),
        tap((query) => {
          this.hasNoResults.set(false);
          if (!query || query.length < 3) {
            this.results.set([]);
            this.isSearching.set(false);
          } else {
            this.isSearching.set(true);
          }
        }),
        filter((query) => query.length >= 3),
        switchMap((query) => {
          const map = this.mapRenderService.getMap();
          let url = `${environment.apiUrl}/geocoding/search?q=${encodeURIComponent(query)}`;
          if (map) {
            const center = map.getCenter();
            url += `&lat=${center.lat}&lon=${center.lng}`;
          }

          return this.http.get<GeoResponse>(url).pipe(
            catchError((err) => {
              this.logger.error('Error in Geocoding (Backend Proxy):', err);
              return of({ features: [] } as GeoResponse);
            }),
          );
        }),
        rxMap((response: GeoResponse) => {
          const features = response.features || [];

          const mappedResults: SearchResult[] = features.map((f: GeoFeature) => {
            const props = f.properties;
            const coords = f.geometry.coordinates;

            const mainName =
              props.name ||
              props.street ||
              props.city ||
              this.translate.instant('MAP_SEARCH.UNKNOWN_LOCATION');
            const details: string[] = [];

            if (props.city && props.city !== mainName) details.push(props.city);

            if (props.state) {
              let stateName = props.state;

              if (stateName.includes('Voivodeship')) {
                const engState = stateName.replace(' Voivodeship', '').trim();

                const translationKey = `VOIVODESHIPS.${engState.toUpperCase().replace(/[\s-]+/g, '_')}`;

                const translatedState = this.translate.instant(translationKey);

                if (translatedState !== translationKey) {
                  stateName = translatedState;
                }
              }
              details.push(stateName);
            }

            return {
              x: coords[0],
              y: coords[1],
              label: `${mainName}${details.length > 0 ? ', ' + details.join(', ') : ''}`,
              shortLabel: mainName,
              subtitle: details.join(', '),
            };
          });

          return mappedResults.filter(
            (val: SearchResult, idx: number, arr: SearchResult[]) =>
              idx === arr.findIndex((t: SearchResult) => t.label === val.label),
          );
        }),
        tap(() => this.isSearching.set(false)),
      )
      .subscribe((finalResults) => {
        this.results.set(finalResults);
        if (finalResults.length === 0 && this.searchControl.value.length >= 3) {
          this.hasNoResults.set(true);
        }
      });
  }

  public clearSearch(): void {
    this.searchControl.setValue('');
    this.results.set([]);
    this.isSearching.set(false);
    this.hasNoResults.set(false);
    this.searchInput()?.nativeElement.focus();
  }

  public selectResult(result: SearchResult): void {
    const map = this.mapRenderService.getMap();
    if (!map) return;

    this.searchControl.setValue(result.shortLabel, { emitEvent: false });
    this.results.set([]);
    this.hasNoResults.set(false);

    map.flyTo([result.y, result.x], 15, { animate: true, duration: 1.5 });

    if (this.marker) {
      this.marker.remove();
    }

    this.marker = L.marker([result.y, result.x], { icon: MAP_CONSTANTS.SEARCH_PIN }).addTo(map);

    this.marker.once('click', () => {
      if (this.marker) {
        this.marker.remove();
        this.marker = undefined;
        this.searchControl.setValue('', { emitEvent: false });
      }
    });
  }
}
