import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CourseService } from '../../../services/course/course';
import { CommonModule } from '@angular/common';
import { SafeVideoPipe } from '../../../pipes/safe-video-pipe';
@Component({
  selector: 'app-admin-course-content',
  standalone: true,
  imports: [CommonModule, RouterLink, SafeVideoPipe], // Importamos RouterLink para el botón de "Volver"
  templateUrl: './admin-course-content.html',
  styleUrl: './admin-course-content.scss',
})
export class AdminCourseContent implements OnInit {
  private route = inject(ActivatedRoute);
  private courseService = inject(CourseService);

  courseId: string = '';
  // Usamos una señal para guardar la lista de módulos y que la vista se actualice sola
  modules = signal<any[]>([]);

  ngOnInit() {
    // 1. Capturamos el ID del curso de la URL (ej: /admin/courses/123/content)
    this.courseId = this.route.snapshot.paramMap.get('id') || '';

    if (this.courseId) {
      this.loadSyllabus();
    }
  }

  // CARGAR TEMARIO COMPLETO
  loadSyllabus() {
    this.courseService.getCourseModules(this.courseId).subscribe((modulesData) => {
      // 1. Guardamos los módulos iniciales (vacíos de lecciones por ahora)
      this.modules.set(modulesData);

      // 2. Pedimos las lecciones de cada uno
      modulesData.forEach((modulo) => {
        this.courseService.getModuleLessons(modulo.id).subscribe((lessonsData) => {
          // Asignamos las lecciones
          modulo.lessons = lessonsData;

          // 🔥 EL TRUCO: Forzamos la actualización de la señal
          // Al hacer [...current], creamos una copia nueva del array.
          // Esto le grita a Angular: "¡EH! ¡HAN CAMBIADO LOS DATOS! ¡REPINTA!"
          this.modules.update((current) => [...current]);
        });
      });
    });
  }

  //Borrar módulo
  deleteModule(moduleId: string) {
    if (confirm('¿Estás seguro de que deseas borrar este módulo?')) {
      this.courseService.deleteModule(moduleId).subscribe(() => {
        // Recargar el temario después de borrar el módulo
        this.loadSyllabus();
      });
    }
  }
  //Borrar lección
  deleteLesson(lessonId: string) {
    if (confirm('¿Estás seguro de que deseas borrar esta lección?')) {
      this.courseService.deleteLesson(lessonId).subscribe(() => {
        // Recargar el temario después de borrar la lección
        this.loadSyllabus();
      });
    }
  }
}
