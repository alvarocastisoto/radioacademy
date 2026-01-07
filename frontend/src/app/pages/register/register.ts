import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth/auth';
import { CommonModule } from '@angular/common'; // Recomendado importar CommonModule

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

  // 👇 El mismo Regex que en Java (permitiendo . _ * -)
  private readonly passwordPattern =
    /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._*-])(?=\S+$).{8,}$/;

  registerForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    surname: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    // 👇 Validación fuerte sincronizada con Backend
    password: [
      '',
      [Validators.required, Validators.minLength(8), Validators.pattern(this.passwordPattern)],
    ],
    dni: ['', Validators.required],
    phone: [''],
    region: ['GALICIA'],
    role: ['STUDENT'],
    termsAccepted: [false, Validators.requiredTrue],
  });

  // Helper para el HTML (Punto 5: Validación consistente touched/dirty)
  isFieldInvalid(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }
  // Getter para usar fácil en el HTML (ej: @if (password.hasError('pattern')))
  get password() {
    return this.registerForm.get('password');
  }

  onSubmit() {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched(); // Marca todo en rojo para que el usuario vea qué falta
      return;
    }

    console.log('Enviando datos:', this.registerForm.value);

    this.authService.register(this.registerForm.value).subscribe({
      next: (response) => {
        console.log('Registro exitoso:', response);
        alert('¡Cuenta creada con éxito! Ahora inicia sesión.');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('Error al registrar:', err);
        // 👇 Leemos el mensaje exacto que envía tu Backend (Map.of("error", "..."))
        const backendMsg = err.error?.error || 'Ocurrió un error al conectar con el servidor.';
        alert('Error: ' + backendMsg);
      },
    });
  }
}
