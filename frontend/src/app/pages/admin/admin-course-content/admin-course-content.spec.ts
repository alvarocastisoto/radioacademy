import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminCourseContent } from './admin-course-content';
import { CourseService } from '../../../services/course/course';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('AdminCourseContent', () => {
  let component: AdminCourseContent;
  let fixture: ComponentFixture<AdminCourseContent>;
  let courseServiceSpy: any;

  beforeEach(async () => {
    courseServiceSpy = {
      getCourseModules: vi.fn().mockReturnValue(of([])),
      getModuleLessons: vi.fn().mockReturnValue(of([])),
      deleteModule: vi.fn(),
      deleteLesson: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [AdminCourseContent],
      providers: [
        { provide: CourseService, useValue: courseServiceSpy },
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

    fixture = TestBed.createComponent(AdminCourseContent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
