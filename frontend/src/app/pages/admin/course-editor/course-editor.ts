import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CourseService } from '../../../services/course/course';
import { MediaService } from '../../../services/media/media';

@Component({
  selector: 'app-edit-course',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './course-editor.html',
  styleUrl: './course-editor.scss',
})
export class EditCourseComponent implements OnInit {
  private fb = inject(FormBuilder);
  private courseService = inject(CourseService);
  private mediaService = inject(MediaService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);
  editForm: FormGroup;
  courseId: string = '';
  loading = true;
  submitting = false;

  previewImage: string | null = null;
  selectedFile: File | null = null;

  constructor() {
    this.editForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(5)]],
      description: ['', [Validators.required]],
      price: [0, [Validators.required, Validators.min(0)]],
      hours: [0, [Validators.required, Validators.min(1)]],
      coverImage: [''],
    });
  }

  ngOnInit(): void {
    this.courseId = this.route.snapshot.paramMap.get('id') || '';
    if (this.courseId) {
      this.loadCourseData();
    } else {
      this.router.navigate(['/admin/courses']);
    }
  }

  loadCourseData() {
    console.log('📡 Solicitando curso id:', this.courseId);

    this.courseService.getCourseById(this.courseId).subscribe({
      next: (course) => {
        console.log('📦 Datos recibidos del Backend:', course);

        try {
          // 1. Rellenamos el formulario (Usamos patchValue para ser tolerantes a faltas de campos)
          this.editForm.patchValue({
            title: course.title,
            description: course.description,
            price: course.price,
            hours: course.hours,
            coverImage: course.coverImage,
          });

          // 2. Previsualización de imagen (Protegemos contra nulos)
          if (course.coverImage) {
            // Si falla toPublicUrl, que no rompa toda la página
            try {
              this.previewImage = this.mediaService.toPublicUrl(course.coverImage);
            } catch (imgError) {
              console.warn('⚠️ Error generando URL de imagen:', imgError);
            }
          }

          console.log('✅ Formulario relleno correctamente');
        } catch (e) {
          console.error('🔥 Error CRÍTICO procesando datos en Angular:', e);
        } finally {
          // 3. ESTO ES LO IMPORTANTE: Quitar el loading pase lo que pase
          this.loading = false;
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('❌ Error de red/servidor:', err);
        alert('Error al cargar el curso. Mira la consola.');
        this.loading = false; // Quitar loading también si hay error
        this.router.navigate(['/admin/courses']);
      },
    });
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;

      const reader = new FileReader();
      reader.onload = () => {
        this.previewImage = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  onSubmit() {
    if (this.editForm.invalid) return;
    this.submitting = true;

    // A. ¿Hay nueva imagen? -> Subirla primero
    if (this.selectedFile) {
      // ✅ CORRECCIÓN 2: Pasamos el File directo (no FormData)
      this.mediaService.uploadFile(this.selectedFile).subscribe({
        next: (relativePath) => {
          // El servicio devuelve un string limpio (ej: "uploads/images/xxx.jpg")
          this.editForm.patchValue({ coverImage: relativePath });
          this.saveCourseChanges();
        },
        error: (err) => {
          console.error('Error subiendo imagen', err);
          this.submitting = false;
          alert('Error al subir la imagen nueva');
        },
      });
    } else {
      // B. No hay imagen nueva -> Guardamos directamente
      this.saveCourseChanges();
    }
  }

  saveCourseChanges() {
    const request = this.editForm.value;

    // Convertir a DTO si es necesario (ej: price a número si viene como string)
    // Angular suele manejarlo bien si el input es type="number"

    this.courseService.updateCourse(this.courseId, request).subscribe({
      next: () => {
        alert('✅ Curso actualizado correctamente');
        this.router.navigate(['/admin/courses']);
      },
      error: (err) => {
        console.error('Error actualizando curso', err);
        alert('Error al actualizar el curso.');
        this.submitting = false;
      },
    });
  }
}
