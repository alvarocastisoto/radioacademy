import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';



export interface OptionDTO {
  id: string; 
  text: string;
  isCorrect: boolean;
}

export interface QuestionDTO {
  id: string;
  content: string; 
  points: number;
  options: OptionDTO[];
}

export interface QuizDTO {
  id?: string;
  title: string;
  moduleId: string;
  questions: QuestionDTO[];
}


export interface QuizResultDTO {
  score: number;
  passed: boolean;
  questionResults: { [key: string]: boolean }; 
  correctOptions: { [key: string]: string }; 
}

@Injectable({
  providedIn: 'root',
})
export class QuizService {
  private http = inject(HttpClient);

  
  private apiUrl = `${environment.apiUrl}/quizzes`;

  
  createQuiz(quiz: QuizDTO): Observable<QuizDTO> {
    
    return this.http.post<QuizDTO>(this.apiUrl, quiz);
  }

  
  getQuizByModule(moduleId: string): Observable<QuizDTO> {
    return this.http.get<QuizDTO>(`${this.apiUrl}/module/${moduleId}`);
  }

  
  getQuizById(quizId: string): Observable<QuizDTO> {
    return this.http.get<QuizDTO>(`${this.apiUrl}/${quizId}`);
  }

  
  submitQuiz(submission: {
    quizId: string;
    answers: Record<string, string>;
  }): Observable<QuizResultDTO> {
    return this.http.post<QuizResultDTO>(`${this.apiUrl}/submit`, submission);
  }

  
  getSmartFailedQuiz(quizId: string): Observable<QuizDTO> {
    return this.http.get<QuizDTO>(`${this.apiUrl}/${quizId}/smart-retry`);
  }
}
