import { Component, OnInit, OnDestroy, inject, signal, computed, effect } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';


import { StudentService } from '../../services/student/student';
import { ProgressService } from '../../services/progress';
import { MediaService } from '../../services/media/media';
import { QuizService, QuizDTO, QuizResultDTO } from '../../services/quiz/quiz';


export interface Lesson {
  id: string;
  title: string;
  videoUrl?: string;
  pdfUrl?: string;
  duration?: number;
}

export interface Module {
  id: string;
  title: string;
  orderIndex: number;
  quizId?: string;
  lessons: Lesson[];
}

export interface CourseData {
  id: string;
  title: string;
  modules: Module[];
}

@Component({
  selector: 'app-course-player',
  standalone: true,
  imports: [RouterModule, CommonModule, FormsModule],
  templateUrl: './course-player.html',
  styleUrls: ['./course-player.scss'],
})
export class CoursePlayerComponent implements OnInit, OnDestroy {
  
  private route = inject(ActivatedRoute);
  private sanitizer = inject(DomSanitizer);

  
  private studentService = inject(StudentService);
  private progressService = inject(ProgressService);
  private mediaService = inject(MediaService);
  private quizService = inject(QuizService);

  
  courseId = signal<string>('');
  course = signal<CourseData | null>(null);
  loading = signal<boolean>(true);

  currentLesson = signal<Lesson | null>(null);
  currentModuleQuizId = signal<string | null>(null);

  viewMode = signal<'VIDEO' | 'PDF' | 'QUIZ' | 'EMPTY'>('EMPTY');
  openModules = signal<Set<string>>(new Set());

  
  completedLessonIds = signal<Set<string>>(new Set());
  totalLessons = computed(() => {
    const c = this.course();
    const modules = c?.modules || [];
    return modules.reduce((acc, m) => acc + (m.lessons?.length || 0), 0);
  });

  progressPercentage = computed(() => {
    if (this.totalLessons() === 0) return 0;
    const count = this.completedLessonIds().size;
    return Math.min(100, Math.round((count / this.totalLessons()) * 100));
  });

  
  safeVideoUrl = signal<SafeResourceUrl | null>(null);
  isYouTube = signal<boolean>(false);
  pdfState = signal({
    loading: false,
    error: null as string | null,
    safeUrl: null as SafeResourceUrl | null,
    objectUrl: null as string | null,
  });

  
  quizState = signal({
    loading: false,
    activeQuiz: null as QuizDTO | null,
    submitted: false,
    score: null as number | null,
    passed: false,
    userAnswers: {} as Record<string, string>,
  });

  resultDetails: QuizResultDTO | null = null;
  isSmartRetryMode = false;

