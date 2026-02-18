import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CourseService } from '../../../services/course/course';
import { CommonModule } from '@angular/common';
import { SafeVideoPipe } from '../../../pipes/safe-video-pipe';
@Component({
  selector: 'app-admin-course-content',
  standalone: true,
  imports: [CommonModule, RouterLink, SafeVideoPipe], 
  templateUrl: './admin-course-content.html',
  styleUrl: './admin-course-content.scss',
})
export class AdminCourseContent implements OnInit {
  private route = inject(ActivatedRoute);
  private courseService = inject(CourseService);

  courseId: string = '';
  
  modules = signal<any[]>([]);

  ngOnInit() {
    
    this.courseId = this.route.snapshot.paramMap.get('id') || '';

    if (this.courseId) {
      this.loadSyllabus();
    }
  }

  
  loadSyllabus() {
    this.courseService.getCourseModules(this.courseId).subscribe((modulesData) => {
      
      this.modules.set(modulesData);

      
      modulesData.forEach((modulo) => {
        this.courseService.getModuleLessons(modulo.id).subscribe((lessonsData) => {
          
          modulo.lessons = lessonsData;

          
          
          
          this.modules.update((current) => [...current]);
        });
      });
    });
  }

  
  deleteModule(moduleId: string) {
    if (confirm('¿Estás seguro de que deseas borrar este módulo?')) {
      this.courseService.deleteModule(moduleId).subscribe(() => {
        
        this.loadSyllabus();
      });
    }
  }
  
  deleteLesson(lessonId: string) {
    if (confirm('¿Estás seguro de que deseas borrar esta lección?')) {
      this.courseService.deleteLesson(lessonId).subscribe(() => {
        
        this.loadSyllabus();
      });
    }
  }
}
