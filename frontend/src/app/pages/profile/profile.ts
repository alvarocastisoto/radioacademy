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
  public mediaService = inject(MediaService); // 👈 Hacemos público para usar toPublicUrl en el HTML si hiciera falta

  currentEmail: string = '';
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

          // ✅ CORRECCIÓN 1: Convertimos el path relativo (uploads/...) a URL completa
          // para que el navegador pueda mostrar la imagen.
          this.avatarUrl = this.mediaService.toPublicUrl(user.avatar);

          this.profileForm.patchValue({
            name: user.name,
            surname: user.surname,
            email: user.email,
            phone: user.phone,
            avatar: user.avatar, // En el form guardamos el path relativo (para BD)
          });
        },
        error: (err: any) => {
          // 👈 Tipamos como any para evitar quejas
          console.error('Error cargando perfil', err);
          this.errorMessage = 'No se pudieron cargar tus datos.';
        },
      });
  }

  handleImageError() {
    const name = this.profileForm.get('name')?.value || 'U';
    // Si falla la imagen, ponemos un avatar por defecto
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

    // ✅ CORRECCIÓN 2: El método se llama uploadFile, no uploadImage
    this.mediaService
      .uploadFile(file)
      .pipe()
      .subscribe({
        next: (relativePath) => {
          // El backend devuelve "uploads/images/xxx.jpg"
          console.log('✅ Imagen subida a disco. Path:', relativePath);

          // 1. Actualizamos visualmente (Convertimos a URL completa)
          this.avatarUrl = this.mediaService.toPublicUrl(relativePath);

          // 2. Actualizamos el formulario con el path relativo
          this.profileForm.patchValue({ avatar: relativePath });

          // 3. Guardamos en BD (enviamos el path relativo, que es lo correcto)
          this.saveAvatarToDatabase(relativePath);
        },
        error: (err: any) => {
          console.error('❌ Error subiendo fichero:', err);
          this.isUploading = false;
          this.errorMessage = 'Error al subir la imagen.';
          this.cd.detectChanges();
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
      avatar: relativePath, // 👈 Enviamos path relativo (uploads/...)
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

          this.authService.updateUserFields({
            ...dto,
            avatar: relativePath,
          });

          setTimeout(() => (this.successMessage = ''), 3000);
        },
        error: (err: any) => {
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
