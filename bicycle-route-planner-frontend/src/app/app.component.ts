import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd, Event } from '@angular/router';
import { ToastComponent } from './components/toast/toast.component';
import { TranslateService } from '@ngx-translate/core';
import { LoggerService } from './services/logger.service';
import { filter } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  private readonly translate = inject(TranslateService);
  private readonly router = inject(Router);
  private readonly logger = inject(LoggerService);

  constructor() {
    this.setupTranslations();
    this.setupNavigationLogging();
  }

  private setupTranslations(): void {
    this.translate.setFallbackLang('en');

    this.translate.use('en');

    /*
      TODO: Docelowe rozwiązanie na produkcję / obronę (odkomentować na koniec):

      const browserLang = this.translate.getBrowserLang();
      const defaultLang = browserLang && browserLang.match(/en|pl/) ? browserLang : 'en';
      this.translate.use(defaultLang);
    */
  }

  private setupNavigationLogging(): void {
    this.router.events
      .pipe(
        filter((event: Event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe((event: NavigationEnd) => {
        const cleanUrl = this.sanitizeUrl(event.urlAfterRedirects);
        this.logger.debug(`[Navigation] User navigated to: ${cleanUrl}`);
      });
  }

  private sanitizeUrl(url: string): string {
    try {
      const parsedUrl = new URL(url, 'http://localhost');
      return parsedUrl.pathname;
    } catch {
      return url.split('?')[0].split('#')[0];
    }
  }
}