  constructor() {
    effect(() => {
      if (this.viewMode() !== 'PDF') {
        this.revokePdfObjectUrl();
      }
    });
  }

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.courseId.set(id);
        this.initData();
      }
    });
  }

  ngOnDestroy() {
    this.revokePdfObjectUrl();
  }

  initData() {
    this.loading.set(true);
    this.studentService.getCourseContent(this.courseId()).subscribe({
      next: (data: any) => {
        const safeData: CourseData = {
          ...data,
          modules: data.modules || data.sections || [],
        };
        this.course.set(safeData);
        this.loading.set(false);
        if (safeData.modules.length > 0) {
          const firstMod = safeData.modules[0];
          this.toggleModule(firstMod.id);
          if (firstMod.lessons?.length > 0) {
            this.selectLesson(firstMod.lessons[0]);
          }
        }
      },
      error: (err) => {
        console.error('Error cargando curso', err);
        this.loading.set(false);
      },
    });
    this.loadProgress();
  }

  loadProgress() {
    this.progressService.getCourseProgress(this.courseId()).subscribe({
      next: (response: any) => {
        let ids: string[] = [];
        if (Array.isArray(response)) ids = response;
        else if (response?.completedLessonIds) ids = response.completedLessonIds;
        this.completedLessonIds.set(new Set(ids));
      },
    });
  }

  selectLesson(lesson: Lesson) {
    this.viewMode.set('EMPTY');
    this.safeVideoUrl.set(null);
    this.currentModuleQuizId.set(null);
    this.resultDetails = null;
    this.currentLesson.set(lesson);

    setTimeout(() => {
      if (lesson.videoUrl) {
        this.setupVideo(lesson.videoUrl);
        this.viewMode.set('VIDEO');
      } else if (lesson.pdfUrl) {
        this.loadPdfPreview(lesson.id);
      } else {
        this.viewMode.set('EMPTY');
      }
    }, 0);
  }

  private setupVideo(url: string) {
    const isYt = url.includes('youtube.com') || url.includes('youtu.be');
    this.isYouTube.set(isYt);
    let finalUrl = isYt ? this.getYouTubeEmbedUrl(url) : url;
    this.safeVideoUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(finalUrl));
  }

  openModuleQuiz(quizId: string) {
    this.isSmartRetryMode = false;
    this.currentLesson.set(null);
    this.currentModuleQuizId.set(quizId);
    this.viewMode.set('QUIZ');
    this.resultDetails = null;
    this.quizState.set({
      loading: true,
      activeQuiz: null,
      submitted: false,
      score: null,
      passed: false,
      userAnswers: {},
    });

    this.quizService.getQuizById(quizId).subscribe({
      next: (quiz) => this.quizState.update((s) => ({ ...s, activeQuiz: quiz, loading: false })),
      error: (err) => {
        console.error('Error cargando quiz', err);
        this.quizState.update((s) => ({ ...s, loading: false }));
      },
    });
  }

  
  loadSmartRetry(quizId?: string) {
    
    const finalId = quizId || this.currentModuleQuizId();

    if (!finalId) return;

    this.currentModuleQuizId.set(finalId);
    this.currentLesson.set(null);
    this.isSmartRetryMode = true;
    this.viewMode.set('QUIZ');

    this.resultDetails = null;
    this.quizState.update((s) => ({ ...s, loading: true, activeQuiz: null }));

    this.quizService.getSmartFailedQuiz(finalId).subscribe({
      next: (smartQuiz) => {
        
        if (!smartQuiz || !smartQuiz.questions || smartQuiz.questions.length === 0) {
          this.handleEmptyRetry();
        } else {
          this.quizState.update((s) => ({
            ...s,
            activeQuiz: smartQuiz,
            loading: false,
            submitted: false,
            score: null,
            passed: false,
            userAnswers: {},
          }));
        }
      },
      error: () => this.handleEmptyRetry(),
    });
  }

  private handleEmptyRetry() {
    this.quizState.update((s) => ({
      ...s,
      loading: false,
      activeQuiz: { questions: [] } as any,
    }));
  }

  selectOption(questionId: string, optionId: string) {
    if (this.quizState().submitted) return;
    this.quizState.update((s) => ({
      ...s,
      userAnswers: { ...s.userAnswers, [questionId]: optionId },
    }));
  }

  submitQuiz() {
    const state = this.quizState();
    if (!state.activeQuiz) return;

    this.quizState.update((s) => ({ ...s, loading: true }));
    const payload = { quizId: state.activeQuiz.id!, answers: state.userAnswers };

    this.quizService.submitQuiz(payload).subscribe({
      next: (result: QuizResultDTO) => {
        this.resultDetails = result;
        this.quizState.update((s) => ({
          ...s,
          loading: false,
          submitted: true,
          score: result.score,
          passed: result.passed,
        }));
      },
      error: () => {
        alert('Error al enviar el examen.');
        this.quizState.update((s) => ({ ...s, loading: false }));
      },
    });
  }

  loadPdfPreview(lessonId: string) {
    this.viewMode.set('PDF');
    this.pdfState.update((s) => ({ ...s, loading: true, error: null }));
    this.mediaService.getLessonPdfBlob(lessonId, false).subscribe({
      next: (blob) => {
        this.revokePdfObjectUrl();
        const objectUrl = URL.createObjectURL(blob);
        this.pdfState.update((s) => ({
          ...s,
          loading: false,
          objectUrl: objectUrl,
          safeUrl: this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl),
        }));
      },
      error: () =>
        this.pdfState.update((s) => ({ ...s, loading: false, error: 'Error cargando PDF' })),
    });
  }

  downloadPdf() {
    const lesson = this.currentLesson();
    if (!lesson) return;
    this.mediaService.getLessonPdfBlob(lesson.id, true).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${lesson.title}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  private revokePdfObjectUrl() {
    const currentUrl = this.pdfState().objectUrl;
    if (currentUrl) {
      URL.revokeObjectURL(currentUrl);
      this.pdfState.update((s) => ({ ...s, objectUrl: null, safeUrl: null }));
    }
  }

  toggleLessonCompletion(lessonId: string) {
    this.progressService.toggleProgress(lessonId).subscribe({
      next: () => this.loadProgress(),
    });
  }

  isLessonCompleted(lessonId: string): boolean {
    return this.completedLessonIds().has(lessonId);
  }

  toggleModule(moduleId: string) {
    const current = this.openModules();
    const newSet = new Set(current);
    newSet.has(moduleId) ? newSet.delete(moduleId) : newSet.add(moduleId);
    this.openModules.set(newSet);
  }

  isModuleOpen(moduleId: string): boolean {
    return this.openModules().has(moduleId);
  }

  private getYouTubeEmbedUrl(url: string): string {
    let videoId = '';
    if (url.includes('youtu.be')) videoId = url.split('/').pop()?.split('?')[0] || '';
    else if (url.includes('watch?v=')) videoId = url.split('v=')[1]?.split('&')[0] || '';
    else if (url.includes('embed/')) return url;
    return `https://www.youtube.com/embed/${videoId}`;
  }
}
