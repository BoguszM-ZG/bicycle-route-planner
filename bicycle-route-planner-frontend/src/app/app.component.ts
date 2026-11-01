import { Component, ChangeDetectionStrategy, inject, DOCUMENT } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from './components/toast/toast.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastComponent],
  templateUrl: './app.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  private readonly translate = inject(TranslateService);
  private readonly document = inject(DOCUMENT);

  constructor() {
    this.setupTranslations();
  }

  private setupTranslations(): void {
    const browserLang = this.translate.getBrowserLang();
    const lang = browserLang === 'en' || browserLang === 'pl' ? browserLang : 'pl';
    this.translate.use(lang);
    this.document.documentElement.lang = lang;
  }
}
