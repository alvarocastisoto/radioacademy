import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QuizEditor } from './quiz-editor';

describe('QuizEditor', () => {
  let component: QuizEditor;
  let fixture: ComponentFixture<QuizEditor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuizEditor]
    })
    .compileComponents();

    fixture = TestBed.createComponent(QuizEditor);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
