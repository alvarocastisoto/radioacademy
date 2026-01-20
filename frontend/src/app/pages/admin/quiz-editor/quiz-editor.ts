import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { QuizService, QuizDTO } from '../../../services/quiz/quiz'; // Ajusta la ruta de importación si es necesario

@Component({
  selector: 'app-quiz-editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './quiz-editor.html',
  styleUrl: './quiz-editor.scss'
})
export class QuizEditorComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private quizService = inject(QuizService);

  lessonId = signal<string>('');
  quizForm: FormGroup;
  isSubmitting = false;

  constructor() {
    // Inicializamos el formulario base
    this.quizForm = this.fb.group({
      title: ['', [Validators.required]],
      questions: this.fb.array([]) 
    });
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('lessonId');
    this.lessonId.set(id || '');

    if (this.lessonId()) {
      // 👇 LÓGICA CLAVE: Preguntamos al backend si ya hay test para esta lección
      this.quizService.getQuizByLesson(this.lessonId()).subscribe({
        next: (quiz) => {
          if (quiz) {
            // MODO EDICIÓN: El backend devolvió un examen, lo cargamos
            console.log('Examen encontrado, cargando datos...', quiz);
            this.loadExistingQuiz(quiz);
          } else {
            // MODO CREACIÓN: El backend devolvió null/204, formulario limpio
            console.log('No hay examen previo, creando uno nuevo.');
            this.addQuestion();
          }
        },
        error: (err) => {
          console.error('Error al verificar examen existente (o 404)', err);
          // Fallback: Si falla la conexión, asumimos que es nuevo para no bloquear al usuario
          this.addQuestion(); 
        }
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
    // A. Ponemos el título
    this.quizForm.patchValue({ title: quiz.title });

    // B. Limpiamos el array de preguntas por seguridad
    this.questionsArray.clear();

    // C. Reconstruimos la estructura anidada (Preguntas -> Opciones)
    quiz.questions.forEach(q => {
      // Creamos el grupo de la pregunta
      const questionGroup = this.fb.group({
        question: [q.question, Validators.required], 
        points: [q.points, [Validators.required, Validators.min(1)]],
        options: this.fb.array([])
      });

      // Rellenamos las opciones de ESA pregunta
      const optionsArray = questionGroup.get('options') as FormArray;
      q.options.forEach(o => {
        optionsArray.push(this.fb.group({
          text: [o.text, Validators.required],
          isCorrect: [o.isCorrect]
        }));
      });

      // Añadimos la pregunta completa al formulario principal
      this.questionsArray.push(questionGroup);
    });
  }

  // 2. Método para añadir una pregunta manual vacía (botón +)
  addQuestion() {
    const questionGroup = this.fb.group({
      question: ['', Validators.required],
      points: [10, [Validators.required, Validators.min(1)]],
      options: this.fb.array([])
    });

    this.questionsArray.push(questionGroup);
    
    // Añadimos 2 opciones vacías por defecto para agilizar la escritura
    this.addOption(this.questionsArray.length - 1);
    this.addOption(this.questionsArray.length - 1);
  }

  removeQuestion(index: number) {
    this.questionsArray.removeAt(index);
  }

  // 3. Métodos para gestionar opciones dentro de una pregunta
  addOption(questionIndex: number) {
    const options = this.getOptionsArray(questionIndex);
    const optionGroup = this.fb.group({
      text: ['', Validators.required],
      isCorrect: [false]
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
      this.quizForm.markAllAsTouched(); // Marca todo en rojo para que el usuario vea qué falta
      return;
    }

    this.isSubmitting = true;
    const formValue = this.quizForm.value;

    // Convertimos el formulario al DTO exacto
    const quizDTO: QuizDTO = {
      title: formValue.title,
      lessonId: this.lessonId(),
      questions: formValue.questions
    };

    console.log('Enviando DTO al backend:', quizDTO);

    this.quizService.createQuiz(quizDTO).subscribe({
      next: () => {
        this.isSubmitting = false;
        alert('¡Examen guardado correctamente!');
        // Navegamos hacia atrás (al temario del curso)
        window.history.back(); 
      },
      error: (err) => {
        console.error('Error guardando:', err);
        this.isSubmitting = false;
        alert('Error al guardar el examen. Revisa la consola.');
      }
    });
  }
}