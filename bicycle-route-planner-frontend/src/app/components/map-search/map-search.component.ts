import { TranslatePipe } from '@ngx-translate/core';
import { LoggerService } from '../../services/logger.service';
import { Component, ChangeDetectionStrategy, signal, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap, catchError, filter, tap, map as rxMap } from 'rxjs/operators';
import { of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { MapRenderService } from '../../services/map-render.service';
import * as L from 'leaflet';
import { environment } from '../../../environments/environment';
import { MAP_CONSTANTS } from '../../constants/map.constants';

import { PL_VOIVODESHIPS } from '../../constants/geocoding.constants';

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
  public results = signal<any[]>([]);
  public isSearching = signal(false);

  private readonly http = inject(HttpClient);
  private readonly mapRenderService = inject(MapRenderService);
  private readonly logger = inject(LoggerService);
  private marker?: L.Marker;

  constructor() {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(350),
        distinctUntilChanged(),
        takeUntilDestroyed(),
        tap((query) => {
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

          return this.http.get<any>(url).pipe(
            catchError((err) => {
              this.logger.error('Error in Geocoding (Backend Proxy):', err);
              return of({ features: [] });
            }),
          );
        }),
        rxMap((response) => {
          const features = response.features || [];

          const mappedResults = features.map((f: any) => {
            const props = f.properties;
            const coords = f.geometry.coordinates;

            const mainName = props.name || props.street || props.city || 'Nieznane miejsce';

            const details = [];
            if (props.city && props.city !== mainName) details.push(props.city);

            if (props.state) {
              let stateName = props.state;
              if (stateName.includes('Voivodeship')) {
                const engState = stateName.replace(' Voivodeship', '').trim();
                const translated = PL_VOIVODESHIPS[engState] || engState.toLowerCase();
                stateName = `woj. ${translated}`;
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
            (val: any, idx: number, arr: any[]) =>
              idx === arr.findIndex((t: any) => t.label === val.label),
          );
        }),
        tap(() => this.isSearching.set(false)),
      )
      .subscribe((finalResults) => {
        this.results.set(finalResults);
      });
  }

  public selectResult(result: any) {
    const map = this.mapRenderService.getMap();
    if (!map) return;

    this.searchControl.setValue(result.shortLabel, { emitEvent: false });
    this.results.set([]);

    map.flyTo([result.y, result.x], 15, { animate: true, duration: 1.5 });

    if (this.marker) {
      this.marker.remove();
    }

    this.marker = L.marker([result.y, result.x], { icon: MAP_CONSTANTS.SEARCH_PIN }).addTo(map);
    this.marker.on('click', () => {
      if (this.marker) {
        this.marker.remove();
        this.marker = undefined;
        this.searchControl.setValue('', { emitEvent: false });
      }
    });
  }
}
