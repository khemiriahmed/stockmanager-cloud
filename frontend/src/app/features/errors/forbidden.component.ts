import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, RouterLink],
  template: `
    <div class="error-page">
      <mat-icon>lock</mat-icon>
      <h1>Accès refusé</h1>
      <p>Votre rôle ne vous permet pas d'accéder à cette page.</p>
      <a mat-flat-button color="primary" routerLink="/dashboard">Retour au tableau de bord</a>
    </div>
  `,
  styles: [
    `
      .error-page {
        min-height: 60vh;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 12px;
        text-align: center;
      }
      mat-icon {
        font-size: 56px;
        width: 56px;
        height: 56px;
        color: #e5484d;
      }
      p {
        margin: 0 0 8px;
        color: #6b7a99;
      }
    `
  ]
})
export class ForbiddenComponent {}
