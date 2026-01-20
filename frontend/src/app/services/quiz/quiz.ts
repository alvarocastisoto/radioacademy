import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

// --- Interfaces (DTOs) que coinciden con el Backend Java ---
export interface OptionDTO {
  id?: string;
  text: string;
  isCorrect: boolean;
}

export interface QuestionDTO {
  id?: string;
  question: string; // Coincide con backend: question.setContent(dto.question())
  points: number;
  options: OptionDTO[];
}

export interface QuizDTO {
  id?: string;
  title: string;
  lessonId: string;
  questions: QuestionDTO[];
}

@Injectable({
  providedIn: 'root'
})
export class QuizService {
  private http = inject(HttpClient);
  // Ajusta la URL base según tu environment (ej: http://localhost:8080/api)
  private apiUrl = `${environment.apiUrl}/quizzes`; 

  // 1. Crear o Editar (Upsert)
  createQuiz(quiz: QuizDTO): Observable<any> {
    return this.http.post<any>(this.apiUrl, quiz);
  }

  // 2. Obtener por Lección (Este era el que faltaba)
  getQuizByLesson(lessonId: string): Observable<QuizDTO> {
    // Llama al endpoint Java: @GetMapping("/lesson/{lessonId}")
    return this.http.get<QuizDTO>(`${this.apiUrl}/lesson/${lessonId}`);
  }
}