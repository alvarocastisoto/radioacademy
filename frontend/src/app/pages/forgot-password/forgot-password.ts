import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule], 
  templateUrl: './forgot-password.html',
  styleUrls: ['../login/login.scss'], 
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  
  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  message = ''; 
  error = ''; 
  isLoading = false;

  onSubmit() {
    if (this.form.invalid) return;

    this.isLoading = true;
    this.message = '';
    this.error = '';

    
    const email = this.form.get('email')?.value || '';

    this.authService.requestPasswordReset(email).subscribe({
      next: () => {
        this.isLoading = false;
        this.message = 'Si el correo existe, recibirás un enlace en breve.';
        this.form.reset();
      },
      error: () => {
        this.isLoading = false;
        
        this.error = 'Hubo un error al procesar la solicitud.';
      },
    });
  }
}
