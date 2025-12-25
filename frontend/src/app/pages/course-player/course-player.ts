import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { StudentService } from '../../services/student/student';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-course-player',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './course-player.html',
  styleUrls: ['./course-player.scss'],
})
export class CoursePlayerComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private studentService = inject(StudentService);
  private sanitizer = inject(DomSanitizer);
  private cdr = inject(ChangeDetectorRef);

  // 🔧 CONFIGURACIÓN: Ajusta esto si tu backend cambia de puerto o dominio
  private readonly UPLOADS_BASE_URL = 'http://localhost:8080/uploads/';

  course: any = null;
  currentLesson: any = null;
  safeVideoUrl: SafeResourceUrl | null = null;
  safePdfUrl: SafeResourceUrl | null = null;
  loading = true;

  openModules: Set<string> = new Set();

  ngOnInit() {
    const courseId = this.route.snapshot.paramMap.get('id');
    if (courseId) {
      this.loadContent(courseId);
    }
  }

  loadContent(id: string) {
    this.studentService.getCourseContent(id).subscribe({
      next: (data) => {
        console.log('📚 Temario recibido:', data);
        this.course = data;

        // Autoseleccionar primera lección si existe
        if (this.course.sections?.length > 0) {
          const firstModule = this.course.sections[0];
          this.toggleModule(firstModule.id);

          if (firstModule.lessons?.length > 0) {
            this.selectLesson(firstModule.lessons[0]);
          }
        }

        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando curso:', err);
        this.loading = false;
        alert('Error cargando el contenido. Revisa la consola.');
        this.router.navigate(['/student-dashboard']);
      },
    });
  }

  selectLesson(lesson: any) {
    this.currentLesson = lesson;

    // 1. GESTIÓN DEL VIDEO
    if (lesson.videoUrl) {
      this.safeVideoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(lesson.videoUrl);
    } else {
      this.safeVideoUrl = null;
    }

    // 2. GESTIÓN DEL PDF (SOLUCIÓN AL DUPLICADO)
    if (lesson.pdfUrl) {
      let fullPath = '';

      // CASO A: Es una URL absoluta (S3, Drive, etc.) -> Se usa tal cual
      if (lesson.pdfUrl.startsWith('http://') || lesson.pdfUrl.startsWith('https://')) {
        fullPath = lesson.pdfUrl;
      }
      // CASO B: Es un archivo local
      else {
        // TRUCO: Si la BD nos devuelve "uploads/archivo.pdf", le quitamos el "uploads/"
        // para quedarnos solo con el nombre "archivo.pdf".
        // La expresión regular reemplaza "uploads/" o "/uploads/" al principio.
        const cleanFileName = lesson.pdfUrl.replace(/^\/?uploads\//, '');

        // Ahora concatenamos: http://localhost:8080/uploads/ + archivo.pdf
        fullPath = this.UPLOADS_BASE_URL + cleanFileName;
      }

      console.log('🔗 Link PDF corregido:', fullPath);
      this.safePdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(fullPath);
    } else {
      this.safePdfUrl = null;
    }

    this.cdr.detectChanges();
  }

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
