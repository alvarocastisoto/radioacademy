import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CourseService } from '../../../services/course/course';

@Component({
  selector: 'app-module-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './module-form.html',
  styleUrl: './module-form.scss',
})
export class ModuleForm implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute); 
  private router = inject(Router); 
  private courseService = inject(CourseService);

  courseId: string = '';

  form: FormGroup = this.fb.group({
    title: ['', [Validators.required]],
    orderIndex: [1, [Validators.required]],
  });

  ngOnInit() {
    
    this.courseId = this.route.snapshot.paramMap.get('courseId') || '';
  }

  onSubmit() {
    if (this.form.valid && this.courseId) {
      const moduleData = {
        ...this.form.value,
        courseId: this.courseId, 
      };

      this.courseService.createModule(moduleData).subscribe(() => {
        
        this.router.navigate(['/admin/courses', this.courseId, 'content']);
      });
    }
  }
}
