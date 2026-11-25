import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AnomalyResponse,
  AskResponse,
  CancellationRiskResponse,
  ForecastResponse,
  PricingResponse
} from '../models/ai.model';

@Injectable({ providedIn: 'root' })
export class AiService {
  private http = inject(HttpClient);
  private baseUrl = environment.aiUrl;

  getForecast(days = 30): Observable<ForecastResponse> {
    return this.http.get<ForecastResponse>(`${this.baseUrl}/forecast`, { params: { days } }).pipe(catchError(this.handleError));
  }

  getAnomalies(limit = 10): Observable<AnomalyResponse> {
    return this.http.get<AnomalyResponse>(`${this.baseUrl}/anomalies`, { params: { limit } }).pipe(catchError(this.handleError));
  }

  getCancellationRisk(limit = 10): Observable<CancellationRiskResponse> {
    return this.http
      .get<CancellationRiskResponse>(`${this.baseUrl}/cancellation-risk`, { params: { limit } })
      .pipe(catchError(this.handleError));
  }

  getPricing(): Observable<PricingResponse> {
    return this.http.get<PricingResponse>(`${this.baseUrl}/pricing`).pipe(catchError(this.handleError));
  }

  ask(question: string): Observable<AskResponse> {
    return this.http.post<AskResponse>(`${this.baseUrl}/ask`, { question }).pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse) {
    const message =
      error.status === 0
        ? 'The AI service is not reachable. Start it with: cd ai-service && uvicorn app.main:app --port 8000'
        : error.error?.detail ?? `AI service error (${error.status})`;
    return throwError(() => new Error(message));
  }
}
