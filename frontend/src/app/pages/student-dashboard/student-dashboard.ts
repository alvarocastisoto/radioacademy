import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { StudentService } from '../../services/student/student';
import { MediaService } from '../../services/media/media'; 
import { DashboardCourse } from '../../models/dashboard-course';

@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './student-dashboard.html',
  styleUrls: ['./student-dashboard.scss'],
})
export class StudentDashboardComponent implements OnInit {
  private studentService = inject(StudentService);
  private mediaService = inject(MediaService); 
  private router = inject(Router);

  
  courses = signal<DashboardCourse[]>([]);
  loading = signal<boolean>(true);

  ngOnInit() {
    this.loadDashboard();
  }

  loadDashboard() {
    this.loading.set(true);

    this.studentService.getMyCourses().subscribe({
      next: (data) => {
        
        this.courses.set(data as DashboardCourse[]);
        this.loading.set(false);
      },
      error: (e) => {
        console.error('🔥 Error cargando dashboard:', e);
        this.loading.set(false);
      },
    });
  }

  
  getCourseImage(path: string | null | undefined): string {
    if (!path) return 'assets/img/placeholder-course.jpg'; 
    return this.mediaService.toPublicUrl(path);
  }

  enterCourse(courseId: string) {
    
    this.router.navigate(['/course-player', courseId]);

    
    
  }
}
