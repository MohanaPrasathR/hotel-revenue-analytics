export interface AiMeta {
  dataSource: 'api' | 'demo';
  note: string;
  loadedAt: string;
  bookings: number;
}

export interface ForecastPoint {
  date: string;
  forecastRevenue: number;
  lower: number;
  upper: number;
  forecastRoomsSold: number;
  onTheBooksRevenue: number;
}

export interface ForecastResponse {
  meta: AiMeta;
  model: string;
  horizonDays: number;
  points: ForecastPoint[];
  totalForecastRevenue: number;
  changeVsLast30DaysPct: number | null;
  backtest: { days: number; modelMAPE: number; seasonalNaiveMAPE: number };
}

export interface AnomalyItem {
  bookingId: number;
  hotelName: string;
  guestName: string;
  checkInDate: string;
  roomType: string;
  totalRevenue: number;
  anomalyScore: number;
  field: string;
  reason: string;
}

export interface AnomalyResponse {
  meta: AiMeta;
  model: string;
  count: number;
  items: AnomalyItem[];
}

export interface RiskItem {
  bookingId: number;
  hotelName: string;
  guestName: string;
  checkInDate: string;
  roomType: string;
  leadTimeDays: number;
  totalRevenue: number;
  cancellationRisk: number;
}

export interface CancellationRiskResponse {
  meta: AiMeta;
  model: string;
  metrics?: { rocAuc: number; precision: number; recall: number; baseCancellationRatePct: number; testSize: number };
  featureImportance?: { feature: string; importance: number }[];
  upcomingBookings?: number;
  expectedRevenueAtRisk?: number;
  items: RiskItem[];
  note?: string;
}

export interface PricingDay {
  date: string;
  demandIndex: number;
  action: 'raise' | 'hold' | 'discount';
  rates: Record<string, number>;
}

export interface PricingResponse {
  meta: AiMeta;
  method: string;
  baseAdr: Record<string, number>;
  days: PricingDay[];
}

export interface AskResponse {
  answer: string;
  engine: 'llm' | 'rules';
}
