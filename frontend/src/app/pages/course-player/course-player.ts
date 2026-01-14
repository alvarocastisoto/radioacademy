import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { StudentService } from '../../services/student/student';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ProgressService } from '../../services/progress';
import { CommonModule } from '@angular/common';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-course-player',
  standalone: true,
  imports: [RouterModule, CommonModule],
  templateUrl: './course-player.html',
  styleUrls: ['./course-player.scss'],
})
export class CoursePlayerComponent implements OnInit {
  // INYECCIÓN DE DEPENDENCIAS
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private studentService = inject(StudentService);
  private progressService = inject(ProgressService);
  private sanitizer = inject(DomSanitizer);
  private cdr = inject(ChangeDetectorRef);

  // 🔧 CONFIGURACIÓN DINÁMICA DE UPLOADS
  // Esto coge "http://localhost:8080/api", le quita "/api" y le pega "/uploads/pdfs/"
  // Resultado: "http://localhost:8080/uploads/pdfs/"
  private readonly UPLOADS_BASE_URL = environment.apiUrl.replace(/\/api\/?$/, '') + '/uploads/pdfs/';

  // VARIABLES DE DATOS
  course: any = null;
  currentLesson: any = null;

  // VARIABLES DE VISUALIZACIÓN
  safeVideoUrl: SafeResourceUrl | null = null;
  safePdfUrl: SafeResourceUrl | null = null;
  loading = true;
  isYouTube: boolean = false;
  openModules: Set<string> = new Set();

  // VARIABLES DE ESTADO Y PROGRESO
  courseId: string = '';
  completedLessonIds: Set<string> = new Set();
  progressPercentage: number = 0;
  totalLessons: number = 0;

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      this.courseId = params.get('id') || '';

      console.log('🆔 ID RECIBIDO:', this.courseId);
      console.log('🌍 URL BASE DE ARCHIVOS:', this.UPLOADS_BASE_URL); // Para depurar

      if (this.courseId) {
        this.loadCourseData();
        this.loadProgress();
      } else {
        console.error('❌ ERROR CRÍTICO: No llega ningún ID en la URL');
      }
    });
  }

  // ==========================================
  // 1. LOGICA DE DATOS DEL CURSO
  // ==========================================

  loadCourseData() {
    this.loading = true;
    this.studentService.getCourseContent(this.courseId).subscribe({
      next: (data) => {
        console.log('📚 Temario RAW recibido:', data);
        this.course = data;

        const modulesList = this.course.modules || this.course.sections || [];

        this.totalLessons = 0;
        if (modulesList) {
          modulesList.forEach((module: any) => {
            if (module.lessons) {
              this.totalLessons += module.lessons.length;
            }
          });
        }

        // Autoselección de la primera lección
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

  // ==========================================
  // 2. LOGICA DE PROGRESO
  // ==========================================

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

    if (wasCompleted) {
      this.completedLessonIds.delete(lessonId);
    } else {
      this.completedLessonIds.add(lessonId);
    }
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
  // 3. LOGICA DE REPRODUCTOR (Video/PDF)
  // ==========================================

  selectLesson(lesson: any) {
    this.currentLesson = lesson;
    this.isYouTube = false;

    // A. GESTIÓN DEL VIDEO
    if (lesson.videoUrl) {
      if (this.isYouTubeUrl(lesson.videoUrl)) {
        this.isYouTube = true;
        const embedUrl = this.getYouTubeEmbedUrl(lesson.videoUrl);
        this.safeVideoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
      } else {
        this.isYouTube = false;
        this.safeVideoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(lesson.videoUrl);
      }
    } else {
      this.safeVideoUrl = null;
    }

    // B. GESTIÓN DEL PDF (CORREGIDO PARA NUEVA ESTRUCTURA)
    if (lesson.pdfUrl) {
      let fullPath = '';
      
      // Si ya viene con http (ej: S3 externo), lo usamos tal cual
      if (lesson.pdfUrl.startsWith('http')) {
        fullPath = lesson.pdfUrl;
      } else {
        // Si es local, usamos nuestra variable calculada.
        // El backend ahora devuelve solo el nombre (ej: "uuid_archivo.pdf")
        // UPLOADS_BASE_URL ya incluye el "/uploads/pdfs/"
        
        // Limpieza extra por si acaso el backend antiguo mandaba paths sucios
        const cleanFileName = lesson.pdfUrl
          .replace(/^\/?uploads\/pdfs\//, '') // Quita uploads/pdfs/ si viniera
          .replace(/^\/?uploads\//, '');      // Quita uploads/ si viniera

        fullPath = this.UPLOADS_BASE_URL + cleanFileName;
      }
      
      console.log('📄 PDF URL GENERADA:', fullPath); // Debug
      this.safePdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(fullPath);
    } else {
      this.safePdfUrl = null;
    }
  }

  // --- Helpers YouTube ---

  private isYouTubeUrl(url: string): boolean {
    return url.includes('youtube.com') || url.includes('youtu.be');
  }

  private getYouTubeEmbedUrl(url: string): string {
    let videoId = '';
    if (url.includes('youtu.be')) {
      videoId = url.split('/').pop()?.split('?')[0] || '';
    } else if (url.includes('watch?v=')) {
      videoId = url.split('v=')[1]?.split('&')[0] || '';
    } else if (url.includes('embed/')) {
      return url;
    }
    return `https://www.youtube.com/embed/${videoId}`;
  }

  // ==========================================
  // 4. LOGICA DE ACORDEÓN
  // ==========================================

  toggleModule(moduleId: string) {
    if (this.openModules.has(moduleId)) {
      this.openModules.delete(moduleId);
    } else {
      this.openModules.add(moduleId);
    }
  }

  isModuleOpen(moduleId: string): boolean {
    return this.openModules.has(moduleId);
  }
}