import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './components/toast/toast.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  private readonly translate = inject(TranslateService);

  constructor() {
    this.setupTranslations();
  }

  private setupTranslations(): void {
    this.translate.setFallbackLang('pl');

    const browserLang = this.translate.getBrowserLang();
    const isSupported = browserLang === 'en' || browserLang === 'pl';
    this.translate.use(isSupported ? browserLang : 'pl');
  }
}
