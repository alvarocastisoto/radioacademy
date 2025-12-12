import { Component, inject, OnInit, signal } from '@angular/core';
import { CourseService } from '../../services/course/course';
import { CurrencyPipe } from '@angular/common'; // Para formatear el precio (€)
import { RouterLink } from '@angular/router';
@Component({
  selector: 'app-course-list',
  standalone: true,
  imports: [CurrencyPipe, RouterLink], // Importamos el formateador de moneda
  templateUrl: './course-list.html',
  styleUrl: './course-list.scss',
})
export class CourseList implements OnInit {
  private courseService = inject(CourseService);

  // Usamos una señal para guardar la lista de cursos
  courses = signal<any[]>([]);

  ngOnInit() {
    this.loadCourses();
  }

  loadCourses() {
    this.courseService.getCourses().subscribe({
      next: (data) => {
        console.log('Cursos cargados:', data);
        this.courses.set(data);
      },
      error: (err) => {
        console.error('Error al cargar cursos:', err);
      },
    });
  }
}
