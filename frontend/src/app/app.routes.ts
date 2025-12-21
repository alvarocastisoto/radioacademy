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

export const routes: Routes = [
  { path: '', component: Home }, // Ruta raíz (Home)
  { path: 'login', component: Login }, // /login
  { path: 'register', component: Register }, // /register
  { path: 'courses', component: CourseList }, // /courses
  { path: 'courses/:id', component: CourseDetails }, // Los dos puntos ':' indican que 'id' es una variable
  { path: 'admin', component: AdminDashboard, canActivate: [authGuard] }, // /admin
  { path: 'admin/courses/new', component: CourseForm, canActivate: [authGuard] }, // /admin/new para crear
  { path: 'admin/courses/:id/content', component: AdminCourseContent, canActivate: [authGuard] },
  { path: 'admin/modules/new', component: ModuleForm, canActivate: [authGuard] },
  { path: 'admin/lessons/new', component: LessonForm, canActivate: [authGuard] },
  {
    path: 'admin/lessons/:id/edit',
    component: LessonForm,
    canActivate: [authGuard],
  },
  {
    path: 'admin/modules/:moduleId/new-lesson',
    component: LessonForm,
    canActivate: [authGuard],
  },
  {
    path: 'admin/courses/:courseId/new-module',
    component: ModuleForm,
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: '' }, // Si escriben algo raro -> volver a Home
];
