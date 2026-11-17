import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { Invoice, PageQuery, PageResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly api = inject(ApiService);

  list(query: PageQuery): Observable<PageResponse<Invoice>> {
    return this.api.getPage<Invoice>('/invoices', query);
  }

  get(id: number): Observable<Invoice> {
    return this.api.get<Invoice>(`/invoices/${id}`);
  }

  download(invoice: Invoice): Observable<Blob> {
    return this.api.getBlob(invoice.downloadPath);
  }

  saveBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }
}
