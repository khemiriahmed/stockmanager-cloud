import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-pager',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="pagination">
      <span>{{ total }} résultat{{ total > 1 ? 's' : '' }}</span>
      <span class="spacer"></span>
      <button mat-icon-button type="button" [disabled]="page <= 0" (click)="pageChange.emit(page - 1)" aria-label="Page précédente">
        <mat-icon>chevron_left</mat-icon>
      </button>
      <span class="page-label">Page {{ page + 1 }} / {{ totalPages || 1 }}</span>
      <button
        mat-icon-button
        type="button"
        [disabled]="page >= totalPages - 1"
        (click)="pageChange.emit(page + 1)"
        aria-label="Page suivante"
      >
        <mat-icon>chevron_right</mat-icon>
      </button>
    </div>
  `,
  styles: [
    `
      .pagination {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 10px 16px;
        border-top: 1px solid #e3e9f3;
        font-size: 12px;
        color: #6b7a99;
      }
      .spacer {
        flex: 1;
      }
      .page-label {
        min-width: 90px;
        text-align: center;
      }
    `
  ]
})
export class PagerComponent {
  @Input() page = 0;
  @Input() totalPages = 0;
  @Input() total = 0;
  @Output() pageChange = new EventEmitter<number>();
}
