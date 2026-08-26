import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { tap } from 'rxjs';

export const loggingInterceptor: HttpInterceptorFn = (req, next) => {
  const started = Date.now();
  return next(req).pipe(
    tap({
      next: (event) => {
        if (event instanceof HttpResponse) {
          const elapsed = Date.now() - started;
          console.debug(`[HTTP OK] ${req.method} ${req.url} - ${event.status} (${elapsed}ms)`);
        }
      },
      error: (error) => {
        const elapsed = Date.now() - started;
        console.error(`[HTTP ERR] ${req.method} ${req.url} - ${error.status || 0} (${elapsed}ms)`, error);
      }
    })
  );
};
