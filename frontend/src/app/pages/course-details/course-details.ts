// course-details.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CourseService } from '../../services/course/course';
import { CommonModule } from '@angular/common';
import { MediaService } from '../../services/media/media';

@Component({
  selector: 'app-course-details',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './course-details.html',
  styleUrl: './course-details.scss',
})
export class CourseDetails implements OnInit {
  private route = inject(ActivatedRoute);
  private courseService = inject(CourseService);
  private mediaService = inject(MediaService);

  modules = signal<any[]>([]);
  courseId: string = '';

  ngOnInit() {
    this.courseId = this.route.snapshot.paramMap.get('id') || '';
    if (this.courseId) this.loadSyllabus();
  }

  loadSyllabus() {
    this.courseService.getCourseModules(this.courseId).subscribe((modulesData) => {
      modulesData.forEach((modulo) => {
        this.courseService.getModuleLessons(modulo.id).subscribe((lessonsData) => {
          modulo.lessons = lessonsData;
          // refresca señal (por si no actualiza la vista al mutar nested)
          this.modules.set([...modulesData]);
        });
      });

      this.modules.set(modulesData);
    });
  }

  openLessonPdf(lessonId: string) {
    this.mediaService.getLessonPdfBlob(lessonId, false).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (err) => {
        if (err?.status === 403)
          console.error('403: No tienes acceso a este PDF (no matriculado o no logueado).');
        else console.error('Error abriendo PDF', err);
      },
    });
  }

  downloadLessonPdf(lessonId: string, title?: string) {
    this.mediaService.getLessonPdfBlob(lessonId, true).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;

        const safeTitle = (title || 'lesson').replace(/[\\/:*?"<>|]/g, '').trim();
        a.download = `${safeTitle || 'lesson'}.pdf`;

        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        if (err?.status === 403)
          console.error('403: No tienes acceso a este PDF (no matriculado o no logueado).');
        else console.error('Error descargando PDF', err);
      },
    });
  }
}
