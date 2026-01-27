import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EditCourseComponent } from './course-editor';
import { CourseService } from '../../../services/course/course';
import { MediaService } from '../../../services/media/media';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('EditCourseComponent', () => {
  let component: EditCourseComponent;
  let fixture: ComponentFixture<EditCourseComponent>;
  let courseServiceSpy: any;
  let mediaServiceSpy: any;

  beforeEach(async () => {
    courseServiceSpy = {
      getCourseById: vi.fn().mockReturnValue(of({ title: '', description: '', price: 0, hours: 0, coverImage: '' })),
      updateCourse: vi.fn()
    };
    mediaServiceSpy = { toPublicUrl: vi.fn().mockReturnValue(''), uploadFile: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [EditCourseComponent, ReactiveFormsModule],
      providers: [
        { provide: CourseService, useValue: courseServiceSpy },
        { provide: MediaService, useValue: mediaServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => '123' } }
          }
        },
        provideRouter([])
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(EditCourseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
