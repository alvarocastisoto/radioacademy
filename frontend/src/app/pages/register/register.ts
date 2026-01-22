import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth/auth'; // Asegúrate de que la ruta es correcta
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  // 👇 Regex sincronizado con tu backend (AuthService.java)
  // "Al menos 1 num, 1 minus, 1 mayus, 1 especial, sin espacios, min 8 chars"
  private readonly passwordPattern =
    /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[\W_])(?=\S+$).{8,}$/;

  registerForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    surname: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: [
      '',
      [Validators.required, Validators.minLength(8), Validators.pattern(this.passwordPattern)],
    ],
    dni: ['', Validators.required],
    phone: [''],
    region: ['GALICIA'],
    // role: ['STUDENT'], // 💡 NOTA: Tu backend fuerza el rol STUDENT, así que no hace falta enviarlo desde aquí.
    termsAccepted: [false, Validators.requiredTrue],
  });

  // Helper para validación visual
  isFieldInvalid(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  get password() {
    return this.registerForm.get('password');
  }

  onSubmit() {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    console.log('Enviando registro...', this.registerForm.value);

    // 👇 AQUÍ ESTÁ LA MAGIA DEL AUTO-LOGIN
    this.authService.register(this.registerForm.value).subscribe({
      next: () => {
        // El AuthService ya guardó el token y el user en localStorage por ti.
        console.log('Registro y Auto-Login exitosos');
        
        // Redirigimos DIRECTAMENTE al Dashboard (o a /courses)
        this.router.navigate(['/dashboard']); 
      },
      error: (err) => {
        console.error('Error al registrar:', err);
        // Tu backend devuelve: { "error": "El email ya está registrado..." }
        // Con status 409 Conflict o 400 Bad Request
        const backendMsg = err.error?.error || 'Error de conexión con el servidor.';
        
        // En una app real, usaríamos un Toast/SnackBar, pero alert vale por ahora
        alert('Error: ' + backendMsg);
      },
    });
  }
}