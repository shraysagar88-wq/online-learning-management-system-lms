import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css'
})
export class AuthComponent {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  isRegister = false;
  busy = false;
  error = '';
  form = { name: '', email: '', password: '' };

  submit(): void {
    if (this.busy) return;
    this.busy = true;
    this.error = '';

    const request = this.isRegister
      ? this.api.register(this.form)
      : this.api.login({ email: this.form.email, password: this.form.password });

    request.subscribe({
      next: () => this.router.navigateByUrl('/dashboard'),
      error: (error) => {
        this.error = error?.error?.detail || error?.error?.message || 'Unable to sign in. Check your details and try again.';
        this.busy = false;
      },
      complete: () => this.busy = false
    });
  }

  toggleMode(): void {
    this.isRegister = !this.isRegister;
    this.error = '';
  }
}
