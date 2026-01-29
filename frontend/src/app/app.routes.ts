import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { CourseList } from './pages/course-list/course-list';
import { CourseDetails } from './pages/course-details/course-details';
import { AdminDashboard } from './pages/admin/admin-dashboard/admin-dashboard';
import { CourseForm } from './pages/admin/course-form/course-form';
import { AdminCourseContent } from './pages/admin/admin-course-content/admin-course-content';
import { ModuleForm } from './pages/admin/module-form/module-form';
import { LessonForm } from './pages/admin/lesson-form/lesson-form';
import { AdminUsersComponent } from './pages/admin-users/admin-users';
import { StudentDashboardComponent } from './pages/student-dashboard/student-dashboard';
import { CoursePlayerComponent } from './pages/course-player/course-player';
import { ProfileComponent } from './pages/profile/profile';
import { ForgotPasswordComponent } from './pages/forgot-password/forgot-password';
import { ResetPasswordComponent } from './pages/reset-password/reset-password';
import { TestPaymentComponent } from './pages/payment/test-payment/test-payment';
import { PaymentSuccessComponent } from './pages/payment/payment-success/payment-success';
import { PaymentCancel } from './pages/payment/payment-cancel/payment-cancel';
import { QuizEditorComponent } from './pages/admin/quiz-editor/quiz-editor';
import { EditCourseComponent } from './pages/admin/course-editor/course-editor';
// GUARDS
import { authGuard } from './guards/auth-guard';
import { adminGuard } from './guards/admin-guard';

export const routes: Routes = [
  // --- 🌍 PÚBLICAS ---
  { path: '', component: Home },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'courses', component: CourseList },
  { path: 'courses/:id', component: CourseDetails },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },

  // --- 🎓 ALUMNO (Protegidas con authGuard) ---
  // ⚠️ CAMBIO CLAVE: De 'student-dashboard' a 'dashboard' para coincidir con el Register
  { path: 'dashboard', component: StudentDashboardComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'course-player/:id', component: CoursePlayerComponent, canActivate: [authGuard] },

  // --- 💳 PAGOS ---
  // Lo ideal es proteger success/cancel también, para que no entren usuarios anonimos
  { path: 'payment/test', component: TestPaymentComponent, canActivate: [authGuard] },
  { path: 'payment/success', component: PaymentSuccessComponent, canActivate: [authGuard] },
  { path: 'payment/cancel', component: PaymentCancel, canActivate: [authGuard] },

  // --- 🛡️ ADMIN (Protegidas con adminGuard) ---
  { path: 'admin', component: AdminDashboard, canActivate: [adminGuard] },
  { path: 'admin/users', component: AdminUsersComponent, canActivate: [adminGuard] },

  // Gestión de Cursos
  { path: 'admin/courses/new', component: CourseForm, canActivate: [adminGuard] },
  { path: 'admin/courses/:id/content', component: AdminCourseContent, canActivate: [adminGuard] },
  { path: 'admin/courses/:courseId/new-module', component: ModuleForm, canActivate: [adminGuard] },
  { path: 'admin/courses/edit/:id', component: EditCourseComponent, canActivate: [adminGuard] },
  // Gestión de Módulos y Lecciones
  { path: 'admin/modules/new', component: ModuleForm, canActivate: [adminGuard] },
  { path: 'admin/modules/:moduleId/new-lesson', component: LessonForm, canActivate: [adminGuard] },
  { path: 'admin/lessons/new', component: LessonForm, canActivate: [adminGuard] },
  { path: 'admin/lessons/:id/edit', component: LessonForm, canActivate: [adminGuard] },

  // Exámenes
  {
    path: 'admin/module/:moduleId/quiz',
    component: QuizEditorComponent,
    canActivate: [adminGuard],
  },

  // --- ⚠️ COMODÍN (Siempre al final) ---
  { path: '**', redirectTo: '' },
];
