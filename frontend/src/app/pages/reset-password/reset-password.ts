import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './reset-password.html',
  styleUrls: ['../login/login.scss'],
})
export class ResetPasswordComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);

  
  private readonly passwordPattern =
    /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._*-])(?=\S+$).{8,}$/;

  token = '';
  error = '';
  success = false;

  form: FormGroup = this.fb.group({
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(this.passwordPattern), 
      ],
    ],
  });

  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      this.token = params['token'];
      if (!this.token) {
        this.error = 'Enlace no válido. Falta el token.';
        this.form.disable();
      }
    });
  }

  
  isFieldInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  
  get password() {
    return this.form.get('password');
  }

  onSubmit() {
    if (this.form.invalid || !this.token) {
      this.form.markAllAsTouched();
      return;
    }

    const newPassword = this.form.get('password')?.value;

    this.authService.resetPassword(this.token, newPassword).subscribe({
      next: () => {
        this.success = true;
        setTimeout(() => this.router.navigate(['/auth/login']), 3000);
      },
      error: (err) => {
        this.error = err.error?.error || 'El enlace ha caducado o no es válido.';
      },
    });
  }
}
