import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { NotificationService } from '../services/notification.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const notification = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/auth/login')) {
        localStorage.removeItem('sm.token');
        localStorage.removeItem('sm.user');
        notification.error('Votre session a expiré. Veuillez vous reconnecter.');
        void router.navigate(['/login']);
      } else if (error.status === 403) {
        notification.error(error.error?.detail ?? "Vous n'avez pas les droits nécessaires.");
      } else if (error.status === 0) {
        notification.error('Impossible de joindre le serveur. Vérifiez votre connexion.');
      } else if (error.status >= 500) {
        notification.error(error.error?.detail ?? 'Une erreur serveur est survenue.');
      }

      return throwError(() => error);
    })
  );
};
