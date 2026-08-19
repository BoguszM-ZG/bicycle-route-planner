import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';


@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RegisterComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  registerForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    fullName: ['', [Validators.maxLength(255)]]
  });

  public errorMessage = signal<string>('');
  public isLoading = signal<boolean>(false);

  public onSubmit() {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    const { email, password, fullName } = this.registerForm.getRawValue();

    this.authService.register({ email, password, fullName }).subscribe({
      next: (response) => {
        localStorage.setItem('accessToken', response.accessToken);
        localStorage.setItem('refreshToken', response.refreshToken);
        localStorage.setItem('currentUser', JSON.stringify(response.user));

        void this.router.navigate(['/']);
      },
      error: (err) => {
        this.isLoading .set(false);
        this.errorMessage.set(err.error?.message || 'Registration failed. Please try again.');
      }
    });
  }
}
