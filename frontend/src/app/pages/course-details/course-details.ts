import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router'; // Para leer la URL
import { CourseService } from '../../services/course/course';
import { CommonModule } from '@angular/common'; // Necesario para algunas directivas básicas

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

  // Guardamos la lista de módulos (que dentro tendrán sus lecciones)
  modules = signal<any[]>([]);
  courseId: string = '';

  ngOnInit() {
    // 1. Capturamos el ID de la URL (ej: abc-123)
    this.courseId = this.route.snapshot.paramMap.get('id') || '';

    if (this.courseId) {
      this.loadSyllabus();
    }
  }

  loadSyllabus() {
    // 2. Pedimos los módulos del curso
    this.courseService.getCourseModules(this.courseId).subscribe((modulesData) => {
      // 3. ¡Magia! Para cada módulo, pedimos sus lecciones
      modulesData.forEach((modulo) => {
        this.courseService.getModuleLessons(modulo.id).subscribe((lessonsData) => {
          // Le añadimos una propiedad 'lessons' al módulo dinámicamente
          modulo.lessons = lessonsData;
        });
      });

      // Guardamos los módulos en la señal
      this.modules.set(modulesData);
    });
  }
}
