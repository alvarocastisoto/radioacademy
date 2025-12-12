import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { CourseList } from './pages/course-list/course-list';
import { CourseDetails } from './pages/course-details/course-details'; // <--- IMPORTANTE

export const routes: Routes = [
  { path: '', component: Home }, // Ruta raíz (Home)
  { path: 'login', component: Login }, // /login
  { path: 'register', component: Register }, // /register
  { path: 'courses', component: CourseList }, // /courses
  { path: 'courses/:id', component: CourseDetails }, // Los dos puntos ':' indican que 'id' es una variable
  { path: '**', redirectTo: '' }, // Si escriben algo raro -> volver a Home
];
