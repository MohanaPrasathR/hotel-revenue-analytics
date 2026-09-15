import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnalyticsComponent } from './analytics.component';
import { AnalyticsService } from '../../core/services/analytics.service';
import { of } from 'rxjs';

describe('AnalyticsComponent', () => {
  let component: AnalyticsComponent;
  let fixture: ComponentFixture<AnalyticsComponent>;
  let mockAnalyticsService: jasmine.SpyObj<AnalyticsService>;

  beforeEach(async () => {
    mockAnalyticsService = jasmine.createSpyObj('AnalyticsService', [
      'getTotalRevenue',
      'getAverageRevenue',
      'getRevenueByHotel',
      'getRevenueByMonth',
      'getBookingCountByStatus',
      'getTopHotels',
      'getRevenueByRoomType',
      'getOperationalMetrics',
      'exportBookingsCsv'
    ]);

    mockAnalyticsService.getTotalRevenue.and.returnValue(of({ totalRevenue: 10000, eligibleBookingsCount: 10 }));
    mockAnalyticsService.getAverageRevenue.and.returnValue(of({ averageRevenue: 1000, eligibleBookingsCount: 10 }));
    mockAnalyticsService.getRevenueByHotel.and.returnValue(of([]));
    mockAnalyticsService.getRevenueByMonth.and.returnValue(of([]));
    mockAnalyticsService.getBookingCountByStatus.and.returnValue(of([]));
    mockAnalyticsService.getTopHotels.and.returnValue(of([]));
    mockAnalyticsService.getRevenueByRoomType.and.returnValue(of([]));
    mockAnalyticsService.getOperationalMetrics.and.returnValue(of({
      averageDailyRate: 150,
      averageLengthOfStay: 2.5,
      totalRoomNights: 25,
      cancellationRate: 10,
      totalBookings: 12,
      activeBookings: 10,
      cancelledBookings: 2
    }));

    await TestBed.configureTestingModule({
      imports: [AnalyticsComponent],
      providers: [
        { provide: AnalyticsService, useValue: mockAnalyticsService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyticsComponent);
    component = fixture.componentInstance;
  });

  it('should create the analytics component', () => {
    expect(component).toBeTruthy();
  });

  it('should default to "all" date preset on initialization', () => {
    expect(component.selectedPreset).toBe('all');
  });

  it('should update selectedPreset and reload analytics on applyPreset', () => {
    spyOn(component, 'loadAnalytics');
    component.applyPreset('30d');
    expect(component.selectedPreset).toBe('30d');
    expect(component.loadAnalytics).toHaveBeenCalled();
  });

  it('should load all analytical metrics via forkJoin on ngOnInit', () => {
    component.ngOnInit();
    expect(mockAnalyticsService.getTotalRevenue).toHaveBeenCalled();
    expect(mockAnalyticsService.getOperationalMetrics).toHaveBeenCalled();
    expect(component.totalRevenue?.totalRevenue).toBe(10000);
    expect(component.isLoading).toBeFalse();
  });
});
