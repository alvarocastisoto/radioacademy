import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CourseService } from '../../../services/course/course';

@Component({
  selector: 'app-module-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './module-form.html',
  styleUrl: './module-form.scss',
})
export class ModuleForm implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute); // Para leer la URL
  private router = inject(Router); // Para navegar al terminar
  private courseService = inject(CourseService);

  courseId: string = '';

  form: FormGroup = this.fb.group({
    title: ['', [Validators.required]],
    orderIndex: [1, [Validators.required]],
  });

  ngOnInit() {
    // Capturamos el ID del curso de la URL
    this.courseId = this.route.snapshot.paramMap.get('courseId') || '';
  }

  onSubmit() {
    if (this.form.valid && this.courseId) {
      const moduleData = {
        ...this.form.value,
        courseId: this.courseId, // Añadimos el ID del padre
      };

      this.courseService.createModule(moduleData).subscribe(() => {
        // Al terminar, volvemos a la lista de contenidos
        this.router.navigate(['/admin/courses', this.courseId, 'content']);
      });
    }
  }
}
