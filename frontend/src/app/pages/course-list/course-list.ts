import { Component, inject, OnInit, signal } from '@angular/core';
import { CourseService } from '../../services/course/course';
import { PaymentService } from '../../services/payment/payment';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Course } from '../../models/course'; // 👈 Importa tu interfaz

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

  // ✅ TIPADO FUERTE: Ya no usamos <any[]>
  courses = signal<Course[]>([]);

  // ✅ CONSISTENCIA: isLoading ahora también es una señal
  isLoading = signal<boolean>(false);

  ngOnInit() {
    this.loadCourses();
  }

  loadCourses() {
    this.courseService.getCourses().subscribe({
      next: (data) => {
        // Al tener tipo, si data no coincide con Course[], TypeScript te avisará aquí
        this.courses.set(data);
      },
      error: (err) => {
        console.error('Error al cargar cursos:', err);
      },
    });
  }

  buyCourse(course: Course) {
    // 👈 Recibimos un Course, no un 'any'
    if (this.isLoading()) return; // Leemos la señal

    this.isLoading.set(true); // Escribimos la señal

    this.paymentService.buyCourse(course.id).subscribe({
      next: (response) => {
        // Redirección directa a Stripe
        window.location.href = response.url;
      },
      error: (err) => {
        console.error('Error al iniciar pago:', err);

        // Mejoramos el feedback: mostramos el mensaje del backend si existe (ej: "Ya tienes este curso")
        const msg = err.error?.error || 'Error al conectar con la pasarela de pago.';
        alert(msg);

        this.isLoading.set(false); // Reseteamos la señal
      },
    });
  }
}