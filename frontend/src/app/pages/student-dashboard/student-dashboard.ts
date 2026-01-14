import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StudentService } from '../../services/student/student';
import { Router, RouterModule } from '@angular/router';
import { DashboardCourse } from '../../models/dashboard-course'; // 👈 Importa la interfaz

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

  // ✅ USAMOS SIGNALS (Más eficiente y reactivo)
  courses = signal<DashboardCourse[]>([]);
  loading = signal<boolean>(true);

  ngOnInit() {
    this.loadDashboard();
  }

  loadDashboard() {
    this.loading.set(true); // Activamos carga

    this.studentService.getMyCourses().subscribe({
      next: (data) => {
        console.log('✅ Dashboard cargado:', data);

        // TypeScript confiará en que data cumple la interfaz si el servicio está tipado,
        // si no, el cast es seguro aquí porque sabemos lo que envía el backend.
        this.courses.set(data as DashboardCourse[]);

        this.loading.set(false); // Desactivamos carga
      },
      error: (e) => {
        console.error('🔥 Error cargando dashboard:', e);
        this.loading.set(false);
        // Aquí podrías mostrar un toast o mensaje de error en la UI
      },
    });
  }

  enterCourse(courseId: string) {
    this.router.navigate(['/course-player', courseId]);
  }
}
