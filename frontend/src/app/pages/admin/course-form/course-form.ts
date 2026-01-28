import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CourseService } from '../../../services/course/course';
import { MediaService } from '../../../services/media/media';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-course-form',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, CommonModule],
  templateUrl: './course-form.html',
  styleUrl: './course-form.scss',
})
export class CourseForm {
  private fb = inject(FormBuilder);
  private courseService = inject(CourseService);
  public mediaService = inject(MediaService); // 👈 Público por si lo usas en el HTML
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  // Variables para la vista previa
  isUploading = false;
  coverPreview: string | null = null;

  courseForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(5)]],
    description: ['', [Validators.required, Validators.maxLength(500)]],
    price: [0, [Validators.required, Validators.min(0)]],
    hours: [0, [Validators.required, Validators.min(1)]],
    level: ['BEGINNER', Validators.required],
    coverImage: [''],
  });

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.isUploading = true;
      this.cdr.detectChanges();

      const file = input.files[0];

      // ✅ CORRECCIÓN 1: uploadFile (no uploadImage)
      this.mediaService.uploadFile(file, 'courses').subscribe({
        next: (relativePath) => {
          // 1. Guardamos el path relativo en el formulario (para la BD)
          this.courseForm.patchValue({ coverImage: relativePath });

          // 2. ✅ CORRECCIÓN 2: Generamos URL completa para la vista previa visual
          this.coverPreview = this.mediaService.toPublicUrl(relativePath);

          this.isUploading = false;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          // 👈 Tipado any para evitar quejas
          console.error(err);
          alert('Error al subir la imagen');

          this.isUploading = false;
          this.cdr.detectChanges();
        },
      });
    }
  }

  onSubmit() {
    if (this.courseForm.valid) {
      this.courseService.createCourse(this.courseForm.value).subscribe({
        next: () => {
          alert('Curso creado con éxito');
          this.router.navigate(['/admin']);
        },
        error: (err: any) => {
          console.error(err);
          alert('Error al crear el curso');
        },
      });
    }
  }
}
