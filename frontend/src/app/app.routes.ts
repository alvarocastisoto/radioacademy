import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { CourseList } from './pages/course-list/course-list';
import { CourseDetails } from './pages/course-details/course-details'; // <--- IMPORTANTE
import { AdminDashboard } from './pages/admin/admin-dashboard/admin-dashboard';
import { CourseForm } from './pages/admin/course-form/course-form';
import { AdminCourseContent } from './pages/admin/admin-course-content/admin-course-content';
import { ModuleForm } from './pages/admin/module-form/module-form';
import { LessonForm } from './pages/admin/lesson-form/lesson-form';
import { authGuard } from './guards/auth-guard'; // Asegúrate de tener el guard
import { AdminUsersComponent } from './pages/admin-users/admin-users';
import { StudentDashboardComponent } from './pages/student-dashboard/student-dashboard';
import { CoursePlayerComponent } from './pages/course-player/course-player';
import { ProfileComponent } from './pages/profile/profile'; // 👈 Importa esto
import { adminGuard } from './guards/admin-guard';
import { ForgotPasswordComponent } from './pages/forgot-password/forgot-password';
import { ResetPasswordComponent } from './pages/reset-password/reset-password';
import { TestPaymentComponent } from './pages/payment/test-payment/test-payment';
import { PaymentSuccessComponent } from './pages/payment/payment-success/payment-success';
import { PaymentCancel } from './pages/payment/payment-cancel/payment-cancel';

export const routes: Routes = [
  { path: '', component: Home }, // Ruta raíz (Home)
  { path: 'login', component: Login }, // /login
  { path: 'register', component: Register }, // /register

  // Course routes
  { path: 'courses', component: CourseList }, // /courses
  { path: 'courses/:id', component: CourseDetails }, // Los dos puntos ':' indican que 'id' es una variable
  { path: 'course-player/:id', component: CoursePlayerComponent, canActivate: [authGuard] },

  // Student Routes
  { path: 'student-dashboard', component: StudentDashboardComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },

  // Payment Routes
  { path: 'payment/test', component: TestPaymentComponent }, // Para ver el botón de prueba
  { path: 'payment/success', component: PaymentSuccessComponent }, // Retorno OK
  { path: 'payment/cancel', component: PaymentCancel }, // Retorno Cancelado
  // Admin Routes
  { path: 'admin', component: AdminDashboard, canActivate: [adminGuard] }, // /admin
  { path: 'admin/users', component: AdminUsersComponent, canActivate: [adminGuard] },
  { path: 'admin/courses/new', component: CourseForm, canActivate: [adminGuard] }, // /admin/new para crear
  { path: 'admin/courses/:id/content', component: AdminCourseContent, canActivate: [adminGuard] },
  { path: 'admin/modules/new', component: ModuleForm, canActivate: [adminGuard] },
  { path: 'admin/lessons/new', component: LessonForm, canActivate: [adminGuard] },
  { path: 'admin/lessons/:id/edit', component: LessonForm, canActivate: [adminGuard] },
  { path: 'admin/modules/:moduleId/new-lesson', component: LessonForm, canActivate: [adminGuard] },
  { path: 'admin/courses/:courseId/new-module', component: ModuleForm, canActivate: [adminGuard] },

  { path: '**', redirectTo: '' }, // Si escriben algo raro -> volver a Home
];
