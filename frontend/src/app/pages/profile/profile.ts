import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { StudentService } from '../../services/student/student';
import { AuthService } from '../../services/auth/auth'; // 👈 1. IMPORTAMOS AUTH SERVICE
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
  private authService = inject(AuthService); // 👈 2. INYECTAMOS AUTH SERVICE
  private cd = inject(ChangeDetectorRef);

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

    if (formValues.newPassword && formValues.newPassword !== formValues.confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden.';
      return;
    }

    this.loading = true;

    const dto = {
      name: formValues.name,
      surname: formValues.surname,
      email: formValues.email,
      phone: formValues.phone,
      newPassword: formValues.newPassword,
    };

    console.log('Enviando datos...', dto);

    this.studentService
      .updateProfile(dto)
      .pipe(
        finalize(() => {
          console.log('🏁 Petición finalizada.');
          this.loading = false;
          this.cd.detectChanges();
        })
      )
      .subscribe({
        next: (res) => {
          console.log('✅ Éxito recibido:', res);
          this.successMessage = '¡Perfil actualizado correctamente!';
          this.profileForm.patchValue({ newPassword: '', confirmPassword: '' });

          // 👇 3. ¡LA MAGIA! Actualizamos el AuthService para que el Navbar cambie YA
          this.authService.updateUserFields({
            name: dto.name,
            surname: dto.surname,
            email: dto.email,
            phone: dto.phone,
          });
        },
        error: (error: HttpErrorResponse) => {
          console.error('❌ Error recibido:', error);
          if (error.status === 409) {
            this.errorMessage = 'Ese correo electrónico ya está en uso.';
          } else {
            this.errorMessage = 'Ocurrió un error al guardar. Inténtalo luego.';
          }
        },
      });
  }
}
