import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LessonForm } from './lesson-form';
import { CourseService } from '../../../services/course/course';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('LessonForm', () => {
  let component: LessonForm;
  let fixture: ComponentFixture<LessonForm>;
  let courseServiceSpy: any;

  beforeEach(async () => {
    courseServiceSpy = {
      getLessonById: vi.fn().mockReturnValue(of({ title: '', orderIndex: 1 })),
      createLesson: vi.fn(),
      updateLesson: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [LessonForm, ReactiveFormsModule],
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

    fixture = TestBed.createComponent(LessonForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
