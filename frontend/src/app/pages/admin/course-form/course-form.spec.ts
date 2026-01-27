import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CourseForm } from './course-form';
import { CourseService } from '../../../services/course/course';
import { MediaService } from '../../../services/media/media';
import { provideRouter } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('CourseForm', () => {
  let component: CourseForm;
  let fixture: ComponentFixture<CourseForm>;
  let courseServiceSpy: any;
  let mediaServiceSpy: any;

  beforeEach(async () => {
    courseServiceSpy = { createCourse: vi.fn() };
    mediaServiceSpy = { uploadFile: vi.fn(), toPublicUrl: vi.fn().mockReturnValue('') };

    await TestBed.configureTestingModule({
      imports: [CourseForm, ReactiveFormsModule],
      providers: [
        { provide: CourseService, useValue: courseServiceSpy },
        { provide: MediaService, useValue: mediaServiceSpy },
        provideRouter([])
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(CourseForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
