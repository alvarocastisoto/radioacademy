import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CourseService } from '../../../services/course/course';
import { Location } from '@angular/common'; // Útil para el botón "Atrás"

@Component({
  selector: 'app-lesson-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './lesson-form.html',
  styleUrl: './lesson-form.scss',
})
export class LessonForm implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private courseService = inject(CourseService);
  private location = inject(Location); // Para volver atrás fácil

  moduleId: string = '';

  form: FormGroup = this.fb.group({
    title: ['', [Validators.required]],
    videoUrl: ['', [Validators.required]], // Aquí pegarán el link de YouTube
    orderIndex: [1, [Validators.required]],
  });

  ngOnInit() {
    this.moduleId = this.route.snapshot.paramMap.get('moduleId') || '';
  }

  onSubmit() {
    if (this.form.valid && this.moduleId) {
      const lessonData = {
        ...this.form.value,
        moduleId: this.moduleId,
      };

      this.courseService.createLesson(lessonData).subscribe(() => {
        // Volvemos atrás (al temario)
        this.location.back();
      });
    }
  }

  goBack() {
    this.location.back();
  }
}
