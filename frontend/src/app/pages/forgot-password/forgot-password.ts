import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule], // Importamos lo necesario
  templateUrl: './forgot-password.html',
  styleUrls: ['../login/login.scss'], // 👈 REUTILIZAMOS los estilos del Login
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  // Formulario con un solo campo: email
  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  message = ''; // Para mensaje de éxito
  error = ''; // Para mensaje de error
  isLoading = false;

  onSubmit() {
    if (this.form.invalid) return;

    this.isLoading = true;
    this.message = '';
    this.error = '';

    // 👇 EL CAMBIO ESTÁ AQUÍ: Añadimos "|| ''"
    const email = this.form.get('email')?.value || '';

    this.authService.requestPasswordReset(email).subscribe({
      next: () => {
        this.isLoading = false;
        this.message = 'Si el correo existe, recibirás un enlace en breve.';
        this.form.reset();
      },
      error: () => {
        this.isLoading = false;
        // Para desarrollo puedes dejar esto para ver si falla algo en el back:
        this.error = 'Hubo un error al procesar la solicitud.';
      },
    });
  }
}
