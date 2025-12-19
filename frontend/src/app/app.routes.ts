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

export const routes: Routes = [
  { path: '', component: Home }, // Ruta raíz (Home)
  { path: 'login', component: Login }, // /login
  { path: 'register', component: Register }, // /register
  { path: 'courses', component: CourseList }, // /courses
  { path: 'courses/:id', component: CourseDetails }, // Los dos puntos ':' indican que 'id' es una variable
  { path: 'admin', component: AdminDashboard }, // /admin
  { path: 'admin/courses/new', component: CourseForm }, // /admin/new para crear
  { path: 'admin/courses/:id/content', component: AdminCourseContent },
  { path: 'admin/modules/new', component: ModuleForm },
  { path: 'admin/lessons/new', component: LessonForm },

  { path: '**', redirectTo: '' }, // Si escriben algo raro -> volver a Home
];
