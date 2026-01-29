import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

// --- Interfaces (DTOs) ---

export interface OptionDTO {
  id: string; // 👈 CAMBIO 1: ID obligatorio (sin ?) para usarlo de índice
  text: string;
  isCorrect: boolean;
}

export interface QuestionDTO {
  id: string;
  content: string; // 👈 CAMBIO 2: Renombrado a 'content' (antes 'question')
  points: number;
  options: OptionDTO[];
}

export interface QuizDTO {
  id?: string;
  title: string;
  moduleId: string;
  questions: QuestionDTO[];
}

// Interface para el resultado de la corrección
export interface QuizResultDTO {
  score: number;
  passed: boolean;
  questionResults: { [key: string]: boolean }; // ID pregunta -> true/false
  correctOptions: { [key: string]: string }; // ID pregunta -> ID opción correcta
}

@Injectable({
  providedIn: 'root',
})
export class QuizService {
  private http = inject(HttpClient);

  // Base URL: http://localhost:8080/api/quizzes
  private apiUrl = `${environment.apiUrl}/quizzes`;

  // 1. Crear o Editar (ADMIN)
  createQuiz(quiz: QuizDTO): Observable<QuizDTO> {
    // Devolvemos QuizDTO
    return this.http.post<QuizDTO>(this.apiUrl, quiz);
  }

  // 2. Obtener por Módulo (ADMIN - Para editar)
  getQuizByModule(moduleId: string): Observable<QuizDTO> {
    return this.http.get<QuizDTO>(`${this.apiUrl}/module/${moduleId}`);
  }

  // 3. Obtener por ID (ESTUDIANTE - Pool aleatorio de 50)
  getQuizById(quizId: string): Observable<QuizDTO> {
    return this.http.get<QuizDTO>(`${this.apiUrl}/${quizId}`);
  }

  // 4. Enviar respuestas (ESTUDIANTE - Para corregir y recibir feedback)
  submitQuiz(submission: {
    quizId: string;
    answers: Record<string, string>;
  }): Observable<QuizResultDTO> {
    return this.http.post<QuizResultDTO>(`${this.apiUrl}/submit`, submission);
  }

  // 5. SMART RETRY (Banco de Fallos Persistente)
  getSmartFailedQuiz(quizId: string): Observable<QuizDTO> {
    return this.http.get<QuizDTO>(`${this.apiUrl}/${quizId}/smart-retry`);
  }
}
