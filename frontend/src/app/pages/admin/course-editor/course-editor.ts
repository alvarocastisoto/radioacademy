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
          
          this.editForm.patchValue({
            title: course.title,
            description: course.description,
            price: course.price,
            hours: course.hours,
            coverImage: course.coverImage,
          });

          
          if (course.coverImage) {
            
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
          
          this.loading = false;
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('❌ Error de red/servidor:', err);
        alert('Error al cargar el curso. Mira la consola.');
        this.loading = false; 
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

    
    if (this.selectedFile) {
      
      this.mediaService.uploadFile(this.selectedFile).subscribe({
        next: (relativePath) => {
          
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
      
      this.saveCourseChanges();
    }
  }

  saveCourseChanges() {
    const request = this.editForm.value;

    
    

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
