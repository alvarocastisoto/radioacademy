import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink], // Importamos las herramientas de formulario
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class Register {
  // Inyectamos dependencias
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  // Definimos la estructura del formulario
  registerForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    surname: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4)]],
    dni: ['', Validators.required],
    phone: [''],
    region: ['GALICIA'], // Valor por defecto
    role: ['STUDENT'],   // Valor por defecto (oculto)
    termsAccepted: [false, Validators.requiredTrue]
  });

  // Qué pasa cuando le das a Enviar
  onSubmit() {
    if (this.registerForm.valid) {
      console.log('Enviando datos:', this.registerForm.value);
      
      this.authService.register(this.registerForm.value).subscribe({
        next: (response) => {
          console.log('Registro exitoso:', response);
          alert('¡Cuenta creada! Ahora inicia sesión.');
          this.router.navigate(['/login']); // Redirigir al login
        },
        error: (error) => {
          console.error('Error al registrar:', error);
          alert('Error: ' + (error.error?.message || 'No se pudo conectar'));
        }
      });
    } else {
      alert('Por favor, rellena todos los campos obligatorios.');
    }
  }
}