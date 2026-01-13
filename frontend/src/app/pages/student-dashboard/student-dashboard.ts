import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StudentService } from '../../services/student/student';
import { Router, RouterModule } from '@angular/router';

// 👇 1. INTERFAZ (Coincide con el Backend)
export interface DashboardCourse {
  id: string;
  title: string;
  description: string;
  coverImage: string | null; // Ahora vendrá como "http://localhost:8080/..."
  pdfUrl: string | null;
  progress: number;
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

  courses: DashboardCourse[] = [];
  loading = true;

  // ❌ HE BORRADO 'UPLOADS_URL'.
  // Ya no hace falta porque el backend nos da la ruta absoluta.
  // Si la dejas, corres el riesgo de concatenar doble.

  ngOnInit() {
    this.loadDashboard();
  }

  loadDashboard() {
    this.loading = true;
    this.studentService.getMyCourses().subscribe({
      next: (data) => {
        console.log('✅ Dashboard cargado:', data);
        
        // Asignamos directamente. 
        // Las URLs de coverImage ya vienen listas para usar en el src=""
        this.courses = data as DashboardCourse[];
        
        this.loading = false;
        this.cdr.detectChanges(); 
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