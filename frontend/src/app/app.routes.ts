import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { BookingsComponent } from './pages/bookings/bookings.component';
import { AnalyticsComponent } from './pages/analytics/analytics.component';
import { AboutComponent } from './pages/about/about.component';
import { AiInsightsComponent } from './pages/ai-insights/ai-insights.component';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent, title: 'Dashboard | StayPulse' },
  { path: 'bookings', component: BookingsComponent, title: 'Bookings | StayPulse' },
  { path: 'analytics', component: AnalyticsComponent, title: 'Analytics | StayPulse' },
  { path: 'ai-insights', component: AiInsightsComponent, title: 'AI Insights | StayPulse' },
  { path: 'about', component: AboutComponent, title: 'About | StayPulse' },
  { path: '**', redirectTo: 'dashboard' }
];
