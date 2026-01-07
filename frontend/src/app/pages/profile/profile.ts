import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { StudentService } from '../../services/student/student';
import { AuthService } from '../../services/auth/auth';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.html',
  styleUrls: ['./profile.scss'],
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private studentService = inject(StudentService);
  private authService = inject(AuthService);
  private cd = inject(ChangeDetectorRef);

  // Variable para recordar el email original y detectar cambios
  currentEmail: string = '';

  profileForm: FormGroup;
  loading = false;
  successMessage = '';
  errorMessage = '';

  constructor() {
    this.profileForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      surname: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      currentPassword: [''],
      newPassword: ['', [Validators.minLength(6)]],
      confirmPassword: [''],
    });
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile() {
    this.loading = true;
    this.studentService
      .getProfile()
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cd.detectChanges();
        })
      )
      .subscribe({
        next: (user) => {
          this.currentEmail = user.email; // Guardamos el email original
          this.profileForm.patchValue({
            name: user.name,
            surname: user.surname,
            email: user.email,
            phone: user.phone,
          });
        },
        error: (err) => {
          console.error('Error cargando perfil', err);
          this.errorMessage = 'No se pudieron cargar tus datos.';
        },
      });
  }

  onSubmit() {
    if (this.profileForm.invalid) return;
    this.successMessage = '';
    this.errorMessage = '';

    const formValues = this.profileForm.value;

    // Validación de contraseñas iguales
    if (formValues.newPassword && formValues.newPassword !== formValues.confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden.';
      return;
    }

    // Validación: Si hay nueva contraseña, exigimos la actual
    if (formValues.newPassword && !formValues.currentPassword) {
      this.errorMessage = 'Por seguridad, introduce tu contraseña actual.';
      return;
    }

    this.loading = true;

    const dto = {
      name: formValues.name,
      surname: formValues.surname,
      email: formValues.email,
      phone: formValues.phone,
      newPassword: formValues.newPassword,
      currentPassword: formValues.currentPassword,
    };

    console.log('Enviando datos...', dto);

    this.studentService
      .updateProfile(dto)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cd.detectChanges();
        })
      )
      .subscribe({
        next: (res) => {
          this.successMessage = '¡Perfil actualizado correctamente!';

          // Limpiamos campos de seguridad
          this.profileForm.patchValue({
            currentPassword: '',
            newPassword: '',
            confirmPassword: '',
          });

          // 🛑 LÓGICA CRÍTICA DE SEGURIDAD 🛑
          if (dto.email !== this.currentEmail) {
            // Si cambió el email, el token JWT antiguo ya no sirve.
            alert(
              'Has cambiado tu correo electrónico. Por seguridad, debes iniciar sesión de nuevo.'
            );
            this.authService.logout();
          } else {
            // Si el email es el mismo, solo actualizamos la interfaz (Navbar, etc.)
            this.authService.updateUserFields({
              name: dto.name,
              surname: dto.surname,
              email: dto.email,
              phone: dto.phone,
            });
          }
        },
        error: (error: HttpErrorResponse) => {
          console.error('❌ Error recibido:', error);

          if (error.status === 409) {
            this.errorMessage = 'Ese correo electrónico ya está en uso.';
          } else if (error.error && error.error.error) {
            // Mensaje específico del backend (ej: "Contraseña actual incorrecta")
            this.errorMessage = error.error.error;
          } else {
            this.errorMessage = 'Ocurrió un error al guardar.';
          }
        },
      });
  }
}
