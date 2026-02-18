import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CourseService } from '../../../services/course/course';
import { Location, CommonModule } from '@angular/common';

@Component({
  selector: 'app-lesson-form',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './lesson-form.html',
  styleUrl: './lesson-form.scss',
})
export class LessonForm implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private courseService = inject(CourseService);
  private location = inject(Location);

  moduleId: string = '';
  lessonId: string = '';
  isEditMode: boolean = false;
  selectedFile: File | null = null; 

  form: FormGroup = this.fb.group({
    title: ['', [Validators.required]],
    videoUrl: [''],
    pdfUrl: [''], 
    orderIndex: [1, [Validators.required]],
  });

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.lessonId = id;
      this.loadLessonData(id);
    } else {
      this.moduleId = this.route.snapshot.paramMap.get('moduleId') || '';
    }
  }

  loadLessonData(id: string) {
    this.courseService.getLessonById(id).subscribe({
      next: (lesson) => {
        this.form.patchValue({
          title: lesson.title,
          videoUrl: lesson.videoUrl,
          pdfUrl: lesson.pdfUrl, 
          orderIndex: lesson.orderIndex,
        });
      },
      error: () => alert('Error al cargar'),
    });
  }

  
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      
      this.selectedFile = file; 

      this.form.patchValue({
        pdfUrl: file.name,
      });
      this.form.get('pdfUrl')?.updateValueAndValidity();
    }
  }

  onSubmit() {
    if (this.form.invalid) return;

    const formData = new FormData();
    formData.append('title', this.form.get('title')?.value);
    formData.append('videoUrl', this.form.get('videoUrl')?.value || '');
    formData.append('orderIndex', this.form.get('orderIndex')?.value);

    if (!this.isEditMode) {
      formData.append('moduleId', this.moduleId);
    }

    if (this.selectedFile) {
      
      console.log('📎 Adjuntando archivo:', this.selectedFile.name);
      formData.append('file', this.selectedFile);
    } else {
      console.log('⚠️ No hay archivo seleccionado para subir.');
    } 

    const request$ = this.isEditMode
      ? this.courseService.updateLesson(this.lessonId, formData)
      : this.courseService.createLesson(formData);

    request$.subscribe({
      next: () => {
        console.log('✅ Lección guardada con éxito');
        this.location.back();
      },
      error: (err) => {
        console.error('❌ Error al guardar:', err);
        alert('Hubo un error al guardar la lección. Revisa la consola.');
      },
    });
  }

  goBack() {
    this.location.back();
  }
}
