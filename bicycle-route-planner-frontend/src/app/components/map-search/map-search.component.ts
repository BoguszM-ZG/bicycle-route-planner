import { Component, ChangeDetectionStrategy, signal, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap, catchError, filter, tap, map as rxMap } from 'rxjs/operators';
import { of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { MapRenderService } from '../../services/map-render.service';
import * as L from 'leaflet';

const PL_VOIVODESHIPS: Record<string, string> = {
  'Lubusz': 'lubuskie',
  'Greater Poland': 'wielkopolskie',
  'Lesser Poland': 'małopolskie',
  'Lower Silesian': 'dolnośląskie',
  'Kuyavian-Pomeranian': 'kujawsko-pomorskie',
  'Lublin': 'lubelskie',
  'Łódź': 'łódzkie',
  'Mazovia': 'mazowieckie',
  'Opole': 'opolskie',
  'Podlaskie': 'podlaskie',
  'Pomeranian': 'pomorskie',
  'Silesian': 'śląskie',
  'Subcarpathian': 'podkarpackie',
  'Świętokrzyskie': 'świętokrzyskie',
  'Warmian-Masurian': 'warmińsko-mazurskie',
  'West Pomeranian': 'zachodniopomorskie'
};

@Component({
  selector: 'app-map-search',
  standalone: true,
  imports: [ReactiveFormsModule],
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
          let locationParams = '';
          if (map) {
            const center = map.getCenter();
            locationParams = `&lat=${center.lat}&lon=${center.lng}`;
          }

          const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(query)}&limit=5${locationParams}`;

          return this.http.get<any>(url).pipe(
            catchError((err) => {
              console.error('Błąd Geocodingu Photon:', err);
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

    const icon = L.divIcon({
      className: 'search-pin',
      html: `<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="#3b82f6" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3" fill="white"></circle></svg>`,
      iconSize: [32, 32],
      iconAnchor: [16, 32],
    });

    this.marker = L.marker([result.y, result.x], { icon }).addTo(map);
  }
}
