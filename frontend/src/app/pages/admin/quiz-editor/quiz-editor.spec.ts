import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QuizEditorComponent } from './quiz-editor';
import { QuizService } from '../../../services/quiz/quiz';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('QuizEditorComponent', () => {
  let component: QuizEditorComponent;
  let fixture: ComponentFixture<QuizEditorComponent>;
  let quizServiceSpy: any;

  beforeEach(async () => {
    quizServiceSpy = {
      getQuizByLesson: vi.fn().mockReturnValue(of({ title: '', questions: [] })),
      createQuiz: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [QuizEditorComponent, ReactiveFormsModule],
      providers: [
        { provide: QuizService, useValue: quizServiceSpy },
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

    fixture = TestBed.createComponent(QuizEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
