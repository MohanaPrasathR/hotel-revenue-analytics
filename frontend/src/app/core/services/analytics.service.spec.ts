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
});
