import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CourseService } from '../../services/course/course';
import { PaymentService } from '../../services/payment/payment';
import { MediaService } from '../../services/media/media'; // 👈 Importamos
import { Course } from '../../models/course';

@Component({
  selector: 'app-course-list',
  standalone: true,
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './course-list.html',
  styleUrl: './course-list.scss',
})
export class CourseList implements OnInit {
  private courseService = inject(CourseService);
  private paymentService = inject(PaymentService);
  private mediaService = inject(MediaService); // 👈 Inyectamos

  courses = signal<Course[]>([]);
  isLoading = signal<boolean>(false);
  loadingData = signal<boolean>(true); // Nuevo signal para el esqueleto de carga inicial

  ngOnInit() {
    this.loadCourses();
  }

  loadCourses() {
    this.loadingData.set(true);
    this.courseService.getCourses().subscribe({
      next: (data) => {
        this.courses.set(data);
        this.loadingData.set(false);
      },
      error: (err) => {
        console.error('Error al cargar cursos:', err);
        this.loadingData.set(false);
      },
    });
  }

  // ✅ Helper para imágenes
  getCourseImage(path: string | undefined): string {
    if (!path) return 'assets/img/placeholder-course.jpg';
    return this.mediaService.toPublicUrl(path);
  }

  buyCourse(course: Course) {
    if (this.isLoading()) return;

    this.isLoading.set(true);

    this.paymentService.buyCourse(course.id).subscribe({
      next: (response) => {
        window.location.href = response.url;
      },
      error: (err) => {
        console.error('Error al iniciar pago:', err);
        const msg = err.error?.error || 'Error al conectar con la pasarela de pago.';
        alert(msg);
        this.isLoading.set(false);
      },
    });
  }
}
