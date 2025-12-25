import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core'; // 1. IMPORTAR ChangeDetectorRef
import { CommonModule } from '@angular/common';
import { StudentService } from '../../services/student/student'; // Ajusta ruta si es necesario
import { Router, RouterModule } from '@angular/router';

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
  private cdr = inject(ChangeDetectorRef); // 2. INYECTAR EL DETECTOR

  courses: any[] = [];
  loading = true;

  ngOnInit() {
    this.studentService.getMyCourses().subscribe({
      next: (data) => {
        console.log('✅ Cursos recibidos:', data);
        this.courses = data;
        this.loading = false; // Apagamos loading

        // 3. 🔥 ¡OBLIGAR A ANGULAR A PINTAR YA!
        this.cdr.detectChanges();
      },
      error: (e) => {
        console.error('Error:', e);
        this.loading = false;
        this.cdr.detectChanges(); // Aquí también por si acaso
      },
    });
  }

  enterCourse(courseId: string) {
    this.router.navigate(['/course-player', courseId]);
  }
}
