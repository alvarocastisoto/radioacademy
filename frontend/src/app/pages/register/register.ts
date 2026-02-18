import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth/auth'; 
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
    
    termsAccepted: [false, Validators.requiredTrue],
  });

  
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

    
    this.authService.register(this.registerForm.value).subscribe({
      next: () => {
        
        console.log('Registro y Auto-Login exitosos');
        
        
        this.router.navigate(['/dashboard']); 
      },
      error: (err) => {
        console.error('Error al registrar:', err);
        
        
        const backendMsg = err.error?.error || 'Error de conexión con el servidor.';
        
        
        alert('Error: ' + backendMsg);
      },
    });
  }
}