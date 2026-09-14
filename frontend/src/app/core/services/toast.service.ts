import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { ToastMessage, ToastType } from '../models/toast.model';

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private readonly toastsSubject = new BehaviorSubject<ToastMessage[]>([]);
  public readonly toasts$: Observable<ToastMessage[]> = this.toastsSubject.asObservable();

  show(message: string, type: ToastType = 'info', title?: string, duration: number = 4000): void {
    const id = Math.random().toString(36).substring(2, 9);
    const toast: ToastMessage = { id, type, title, message, duration };

    const current = this.toastsSubject.getValue();
    this.toastsSubject.next([...current, toast]);

    if (duration > 0) {
      setTimeout(() => {
        this.dismiss(id);
      }, duration);
    }
  }

  success(message: string, title: string = 'Success'): void {
    this.show(message, 'success', title);
  }

  error(message: string, title: string = 'Error'): void {
    this.show(message, 'error', title, 6000);
  }

  warning(message: string, title: string = 'Warning'): void {
    this.show(message, 'warning', title);
  }

  info(message: string, title: string = 'Information'): void {
    this.show(message, 'info', title);
  }

  dismiss(id: string): void {
    const updated = this.toastsSubject.getValue().filter(t => t.id !== id);
    this.toastsSubject.next(updated);
  }

  clear(): void {
    this.toastsSubject.next([]);
  }
}
