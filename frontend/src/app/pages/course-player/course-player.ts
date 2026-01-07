import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { StudentService } from '../../services/student/student';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ProgressService } from '../../services/progress'; // Asegúrate de que la ruta sea correcta
import { CommonModule } from '@angular/common'; // Recomendado aunque uses @if para pipes como json o async

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

  // 🔧 CONFIGURACIÓN
  private readonly UPLOADS_BASE_URL = 'http://localhost:8080/uploads/';

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
      // ✅ CORREGIDO: Buscamos 'id' porque así se llama en tu app.routes.ts
      this.courseId = params.get('id') || '';

      console.log('🆔 ID RECIBIDO:', this.courseId);

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

  // EN course-player.component.ts

  loadCourseData() {
    this.loading = true;
    this.studentService.getCourseContent(this.courseId).subscribe({
      next: (data) => {
        console.log('📚 Temario RAW recibido:', data); // <--- MIRA ESTO EN CONSOLA
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

        // Autoselección
        if (modulesList.length > 0) {
          const firstModule = modulesList[0];
          this.toggleModule(firstModule.id);

          if (firstModule.lessons?.length > 0) {
            this.selectLesson(firstModule.lessons[0]);
          }
        }

        this.calculateProgress();
        this.loading = false; // <--- AQUÍ SE QUITA EL SPINNER
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('🔥 Error cargando curso:', err); // <--- ¿SALE ESTO?
        this.loading = false;
      },
    });
  }

  // ==========================================
  // 2. LOGICA DE PROGRESO (Checks y Barra)
  // ==========================================

  loadProgress() {
    this.progressService.getCourseProgress(this.courseId).subscribe({
      next: (ids) => {
        // Convertimos el array en un Set para búsquedas rápidas (O(1))
        this.completedLessonIds = new Set(ids);
        this.calculateProgress();
      },
      error: (err) => console.error('Error cargando progreso', err),
    });
  }

  toggleLessonCompletion(lessonId: string) {
    // A. OPTIMISTIC UPDATE: Actualizamos UI inmediatamente
    const wasCompleted = this.completedLessonIds.has(lessonId);

    if (wasCompleted) {
      this.completedLessonIds.delete(lessonId);
    } else {
      this.completedLessonIds.add(lessonId);
    }
    this.calculateProgress();

    // B. Llamada al Backend
    this.progressService.toggleProgress(lessonId).subscribe({
      next: (isCompletedBackend) => {
        // Sincronización final con lo que diga el servidor
        if (isCompletedBackend) this.completedLessonIds.add(lessonId);
        else this.completedLessonIds.delete(lessonId);
        this.calculateProgress();
      },
      error: (err) => {
        // C. ROLLBACK: Si falla, deshacemos el cambio visual
        console.error('Error guardando progreso', err);
        if (wasCompleted) this.completedLessonIds.add(lessonId);
        else this.completedLessonIds.delete(lessonId);
        this.calculateProgress();
        // Opcional: Mostrar Toast o alerta suave
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

    // Evitar que pase del 100% por si acaso
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

    // B. GESTIÓN DEL PDF
    if (lesson.pdfUrl) {
      let fullPath = '';
      if (lesson.pdfUrl.startsWith('http')) {
        fullPath = lesson.pdfUrl;
      } else {
        // Limpiamos duplicados 'uploads/uploads/' por si acaso
        const cleanFileName = lesson.pdfUrl.replace(/^\/?uploads\//, '');
        fullPath = this.UPLOADS_BASE_URL + cleanFileName;
      }
      this.safePdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(fullPath);
    } else {
      this.safePdfUrl = null;
    }

    // Marcar como detectado cambio manual (útil si hay OnPush)
    // this.cdr.detectChanges();
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
  // 4. LOGICA DE ACORDEÓN (Sidebar)
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
