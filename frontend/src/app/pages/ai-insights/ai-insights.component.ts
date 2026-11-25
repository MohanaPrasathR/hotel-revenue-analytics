import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, forkJoin } from 'rxjs';
import { Chart, registerables } from 'chart.js';
import { AiService } from '../../core/services/ai.service';
import {
  AiMeta,
  AnomalyResponse,
  CancellationRiskResponse,
  ForecastResponse,
  PricingResponse
} from '../../core/models/ai.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { ErrorBannerComponent } from '../../shared/components/error-banner/error-banner.component';

Chart.register(...registerables);

interface ChatTurn {
  role: 'user' | 'ai';
  text: string;
  engine?: string;
}

@Component({
  selector: 'app-ai-insights',
  standalone: true,
  imports: [CommonModule, FormsModule, LoadingSpinnerComponent, ErrorBannerComponent],
  templateUrl: './ai-insights.component.html',
  styleUrls: ['./ai-insights.component.css']
})
export class AiInsightsComponent implements OnInit, OnDestroy {
  private ai = inject(AiService);
  private subs = new Subscription();
  private chart: Chart | null = null;

  @ViewChild('forecastCanvas') set forecastCanvas(ref: ElementRef<HTMLCanvasElement> | undefined) {
    if (ref && this.forecast) this.renderForecast(ref.nativeElement);
  }

  isLoading = true;
  errorMessage = '';
  meta: AiMeta | null = null;
  forecast: ForecastResponse | null = null;
  anomalies: AnomalyResponse | null = null;
  risk: CancellationRiskResponse | null = null;
  pricing: PricingResponse | null = null;

  question = '';
  asking = false;
  chat: ChatTurn[] = [];
  readonly suggestions = [
    'What is the revenue forecast for next month?',
    'Which bookings are likely to cancel?',
    'Are there any unusual bookings?',
    'Which hotel is performing best?'
  ];

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    this.chart?.destroy();
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.subs.add(
      forkJoin({
        forecast: this.ai.getForecast(30),
        anomalies: this.ai.getAnomalies(8),
        risk: this.ai.getCancellationRisk(8),
        pricing: this.ai.getPricing()
      }).subscribe({
        next: (r) => {
          this.forecast = r.forecast;
          this.anomalies = r.anomalies;
          this.risk = r.risk;
          this.pricing = r.pricing;
          this.meta = r.forecast.meta;
          this.isLoading = false;
        },
        error: (err: Error) => {
          this.errorMessage = err.message;
          this.isLoading = false;
        }
      })
    );
  }

  ask(text?: string): void {
    const q = (text ?? this.question).trim();
    if (q.length < 3 || this.asking) return;
    this.chat.push({ role: 'user', text: q });
    this.question = '';
    this.asking = true;
    this.subs.add(
      this.ai.ask(q).subscribe({
        next: (res) => {
          this.chat.push({ role: 'ai', text: res.answer, engine: res.engine });
          this.asking = false;
        },
        error: (err: Error) => {
          this.chat.push({ role: 'ai', text: err.message });
          this.asking = false;
        }
      })
    );
  }

  get pricingRoomTypes(): string[] {
    return this.pricing ? Object.keys(this.pricing.baseAdr) : [];
  }

  riskClass(p: number): string {
    return p >= 0.5 ? 'risk-high' : p >= 0.3 ? 'risk-mid' : 'risk-low';
  }

  private renderForecast(canvas: HTMLCanvasElement): void {
    if (!this.forecast) return;
    this.chart?.destroy();
    const pts = this.forecast.points;
    const labels = pts.map((p) =>
      new Date(p.date).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })
    );
    this.chart = new Chart(canvas, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Upper (95%)',
            data: pts.map((p) => p.upper),
            borderWidth: 0,
            pointRadius: 0,
            fill: '+1',
            backgroundColor: 'rgba(99, 102, 241, 0.15)'
          },
          {
            label: 'Lower (95%)',
            data: pts.map((p) => p.lower),
            borderWidth: 0,
            pointRadius: 0,
            fill: false
          },
          {
            label: 'Forecast revenue',
            data: pts.map((p) => p.forecastRevenue),
            borderColor: '#818cf8',
            backgroundColor: '#818cf8',
            borderWidth: 2,
            pointRadius: 0,
            tension: 0.3
          },
          {
            label: 'Already booked',
            data: pts.map((p) => p.onTheBooksRevenue),
            borderColor: '#34d399',
            backgroundColor: '#34d399',
            borderDash: [4, 4],
            borderWidth: 1.5,
            pointRadius: 0,
            tension: 0.3
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: {
            labels: { color: '#94a3b8', filter: (item) => !item.text.includes('(95%)') }
          },
          tooltip: {
            callbacks: {
              label: (ctx) => `${ctx.dataset.label}: ₹${Number(ctx.parsed.y).toLocaleString('en-IN')}`
            }
          }
        },
        scales: {
          x: { ticks: { color: '#64748b', maxRotation: 0, autoSkip: true, maxTicksLimit: 10 }, grid: { display: false } },
          y: {
            ticks: { color: '#64748b', callback: (v) => '₹' + (Number(v) / 100000).toFixed(1) + 'L' },
            grid: { color: 'rgba(255,255,255,0.05)' }
          }
        }
      }
    });
  }
}
