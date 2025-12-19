import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CourseService } from '../../../services/course/course';

@Component({
  selector: 'app-course-form',
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './course-form.html',
  styleUrl: './course-form.scss',
})
export class CourseForm {
  private fb = inject(FormBuilder);
  private courseService = inject(CourseService);
  private router = inject(Router);
  // Definimos el formulario con validaciones
  courseForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(5)]],
    description: ['', [Validators.required, Validators.maxLength(500)]],
    price: [0, [Validators.required, Validators.min(0)]],
    hours: [0, [Validators.required, Validators.min(1)]],
    level: ['BEGINNER', Validators.required], // Por defecto BEGINNER
  });

  onSubmit() {
    if (this.courseForm.valid) {
      // Llamamos al servicio para crear
      this.courseService.createCourse(this.courseForm.value).subscribe({
        next: () => {
          alert('Curso creado con éxito');
          this.router.navigate(['/admin']); // Volver al panel
        },
        error: (err) => {
          console.error(err);
          alert('Error al crear el curso');
        },
      });
    }
  }
}
