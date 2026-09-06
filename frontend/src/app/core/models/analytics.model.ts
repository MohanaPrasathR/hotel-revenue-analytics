import { BookingStatus } from './booking.model';

export interface TotalRevenueResponse {
  totalRevenue: number;
  eligibleBookingsCount: number;
}

export interface HotelRevenueResponse {
  hotelName: string;
  totalRevenue: number;
  bookingCount: number;
}

export interface MonthlyRevenueResponse {
  yearMonth: string; // YYYY-MM
  totalRevenue: number;
  bookingCount: number;
  previousMonthRevenue?: number;
  growthRatePercentage?: number;
}

export interface StatusCountResponse {
  bookingStatus: BookingStatus;
  count: number;
}

export interface AverageRevenueResponse {
  averageRevenuePerBooking: number;
  totalBookingsEvaluated: number;
}

export interface TopHotelResponse {
  rank: number;
  hotelName: string;
  totalRevenue: number;
  totalBookings: number;
}

export interface RoomTypeRevenueResponse {
  roomType: string;
  totalRevenue: number;
  bookingCount: number;
  averageRevenue: number;
  revenuePercentage: number;
}

export interface OperationalMetricsResponse {
  averageDailyRate: number;
  averageLengthOfStay: number;
  totalRoomNights: number;
  cancellationRate: number;
  totalBookings: number;
  activeBookings: number;
  cancelledBookings: number;
}

export interface DashboardSummary {
  totalRevenue: number;
  totalBookings: number;
  activeBookings: number;
  occupancyRate: number;
  averageDailyRate: number;
  cancellations: number;
  topHotelName: string;
  topHotelRevenue: number;
}
