import { Component, OnInit, inject, ChangeDetectorRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { StudentService } from '../../services/student/student';
import { AuthService } from '../../services/auth/auth';
import { MediaService } from '../../services/media/media';

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
  public mediaService = inject(MediaService);

  // Estados reactivos
  currentEmail = signal<string>('');
  avatarUrl = signal<string | null>(null);
  loading = signal<boolean>(false);
  isUploading = signal<boolean>(false);

  // Mensajes de feedback
  successMessage = signal<string>('');
  errorMessage = signal<string>('');

  profileForm: FormGroup;

  constructor() {
    this.profileForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      surname: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      currentPassword: [''],
      newPassword: ['', [Validators.minLength(6)]],
      confirmPassword: [''],
      avatar: [''], // Guardamos el path relativo (uploads/...)
    });
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile() {
    this.loading.set(true);

    this.studentService
      .getProfile()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (user) => {
          this.currentEmail.set(user.email);

          // Convertimos path relativo a URL pública para mostrar
          const publicAvatar = this.mediaService.toPublicUrl(user.avatar);
          this.avatarUrl.set(publicAvatar);

          this.profileForm.patchValue({
            name: user.name,
            surname: user.surname,
            email: user.email,
            phone: user.phone,
            avatar: user.avatar,
          });
        },
        error: (err) => {
          console.error('Error cargando perfil', err);
          this.errorMessage.set('No se pudieron cargar tus datos.');
        },
      });
  }

  // Si la imagen falla (404), ponemos un avatar generado por iniciales
  handleImageError() {
    const name = this.profileForm.get('name')?.value || 'U';
    const fallback = `https://ui-avatars.com/api/?name=${name}&background=random&color=fff&size=128`;
    this.avatarUrl.set(fallback);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.uploadAvatar(file);
    }
  }

  uploadAvatar(file: File): void {
    this.isUploading.set(true);
    this.errorMessage.set('');

    // Especificamos la carpeta 'users' para organizar mejor las fotos
    this.mediaService.uploadFile(file, 'users').subscribe({
      next: (relativePath) => {
        console.log('✅ Avatar subido:', relativePath);

        // 1. Actualizamos visualmente
        this.avatarUrl.set(this.mediaService.toPublicUrl(relativePath));

        // 2. Actualizamos el formulario con el path relativo
        this.profileForm.patchValue({ avatar: relativePath });

        // 3. Guardamos en BD inmediatamente (UX mejora)
        this.saveAvatarToDatabase(relativePath);
      },
      error: (err) => {
        console.error('❌ Error subiendo fichero:', err);
        this.isUploading.set(false);
        this.errorMessage.set('Error al subir la imagen.');
      },
    });
  }

  saveAvatarToDatabase(relativePath: string) {
    const formValues = this.profileForm.value;
    const dto = {
      name: formValues.name,
      surname: formValues.surname,
      email: formValues.email,
      phone: formValues.phone,
      avatar: relativePath,
    };

    this.studentService
      .updateProfile(dto)
      .pipe(finalize(() => this.isUploading.set(false)))
      .subscribe({
        next: () => {
          this.successMessage.set('¡Foto de perfil actualizada!');

          // Actualizamos estado global de Auth para que el navbar se entere
          this.authService.updateUserFields({
            ...dto,
            avatar: relativePath,
          });

          setTimeout(() => this.successMessage.set(''), 3000);
        },
        error: (err) => {
          console.error('Error guardando perfil:', err);
          this.errorMessage.set('La imagen se subió, pero no se pudo guardar en tu perfil.');
        },
      });
  }

  onSubmit() {
    if (this.profileForm.invalid) return;

    this.successMessage.set('');
    this.errorMessage.set('');

    const formValues = this.profileForm.value;

    // Validaciones extra de contraseña
    if (formValues.newPassword && formValues.newPassword !== formValues.confirmPassword) {
      this.errorMessage.set('Las contraseñas no coinciden.');
      return;
    }

    if (formValues.newPassword && !formValues.currentPassword) {
      this.errorMessage.set('Por seguridad, introduce tu contraseña actual para cambiarla.');
      return;
    }

    this.loading.set(true);

    const dto = {
      name: formValues.name,
      surname: formValues.surname,
      email: formValues.email,
      phone: formValues.phone,
      newPassword: formValues.newPassword,
      currentPassword: formValues.currentPassword,
      avatar: formValues.avatar,
    };

    this.studentService
      .updateProfile(dto)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.successMessage.set('¡Perfil actualizado correctamente!');

          // Limpiamos campos de password
          this.profileForm.patchValue({
            currentPassword: '',
            newPassword: '',
            confirmPassword: '',
          });

          // Si cambió el email, forzamos logout
          if (dto.email !== this.currentEmail()) {
            alert(
              'Has cambiado tu correo electrónico. Por seguridad, debes iniciar sesión de nuevo.',
            );
            this.authService.logout();
          } else {
            // Actualizamos estado global
            this.authService.updateUserFields({
              name: dto.name,
              surname: dto.surname,
              email: dto.email,
              phone: dto.phone,
              avatar: dto.avatar,
            });
          }

          setTimeout(() => this.successMessage.set(''), 3000);
        },
        error: (error: HttpErrorResponse) => {
          console.error('❌ Error recibido:', error);
          if (error.status === 409) {
            this.errorMessage.set('Ese correo electrónico ya está en uso.');
          } else if (error.error?.error) {
            this.errorMessage.set(error.error.error);
          } else {
            this.errorMessage.set('Ocurrió un error al guardar los cambios.');
          }
        },
      });
  }
}
