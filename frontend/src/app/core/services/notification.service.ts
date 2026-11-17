import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: 3500, panelClass: 'snack-success' });
  }

  info(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: 3500 });
  }

  error(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: 6000, panelClass: 'snack-error' });
  }
}
