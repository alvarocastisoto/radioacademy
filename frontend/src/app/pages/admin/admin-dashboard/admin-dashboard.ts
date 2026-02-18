import { Component, inject, OnInit, signal } from '@angular/core';
import { CourseService } from '../../../services/course/course';
import { CurrencyPipe, DatePipe, SlicePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-dashboard',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboard implements OnInit {
  private courseService = inject(CourseService);

  
  courses = signal<any[]>([]);

  ngOnInit() {
    this.loadCourses();
  }

  loadCourses() {
    this.courseService.getCourses().subscribe({
      next: (data) => this.courses.set(data),
      error: (err) => console.error('Error cargando cursos', err),
    });
  }

  deleteCourse(id: string) {
    if (confirm('¿Estás seguro de que quieres eliminar este curso? No hay vuelta atrás.')) {
      this.courseService.deleteCourse(id).subscribe({
        next: () => {
          
          this.loadCourses();
          alert('Curso eliminado correctamente');
        },
        error: (err) => alert('Error al eliminar: ' + err.message),
      });
    }
  }
}
