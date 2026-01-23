import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef, NgZone } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { StudentService } from '../../services/student/student';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ProgressService } from '../../services/progress';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // 👈 IMPORTANTE
import { MediaService } from '../../services/media/media';
import { QuizService } from '../../services/quiz/quiz';
@Component({
  selector: 'app-course-player',
  standalone: true,
  imports: [RouterModule, CommonModule, FormsModule], // 👈 Añadido FormsModule
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

  // ✅ PDF via Blob
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

  // 👇👇👇 NUEVAS VARIABLES PARA QUIZ 👇👇👇
  activeQuiz: any = null; // Guarda el examen cargado
  quizLoading = false;
  quizSubmitted = false;
  quizScore: number | null = null;
  quizPassed = false;
  userAnswers: { [questionId: string]: string } = {}; // { "uuid-pregunta": "uuid-opcion" }
  // 👆👆👆 FIN NUEVAS VARIABLES 👆👆👆

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      this.courseId = params.get('id') || '';
      if (this.courseId) {
        this.loadCourseData();
        this.loadProgress();
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

  loadCourseData() {
    this.loading = true;
    this.studentService.getCourseContent(this.courseId).subscribe({
      next: (data) => {
        this.course = data;

        const modulesList = this.course.modules || this.course.sections || [];

        this.totalLessons = 0;
        modulesList?.forEach((module: any) => {
          if (module.lessons) this.totalLessons += module.lessons.length;
        });

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

  loadProgress() {
    this.progressService.getCourseProgress(this.courseId).subscribe({
      next: (ids) => {
        this.completedLessonIds = new Set(ids);
        this.calculateProgress();
      },
      error: (err) => console.error('Error cargando progreso', err),
    });
  }

  toggleLessonCompletion(lessonId: string) {
    const wasCompleted = this.completedLessonIds.has(lessonId);

    if (wasCompleted) this.completedLessonIds.delete(lessonId);
    else this.completedLessonIds.add(lessonId);

    this.calculateProgress();

    this.progressService.toggleProgress(lessonId).subscribe({
      next: (isCompletedBackend) => {
        if (isCompletedBackend) this.completedLessonIds.add(lessonId);
        else this.completedLessonIds.delete(lessonId);
        this.calculateProgress();
      },
      error: (err) => {
        console.error('Error guardando progreso', err);
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
    if (this.totalLessons === 0) {
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

    // Reset PDF state
    this.pdfError = null;
    this.safePdfUrl = null;
    this.revokePdfObjectUrl();

    // 👇 RESET STATE QUIZ (Limpiamos datos del examen anterior)
    this.activeQuiz = null;
    this.quizSubmitted = false;
    this.quizScore = null;
    this.quizPassed = false;
    this.userAnswers = {};

    // 1. VIDEO
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

    // 2. PDF: NO construyas /uploads/pdfs/**. Pide blob por endpoint autenticado.
    if (lesson.pdfUrl) {
      this.loadPdfPreview(lesson.id);
    }

    // 3. QUIZ: Si la lección tiene examen, lo cargamos 👈
    // Asumimos que el backend devuelve un campo 'quizId' en la lección
    if (lesson.quizId) {
      this.loadQuiz(lesson.quizId);
    }
  }

  // 👇👇👇 LÓGICA DE QUIZ (MÉTODOS NUEVOS) 👇👇👇

  loadQuiz(quizId: string) {
    this.quizLoading = true;

    // 3. USA quizService EN LUGAR DE studentService
    // Y recuerda que el método lo llamamos 'getQuizById' en el servicio anterior
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
    if (this.quizSubmitted) return; // No dejar cambiar si ya envió
    this.userAnswers[questionId] = optionId;
  }

  submitQuiz() {
    if (!this.activeQuiz) return;

    // 1. Mostrar spinner
    this.quizLoading = true;
    this.cdr.detectChanges(); // Forzamos vista de carga

    const payload = {
      quizId: this.activeQuiz.id,
      answers: this.userAnswers,
    };

    this.quizService.submitQuiz(payload).subscribe({
      next: (result: any) => {
        console.log('✅ RESPUESTA RECIBIDA:', result);

        // 2. EJECUTAR DENTRO DE NGZONE (Seguridad Anti-Fallos de Angular)
        this.ngZone.run(() => {
          // A. PRIMERO: Actualizamos los datos visuales (Lo más importante)
          this.quizScore = result.score;
          this.quizPassed = result.passed;
          this.quizSubmitted = true; // Esto activa la pantalla de resultados
          this.quizLoading = false; // Esto quita el spinner

          // B. SEGUNDO: Intentamos marcar como completada (Lógica secundaria)
          // Usamos un try-catch para que si esto falla, NO rompa la pantalla de la nota
          try {
            if (this.quizPassed && this.currentLesson && !this.isCompleted(this.currentLesson.id)) {
              console.log('🎉 Aprobado -> Marcando lección como completada...');
              this.toggleLessonCompletion(this.currentLesson.id);
            }
          } catch (e) {
            console.warn('⚠️ Error menor actualizando progreso (pero el examen está bien):', e);
          }

          // C. TERCERO: Forzamos el repintado final
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        console.error('❌ ERROR:', err);
        this.ngZone.run(() => {
          this.quizLoading = false;
          this.cdr.detectChanges();
        });
        alert('Hubo un error al enviar el examen.');
      },
    });
  }
  // 👆👆👆 FIN LÓGICA DE QUIZ 👆👆👆

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
        this.pdfError =
          err?.status === 403
            ? 'No tienes acceso a este PDF (no matriculado).'
            : 'Error cargando el PDF.';
        console.error('🔥 Error PDF:', err);
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
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (err) => console.error('🔥 Error abriendo PDF:', err),
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
      error: (err) => console.error('🔥 Error descargando PDF:', err),
    });
  }

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
