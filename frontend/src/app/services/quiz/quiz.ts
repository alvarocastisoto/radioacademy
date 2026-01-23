import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

// --- Interfaces (DTOs) ---
export interface OptionDTO {
  id?: string;
  text: string;
  isCorrect: boolean;
}

export interface QuestionDTO {
  id?: string;
  question: string;
  points: number;
  options: OptionDTO[];
}

export interface QuizDTO {
  id?: string;
  title: string;
  lessonId: string;
  questions: QuestionDTO[];
}

// Interface para el resultado de la corrección
export interface QuizResultDTO {
  score: number;
  passed: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class QuizService {
  private http = inject(HttpClient);

  // Base URL: http://localhost:8080/api/quizzes
  private apiUrl = `${environment.apiUrl}/quizzes`;

  // 1. Crear o Editar (ADMIN)
  createQuiz(quiz: QuizDTO): Observable<any> {
    return this.http.post<any>(this.apiUrl, quiz);
  }

  // 2. Obtener por Lección (ADMIN - Para editar)
  getQuizByLesson(lessonId: string): Observable<QuizDTO> {
    // GET /api/quizzes/lesson/{lessonId}
    return this.http.get<QuizDTO>(`${this.apiUrl}/lesson/${lessonId}`);
  }

  // 3. Obtener por ID (ESTUDIANTE - Para realizar el examen)
  getQuizById(quizId: string): Observable<QuizDTO> {
    // GET /api/quizzes/{quizId}
    return this.http.get<QuizDTO>(`${this.apiUrl}/${quizId}`);
  }

  // 4. Enviar respuestas (ESTUDIANTE - Para corregir) 🚀 NUEVO
  submitQuiz(submission: {
    quizId: string;
    answers: Record<string, string>;
  }): Observable<QuizResultDTO> {
    // POST /api/quizzes/submit
    return this.http.post<QuizResultDTO>(`${this.apiUrl}/submit`, submission);
  }
}
