import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, RouterLink],
  template: `
    <div class="error-page">
      <div class="code">404</div>
      <h1>Page introuvable</h1>
      <p>La page demandée n'existe pas ou a été déplacée.</p>
      <a mat-flat-button color="primary" routerLink="/dashboard">Retour à l'application</a>
    </div>
  `,
  styles: [
    `
      .error-page {
        min-height: 100vh;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 10px;
        text-align: center;
        background: #f2f5fa;
      }
      .code {
        font-size: 72px;
        font-weight: 800;
        color: #3f5efb;
        letter-spacing: -2px;
      }
      p {
        margin: 0 0 8px;
        color: #6b7a99;
      }
    `
  ]
})
export class NotFoundComponent {}
