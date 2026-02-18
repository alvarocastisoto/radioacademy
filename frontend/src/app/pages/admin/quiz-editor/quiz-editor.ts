import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { QuizService, QuizDTO } from '../../../services/quiz/quiz';

@Component({
  selector: 'app-quiz-editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './quiz-editor.html',
  styleUrl: './quiz-editor.scss',
})
export class QuizEditorComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private quizService = inject(QuizService);

  
  moduleId = signal<string>('');
  quizForm: FormGroup;
  isSubmitting = false;

  constructor() {
    
    this.quizForm = this.fb.group({
      title: ['', [Validators.required]],
      questions: this.fb.array([]),
    });
  }

  ngOnInit() {
    
    const id = this.route.snapshot.paramMap.get('moduleId');
    this.moduleId.set(id || '');

    if (this.moduleId()) {
      
      this.quizService.getQuizByModule(this.moduleId()).subscribe({
        next: (quiz) => {
          if (quiz) {
            
            console.log('Examen encontrado para el módulo, cargando...', quiz);
            this.loadExistingQuiz(quiz);
          } else {
            
            console.log('No hay examen previo en este módulo, creando uno nuevo.');
            this.addQuestion();
          }
        },
        error: (err) => {
          console.error('Error al verificar examen existente (o 404)', err);
          this.addQuestion();
        },
      });
    }
  }

  
  get questionsArray() {
    return this.quizForm.get('questions') as FormArray;
  }

  getOptionsArray(questionIndex: number) {
    return this.questionsArray.at(questionIndex).get('options') as FormArray;
  }

  

  
  loadExistingQuiz(quiz: QuizDTO) {
    this.quizForm.patchValue({ title: quiz.title });
    this.questionsArray.clear();

    quiz.questions.forEach((q) => {
      const questionGroup = this.fb.group({
        
        question: [q.content, Validators.required],
        points: [q.points, [Validators.required, Validators.min(1)]],
        options: this.fb.array([]),
      });

      const optionsArray = questionGroup.get('options') as FormArray;
      q.options.forEach((o) => {
        optionsArray.push(
          this.fb.group({
            text: [o.text, Validators.required],
            isCorrect: [o.isCorrect],
          }),
        );
      });

      this.questionsArray.push(questionGroup);
    });
  }

  
  addQuestion() {
    const questionGroup = this.fb.group({
      question: ['', Validators.required],
      points: [10, [Validators.required, Validators.min(1)]],
      options: this.fb.array([]),
    });

    this.questionsArray.push(questionGroup);

    
    this.addOption(this.questionsArray.length - 1);
    this.addOption(this.questionsArray.length - 1);
  }

  removeQuestion(index: number) {
    this.questionsArray.removeAt(index);
  }

  
  addOption(questionIndex: number) {
    const options = this.getOptionsArray(questionIndex);
    const optionGroup = this.fb.group({
      text: ['', Validators.required],
      isCorrect: [false],
    });
    options.push(optionGroup);
  }

  removeOption(questionIndex: number, optionIndex: number) {
    const options = this.getOptionsArray(questionIndex);
    options.removeAt(optionIndex);
  }

  
  onSubmit() {
    if (this.quizForm.invalid) {
      this.quizForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const formValue = this.quizForm.value;

    
    const quizDTO: QuizDTO = {
      title: formValue.title,
      moduleId: this.moduleId(), 
      questions: formValue.questions,
    };

    console.log('Enviando DTO al backend:', quizDTO);

    this.quizService.createQuiz(quizDTO).subscribe({
      next: () => {
        this.isSubmitting = false;
        alert('¡Examen guardado correctamente!');
        window.history.back();
      },
      error: (err) => {
        console.error('Error guardando:', err);
        this.isSubmitting = false;
        alert('Error al guardar el examen. Revisa la consola.');
      },
    });
  }
}
