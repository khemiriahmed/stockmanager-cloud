import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { Invoice } from '../../core/models/api.models';
import { InvoiceService } from '../../core/services/invoice.service';
import { NotificationService } from '../../core/services/notification.service';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { PagerComponent } from '../../shared/pager.component';

@Component({
  selector: 'app-invoices',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    RouterLink,
    PageHeaderComponent,
    PagerComponent
  ],
  templateUrl: './invoices.component.html',
  styleUrl: './invoices.component.scss'
})
export class InvoicesComponent implements OnInit {
  private readonly service = inject(InvoiceService);
  private readonly notification = inject(NotificationService);

  readonly search = new FormControl('', { nonNullable: true });
  readonly items = signal<Invoice[]>([]);
  readonly loading = signal(true);
  readonly downloading = signal<number | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);

  ngOnInit(): void {
    this.search.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => this.load(0));
    this.load(0);
  }

  load(page = this.page()): void {
    this.loading.set(true);
    this.service.list({ page, size: 10, sort: 'issuedAt,desc', search: this.search.value }).subscribe({
      next: (response) => {
        this.items.set(response.content);
        this.page.set(response.page);
        this.totalPages.set(response.totalPages);
        this.total.set(response.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  download(invoice: Invoice): void {
    this.downloading.set(invoice.id);
    this.service.download(invoice).subscribe({
      next: (blob) => {
        this.service.saveBlob(blob, `${invoice.number}.pdf`);
        this.downloading.set(null);
        this.notification.success('Facture téléchargée.');
      },
      error: () => this.downloading.set(null)
    });
  }
}
