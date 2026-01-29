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

  // 🔄 CAMBIO 1: Ahora gestionamos moduleId, no lessonId
  moduleId = signal<string>('');
  quizForm: FormGroup;
  isSubmitting = false;

  constructor() {
    // Inicializamos el formulario base
    this.quizForm = this.fb.group({
      title: ['', [Validators.required]],
      questions: this.fb.array([]),
    });
  }

  ngOnInit() {
    // 🔄 CAMBIO 2: Buscamos 'moduleId' en la URL
    const id = this.route.snapshot.paramMap.get('moduleId');
    this.moduleId.set(id || '');

    if (this.moduleId()) {
      // 👇 Llamada al servicio con el nuevo método getQuizByModule
      this.quizService.getQuizByModule(this.moduleId()).subscribe({
        next: (quiz) => {
          if (quiz) {
            // MODO EDICIÓN: El backend devolvió un examen
            console.log('Examen encontrado para el módulo, cargando...', quiz);
            this.loadExistingQuiz(quiz);
          } else {
            // MODO CREACIÓN: No hay examen, empezamos de cero
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

  // --- Getters para acceder al FormArray en el HTML ---
  get questionsArray() {
    return this.quizForm.get('questions') as FormArray;
  }

  getOptionsArray(questionIndex: number) {
    return this.questionsArray.at(questionIndex).get('options') as FormArray;
  }

  // --- MÉTODOS DE GESTIÓN DEL FORMULARIO ---

  // 1. Método para reconstruir un formulario existente desde datos del Backend
  loadExistingQuiz(quiz: QuizDTO) {
    this.quizForm.patchValue({ title: quiz.title });
    this.questionsArray.clear();

    quiz.questions.forEach((q) => {
      const questionGroup = this.fb.group({
        // Si tienes ID de pregunta, podrías guardarlo aquí en un control oculto si lo necesitas para updates parciales
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

  // 2. Método para añadir una pregunta manual vacía
  addQuestion() {
    const questionGroup = this.fb.group({
      question: ['', Validators.required],
      points: [10, [Validators.required, Validators.min(1)]],
      options: this.fb.array([]),
    });

    this.questionsArray.push(questionGroup);

    // Añadimos 2 opciones vacías por defecto
    this.addOption(this.questionsArray.length - 1);
    this.addOption(this.questionsArray.length - 1);
  }

  removeQuestion(index: number) {
    this.questionsArray.removeAt(index);
  }

  // 3. Métodos para gestionar opciones
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

  // --- ENVIAR AL BACKEND ---
  onSubmit() {
    if (this.quizForm.invalid) {
      this.quizForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const formValue = this.quizForm.value;

    // 🔄 CAMBIO 3: Construcción correcta del DTO con moduleId
    const quizDTO: QuizDTO = {
      title: formValue.title,
      moduleId: this.moduleId(), // 👈 Aquí asignamos el ID del módulo
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
