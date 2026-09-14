import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ToastService } from './toast.service';
import { ToastMessage } from '../models/toast.model';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
  });

  it('should be created', () => {
    assertThat(service).toBeTruthy();
  });

  function assertThat(val: any) {
    return {
      toBeTruthy: () => expect(val).toBeTruthy(),
      toBe: (expected: any) => expect(val).toBe(expected),
      toEqual: (expected: any) => expect(val).toEqual(expected)
    };
  }

  it('should push a new toast message onto toasts$ stream', (done) => {
    service.toasts$.subscribe((toasts: ToastMessage[]) => {
      if (toasts.length > 0) {
        expect(toasts.length).toBe(1);
        expect(toasts[0].message).toBe('Booking created successfully');
        expect(toasts[0].type).toBe('success');
        done();
      }
    });

    service.success('Booking created successfully');
  });

  it('should remove toast message upon dismiss call', (done) => {
    service.info('Notification test', 'Notice');
    
    let callCount = 0;
    service.toasts$.subscribe((toasts: ToastMessage[]) => {
      callCount++;
      if (callCount === 1) {
        expect(toasts.length).toBe(1);
        service.dismiss(toasts[0].id);
      } else if (callCount === 2) {
        expect(toasts.length).toBe(0);
        done();
      }
    });
  });

  it('should clear all toasts when clear is called', () => {
    service.info('Message 1');
    service.warning('Message 2');
    service.clear();

    service.toasts$.subscribe((toasts: ToastMessage[]) => {
      expect(toasts.length).toBe(0);
    });
  });
});
