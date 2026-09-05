import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AnalyticsService } from './analytics.service';
import { environment } from '../../../environments/environment';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AnalyticsService]
    });
    service = TestBed.inject(AnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch total revenue summary from REST API', () => {
    const mockTotal = { totalRevenue: 39990.0, eligibleBookingsCount: 21 };

    service.getTotalRevenue().subscribe((res) => {
      expect(res.totalRevenue).toBe(39990.0);
      expect(res.eligibleBookingsCount).toBe(21);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/analytics/total-revenue`);
    expect(req.request.method).toBe('GET');
    req.flush(mockTotal);
  });

  it('should request top hotels with query limit parameter', () => {
    service.getTopHotels(3).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/analytics/top-hotels?limit=3`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should fetch room type revenue distribution', () => {
    const mockRoomTypes = [
      { roomType: 'DELUXE', totalRevenue: 12000.0, bookingCount: 8, averageRevenue: 1500.0, revenuePercentage: 40.0 }
    ];

    service.getRevenueByRoomType().subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].roomType).toBe('DELUXE');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/analytics/revenue-by-room-type`);
    expect(req.request.method).toBe('GET');
    req.flush(mockRoomTypes);
  });

  it('should fetch hospitality operational metrics', () => {
    const mockOpMetrics = {
      averageDailyRate: 195.5,
      averageLengthOfStay: 3.5,
      totalRoomNights: 140,
      cancellationRate: 5.2,
      totalBookings: 42,
      activeBookings: 40,
      cancelledBookings: 2
    };

    service.getOperationalMetrics().subscribe((res) => {
      expect(res.averageDailyRate).toBe(195.5);
      expect(res.averageLengthOfStay).toBe(3.5);
      expect(res.cancellationRate).toBe(5.2);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/analytics/operational-metrics`);
    expect(req.request.method).toBe('GET');
    req.flush(mockOpMetrics);
  });
});
