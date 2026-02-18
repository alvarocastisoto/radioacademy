import { Component, inject, signal } from '@angular/core'; 
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  
  errorMessage = signal<string | null>(null);
  loading = signal(false);
  showPassword = signal(false);
  loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4)]],
  });

  onSubmit() {
    if (this.loginForm.valid) {
      
      this.loading.set(true);
      this.errorMessage.set(null);

      this.authService.login(this.loginForm.value).subscribe({
        next: (response) => {
          console.log('Login correcto en RadioAcademy:', response);
          this.loading.set(false);
          this.router.navigate(['/courses']);
        },
        error: (error) => {
          this.loading.set(false);
          console.error('Error en login:', error);

          if (error.status === 401) {
            this.errorMessage.set('Email o contraseña incorrectos.');
          } else if (error.status === 0) {
            this.errorMessage.set('El servidor local no responde.');
          } else {
            this.errorMessage.set('Error inesperado al iniciar sesión.');
          }
        },
      });
    }
  }

  togglePassword() {
    this.showPassword.update((v) => !v);
  }
}
