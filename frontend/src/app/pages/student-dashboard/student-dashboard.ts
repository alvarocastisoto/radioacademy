import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StudentService } from '../../services/student/student'; // Revisa que la ruta sea correcta
import { Router, RouterModule } from '@angular/router';

// 👇 1. INTERFAZ QUE COINCIDE CON EL DTO DE JAVA
export interface DashboardCourse {
  id: string;
  title: string;
  description: string;
  coverImage: string | null;
  pdfUrl: string | null;
  progress: number; // Viene calculado del backend (0-100)
}

@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './student-dashboard.html',
  styleUrls: ['./student-dashboard.scss'],
})
export class StudentDashboardComponent implements OnInit {
  private studentService = inject(StudentService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  // 👇 2. USAMOS LA INTERFAZ
  courses: DashboardCourse[] = [];
  loading = true;

  // Variable para la URL base de imágenes (más limpio que ponerlo en el HTML)
  readonly UPLOADS_URL = 'http://localhost:8080/uploads/';

  ngOnInit() {
    this.loadDashboard();
  }

  loadDashboard() {
    this.loading = true;
    this.studentService.getMyCourses().subscribe({
      next: (data) => {
        console.log('✅ Cursos y progreso recibidos:', data);
        // Cast manual por si acaso, aunque el backend debe enviarlo bien
        this.courses = data as DashboardCourse[];
        this.loading = false;
        this.cdr.detectChanges(); // Forzamos actualización de vista
      },
      error: (e) => {
        console.error('🔥 Error cargando dashboard:', e);
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  enterCourse(courseId: string) {
    this.router.navigate(['/course-player', courseId]);
  }
}
