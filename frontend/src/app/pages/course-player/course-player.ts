import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef, NgZone } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { StudentService } from '../../services/student/student';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ProgressService } from '../../services/progress';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MediaService } from '../../services/media/media';
import { QuizService } from '../../services/quiz/quiz';

@Component({
  selector: 'app-course-player',
  standalone: true,
  imports: [RouterModule, CommonModule, FormsModule],
  templateUrl: './course-player.html',
  styleUrls: ['./course-player.scss'],
})
export class CoursePlayerComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private studentService = inject(StudentService);
  private progressService = inject(ProgressService);
  private mediaService = inject(MediaService);
  private sanitizer = inject(DomSanitizer);
  private cdr = inject(ChangeDetectorRef);
  private quizService = inject(QuizService);
  private ngZone = inject(NgZone);

  course: any = null;
  currentLesson: any = null;

  // Estados de media
  safeVideoUrl: SafeResourceUrl | null = null;
  safePdfUrl: SafeResourceUrl | null = null;
  private pdfObjectUrl: string | null = null;
  pdfLoading = false;
  pdfError: string | null = null;

  loading = true;
  isYouTube: boolean = false;
  openModules: Set<string> = new Set();

  courseId: string = '';
  completedLessonIds: Set<string> = new Set();
  progressPercentage: number = 0;
  totalLessons: number = 0;

  // Variables Quiz
  activeQuiz: any = null;
  quizLoading = false;
  quizSubmitted = false;
  quizScore: number | null = null;
  quizPassed = false;
  userAnswers: { [questionId: string]: string } = {};

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      this.courseId = params.get('id') || '';
      if (this.courseId) {
        this.initData();
      } else {
        console.error('❌ ERROR: No llega ningún ID en la URL');
      }
    });
  }

  ngOnDestroy() {
    this.revokePdfObjectUrl();
  }

  private revokePdfObjectUrl() {
    if (this.pdfObjectUrl) {
      URL.revokeObjectURL(this.pdfObjectUrl);
      this.pdfObjectUrl = null;
    }
  }

  initData() {
    this.loading = true;
    this.loadCourseData();
    this.loadProgress();
  }

  loadCourseData() {
    this.studentService.getCourseContent(this.courseId).subscribe({
      next: (data) => {
        this.course = data;

        // Calcular total de lecciones
        const modulesList = this.course.modules || this.course.sections || [];
        this.totalLessons = 0;
        modulesList?.forEach((module: any) => {
          if (module.lessons) this.totalLessons += module.lessons.length;
        });

        // Abrir primer módulo y seleccionar lección
        if (modulesList.length > 0) {
          const firstModule = modulesList[0];
          this.toggleModule(firstModule.id);
          if (firstModule.lessons?.length > 0) {
            this.selectLesson(firstModule.lessons[0]);
          }
        }

        this.calculateProgress();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('🔥 Error cargando curso:', err);
        this.loading = false;
      },
    });
  }

  // 👇 CORRECCIÓN IMPORTANTE AQUÍ 👇
  loadProgress() {
    console.log('🔄 Cargando progreso...');
    this.progressService.getCourseProgress(this.courseId).subscribe({
      next: (response: any) => {
        console.log('✅ Respuesta Progreso:', response);

        let ids: string[] = [];

        // Caso 1: El backend devuelve el array directo ['id1', 'id2']
        if (Array.isArray(response)) {
          ids = response;
        }
        // Caso 2 (TU CASO): El backend devuelve objeto { completedLessonIds: [...] }
        else if (response && Array.isArray(response.completedLessonIds)) {
          ids = response.completedLessonIds;
        }

        this.completedLessonIds = new Set(ids);
        this.calculateProgress();
        this.cdr.detectChanges();
      },
      error: (err) => console.error('❌ Error cargando progreso', err),
    });
  }

  toggleLessonCompletion(lessonId: string) {
    const wasCompleted = this.completedLessonIds.has(lessonId);

    // Optimistic Update
    if (wasCompleted) this.completedLessonIds.delete(lessonId);
    else this.completedLessonIds.add(lessonId);

    this.calculateProgress();

    this.progressService.toggleProgress(lessonId).subscribe({
      next: (res) => {
        console.log('Guardado OK', res);
      },
      error: (err) => {
        console.error('❌ Error guardando progreso:', err);
        // Rollback
        if (wasCompleted) this.completedLessonIds.add(lessonId);
        else this.completedLessonIds.delete(lessonId);
        this.calculateProgress();
      },
    });
  }

  isCompleted(lessonId: string): boolean {
    return this.completedLessonIds.has(lessonId);
  }

  calculateProgress() {
    if (!this.totalLessons || this.totalLessons === 0) {
      this.progressPercentage = 0;
      return;
    }
    const completedCount = this.completedLessonIds.size;
    this.progressPercentage = Math.round((completedCount / this.totalLessons) * 100);
    if (this.progressPercentage > 100) this.progressPercentage = 100;
  }

  // ==========================================
  // REPRODUCTOR
  // ==========================================
  selectLesson(lesson: any) {
    this.currentLesson = lesson;
    this.isYouTube = false;
    this.pdfError = null;
    this.safePdfUrl = null;
    this.revokePdfObjectUrl();

    // Reset Quiz
    this.activeQuiz = null;
    this.quizSubmitted = false;
    this.quizScore = null;
    this.quizPassed = false;
    this.userAnswers = {};

    // VIDEO
    if (lesson.videoUrl) {
      if (this.isYouTubeUrl(lesson.videoUrl)) {
        this.isYouTube = true;
        const embedUrl = this.getYouTubeEmbedUrl(lesson.videoUrl);
        this.safeVideoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
      } else {
        this.safeVideoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(lesson.videoUrl);
      }
    } else {
      this.safeVideoUrl = null;
    }

    // PDF
    if (lesson.pdfUrl) {
      this.loadPdfPreview(lesson.id);
    }

    // QUIZ
    if (lesson.quizId) {
      this.loadQuiz(lesson.quizId);
    }
  }

  // --- QUIZ ---
  loadQuiz(quizId: string) {
    this.quizLoading = true;
    this.quizService.getQuizById(quizId).subscribe({
      next: (quiz) => {
        this.activeQuiz = quiz;
        this.quizLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando quiz', err);
        this.quizLoading = false;
      },
    });
  }

  selectOption(questionId: string, optionId: string) {
    if (this.quizSubmitted) return;
    this.userAnswers[questionId] = optionId;
  }

  submitQuiz() {
    if (!this.activeQuiz) return;
    this.quizLoading = true;
    this.cdr.detectChanges();

    const payload = {
      quizId: this.activeQuiz.id,
      answers: this.userAnswers,
    };

    this.quizService.submitQuiz(payload).subscribe({
      next: (result: any) => {
        this.ngZone.run(() => {
          this.quizScore = result.score;
          this.quizPassed = result.passed;
          this.quizSubmitted = true;
          this.quizLoading = false;

          if (this.quizPassed && this.currentLesson && !this.isCompleted(this.currentLesson.id)) {
            this.toggleLessonCompletion(this.currentLesson.id);
          }
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.quizLoading = false;
          this.cdr.detectChanges();
        });
        alert('Hubo un error al enviar el examen.');
      },
    });
  }

  // --- PDF ---
  private loadPdfPreview(lessonId: string) {
    this.pdfLoading = true;
    this.pdfError = null;
    this.mediaService.getLessonPdfBlob(lessonId, false).subscribe({
      next: (blob) => {
        this.revokePdfObjectUrl();
        this.pdfObjectUrl = URL.createObjectURL(blob);
        this.safePdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfObjectUrl);
        this.pdfLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.pdfLoading = false;
        this.pdfError = err?.status === 403 ? 'Acceso denegado.' : 'Error cargando PDF.';
        this.cdr.detectChanges();
      },
    });
  }

  openPdfNewTab() {
    if (this.pdfObjectUrl) {
      window.open(this.pdfObjectUrl, '_blank', 'noopener');
      return;
    }
    this.mediaService.getLessonPdfBlob(this.currentLesson.id, false).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener');
        setTimeout(() => URL.revokeObjectURL(url), 60000);
      },
      error: (err) => console.error('Error abriendo PDF:', err),
    });
  }

  downloadPdf() {
    this.mediaService.getLessonPdfBlob(this.currentLesson.id, true).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${this.currentLesson.title || 'lesson'}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Error descargando PDF:', err),
    });
  }

  // --- UTILS ---
  private isYouTubeUrl(url: string): boolean {
    return url.includes('youtube.com') || url.includes('youtu.be');
  }

  private getYouTubeEmbedUrl(url: string): string {
    let videoId = '';
    if (url.includes('youtu.be')) videoId = url.split('/').pop()?.split('?')[0] || '';
    else if (url.includes('watch?v=')) videoId = url.split('v=')[1]?.split('&')[0] || '';
    else if (url.includes('embed/')) return url;
    return `https://www.youtube.com/embed/${videoId}`;
  }

  toggleModule(moduleId: string) {
    if (this.openModules.has(moduleId)) this.openModules.delete(moduleId);
    else this.openModules.add(moduleId);
  }

  isModuleOpen(moduleId: string): boolean {
    return this.openModules.has(moduleId);
  }
}
