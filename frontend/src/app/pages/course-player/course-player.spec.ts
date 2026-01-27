import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CoursePlayerComponent } from './course-player';
import { StudentService } from '../../services/student/student';
import { ProgressService } from '../../services/progress';
import { MediaService } from '../../services/media/media';
import { QuizService } from '../../services/quiz/quiz';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('CoursePlayerComponent', () => {
  let component: CoursePlayerComponent;
  let fixture: ComponentFixture<CoursePlayerComponent>;
  let studentServiceSpy: any;
  let progressServiceSpy: any;
  let mediaServiceSpy: any;
  let quizServiceSpy: any;

  beforeEach(async () => {
    studentServiceSpy = { getCourseContent: vi.fn().mockReturnValue(of({ modules: [] })) };
    progressServiceSpy = { getCourseProgress: vi.fn().mockReturnValue(of([])) };
    mediaServiceSpy = { getLessonPdfBlob: vi.fn() };
    quizServiceSpy = { getQuizById: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [CoursePlayerComponent, FormsModule],
      providers: [
        { provide: StudentService, useValue: studentServiceSpy },
        { provide: ProgressService, useValue: progressServiceSpy },
        { provide: MediaService, useValue: mediaServiceSpy },
        { provide: QuizService, useValue: quizServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of({ get: () => '123' }),
            snapshot: { paramMap: { get: () => '123' } }
          }
        },
        provideRouter([])
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(CoursePlayerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
