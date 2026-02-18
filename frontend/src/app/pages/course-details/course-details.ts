
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { CourseService } from '../../services/course/course';
import { MediaService } from '../../services/media/media';

@Component({
  selector: 'app-course-details',
  standalone: true,
  imports: [RouterLink, CommonModule, CurrencyPipe],
  templateUrl: './course-details.html',
  styleUrl: './course-details.scss',
})
export class CourseDetails implements OnInit {
  private route = inject(ActivatedRoute);
  private courseService = inject(CourseService);
  public mediaService = inject(MediaService); 

  
  course = signal<any>(null);
  modules = signal<any[]>([]);
  loading = signal<boolean>(true);

  courseId: string = '';

  ngOnInit() {
    this.courseId = this.route.snapshot.paramMap.get('id') || '';
    
    if (this.courseId) {
      this.loadCourseData();
      this.loadSyllabus();
    }
  }

  
  loadCourseData() {
    this.loading.set(true);
    this.courseService.getCourseById(this.courseId).subscribe({
      next: (data) => {
        this.course.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error cargando curso:', err);
        this.loading.set(false);
      }
    });
  }

  
  loadSyllabus() {
    this.courseService.getCourseModules(this.courseId).subscribe((modulesData) => {
      
      this.modules.set(modulesData);

      
      modulesData.forEach((modulo) => {
        this.courseService.getModuleLessons(modulo.id).subscribe((lessonsData) => {
          modulo.lessons = lessonsData;
          
          this.modules.update((current) => [...current]); 
        });
      });
    });
  }

  
  getCoverImageUrl(): string {
    const c = this.course();
    if (!c || !c.coverImage) return 'assets/img/placeholder-course.jpg'; 
    return this.mediaService.toPublicUrl(c.coverImage);
  }

  

  openLessonPdf(lessonId: string) {
    this.mediaService.getLessonPdfBlob(lessonId, false).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener');
        
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (err) => {
        this.handlePdfError(err);
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
        this.handlePdfError(err);
      },
    });
  }

  private handlePdfError(err: any) {
    if (err?.status === 403) {
      alert('🔒 No tienes acceso a este contenido. Por favor, matricúlate en el curso.');
    } else {
      console.error('Error con el PDF:', err);
      alert('Ocurrió un error al cargar el documento.');
    }
  }
}