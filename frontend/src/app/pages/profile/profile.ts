import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { StudentService } from '../../services/student/student';
import { AuthService } from '../../services/auth/auth';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
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
  private mediaService = inject(MediaService);

  // Variable para recordar el email original
  currentEmail: string = '';

  // 👇 NUEVO: Variable para controlar la imagen visualmente
  avatarUrl: string | null = null;

  profileForm: FormGroup;
  loading = false;
  successMessage = '';
  errorMessage = '';
  isUploading = false;

  constructor() {
    this.profileForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      surname: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      currentPassword: [''],
      newPassword: ['', [Validators.minLength(6)]],
      confirmPassword: [''],
      avatar: [''],
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
          this.currentEmail = user.email;

          // 👇 NUEVO: Guardamos la URL para mostrarla
          this.avatarUrl = user.avatar;

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
          this.errorMessage = 'No se pudieron cargar tus datos.';
        },
      });
  }

  // 👇 NUEVO: Si la imagen falla (404/403), cargamos una generada
  handleImageError() {
    const name = this.profileForm.get('name')?.value || 'U';
    // Usamos ui-avatars para generar una imagen con la inicial
    this.avatarUrl = `https://ui-avatars.com/api/?name=${name}&background=random&color=fff&size=128`;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.uploadAvatar(file);
    }
  }

  uploadAvatar(file: File): void {
    this.isUploading = true;
    this.errorMessage = '';

    this.mediaService
      .uploadImage(file)
      .pipe
      // No usamos finalize aquí para mantener el spinner hasta que guardemos en BD
      ()
      .subscribe({
        next: (url) => {
          console.log('✅ Imagen subida a disco. URL:', url);

          // 1. Actualizamos visualmente
          this.avatarUrl = url;
          this.profileForm.patchValue({ avatar: url });

          // 2. AUTO-GUARDADO: Llamamos al backend para vincular la foto al usuario
          this.saveAvatarToDatabase(url);
        },
        error: (err) => {
          console.error('❌ Error subiendo fichero:', err);
          this.isUploading = false;
          this.errorMessage = 'Error al subir la imagen.';
          this.cd.detectChanges();
        },
      });
  }

  // 👇 NUEVO MÉTODO AUXILIAR PARA GUARDAR EN BD
  saveAvatarToDatabase(url: string) {
    const formValues = this.profileForm.value;

    // Preparamos los datos (sin tocar passwords)
    const dto = {
      name: formValues.name,
      surname: formValues.surname,
      email: formValues.email,
      phone: formValues.phone,
      avatar: url, // 👈 La clave: enviamos la nueva URL
      // No enviamos passwords para no activar validaciones extrañas
    };

    this.studentService
      .updateProfile(dto)
      .pipe(
        finalize(() => {
          this.isUploading = false;
          this.cd.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          console.log('💾 Avatar guardado en base de datos');
          this.successMessage = '¡Foto de perfil actualizada!';

          // Actualizamos la sesión para que el Navbar se entere también
          this.authService.updateUserFields({
            ...dto,
            avatar: url,
          });

          // Quitamos el mensaje de éxito a los 3 segundos para que quede limpio
          setTimeout(() => (this.successMessage = ''), 3000);
        },
        error: (err) => {
          console.error('Error guardando perfil:', err);
          this.errorMessage = 'La imagen se subió, pero no se pudo guardar en tu perfil.';
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

    if (formValues.newPassword && !formValues.currentPassword) {
      this.errorMessage = 'Por seguridad, introduce tu contraseña actual para cambiarla.';
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
      avatar: formValues.avatar,
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

          this.profileForm.patchValue({
            currentPassword: '',
            newPassword: '',
            confirmPassword: '',
          });

          if (dto.email !== this.currentEmail) {
            alert(
              'Has cambiado tu correo electrónico. Por seguridad, debes iniciar sesión de nuevo.'
            );
            this.authService.logout();
          } else {
            this.authService.updateUserFields({
              name: dto.name,
              surname: dto.surname,
              email: dto.email,
              phone: dto.phone,
              avatar: dto.avatar,
            });
          }
        },
        error: (error: HttpErrorResponse) => {
          console.error('❌ Error recibido:', error);

          if (error.status === 409) {
            this.errorMessage = 'Ese correo electrónico ya está en uso.';
          } else if (error.error && error.error.error) {
            this.errorMessage = error.error.error;
          } else {
            this.errorMessage = 'Ocurrió un error al guardar.';
          }
        },
      });
  }
}
