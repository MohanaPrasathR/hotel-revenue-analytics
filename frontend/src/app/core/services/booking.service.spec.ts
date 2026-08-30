import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BookingService } from './booking.service';
import { environment } from '../../../environments/environment';
import { Booking } from '../models/booking.model';

describe('BookingService', () => {
  let service: BookingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [BookingService]
    });
    service = TestBed.inject(BookingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should retrieve all hotel bookings', () => {
    const mockBookings: Partial<Booking>[] = [
      { id: 1, guestName: 'Alice', hotelName: 'Grand Horizon' }
    ];

    service.getAllBookings().subscribe((bookings) => {
      expect(bookings.length).toBe(1);
      expect(bookings[0].guestName).toBe('Alice');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/bookings`);
    expect(req.request.method).toBe('GET');
    req.flush(mockBookings);
  });

  it('should send DELETE request for specific booking ID', () => {
    service.deleteBooking(42).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/bookings/42`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
