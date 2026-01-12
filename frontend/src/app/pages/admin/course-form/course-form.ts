import { Component, inject, ChangeDetectorRef } from '@angular/core'; // 👈 1. Importar ChangeDetectorRef
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
  private mediaService = inject(MediaService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef); // 👈 2. Inyectar CDR

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

  // 👇 Lógica de subida CORREGIDA
  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.isUploading = true;
      this.cdr.detectChanges(); // 👈 Forzar actualización para mostrar el spinner YA

      const file = input.files[0];

      this.mediaService.uploadImage(file).subscribe({
        next: (url) => {
          this.courseForm.patchValue({ coverImage: url });
          this.coverPreview = url;

          this.isUploading = false;
          this.cdr.detectChanges(); // 👈 IMPORTANTE: Avisar a Angular que ya terminó
        },
        error: (err) => {
          console.error(err);
          alert('Error al subir la imagen');

          this.isUploading = false;
          this.cdr.detectChanges(); // 👈 IMPORTANTE: Quitar spinner aunque falle
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
        error: (err) => {
          console.error(err);
          alert('Error al crear el curso');
        },
      });
    }
  }
}
