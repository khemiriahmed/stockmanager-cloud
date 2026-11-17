import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { MovementType, PageQuery, PageResponse, Stock, StockMovement } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly api = inject(ApiService);

  list(query: PageQuery): Observable<PageResponse<Stock>> {
    return this.api.getPage<Stock>('/stocks', query);
  }

  lowStock(query: PageQuery): Observable<PageResponse<Stock>> {
    return this.api.getPage<Stock>('/stocks/low-stock', query);
  }

  movements(query: PageQuery): Observable<PageResponse<StockMovement>> {
    return this.api.getPage<StockMovement>('/stocks/movements', query);
  }

  entry(body: unknown): Observable<Stock> {
    return this.api.post<Stock>('/stocks/entry', body);
  }

  exit(body: unknown): Observable<Stock> {
    return this.api.post<Stock>('/stocks/exit', body);
  }

  transfer(body: unknown): Observable<Stock> {
    return this.api.post<Stock>('/stocks/transfer', body);
  }

  adjustment(body: unknown): Observable<Stock> {
    return this.api.post<Stock>('/stocks/adjustment', body);
  }

  movementLabel(type: MovementType): string {
    const labels: Record<MovementType, string> = {
      IN: 'Entrée',
      OUT: 'Sortie',
      TRANSFER: 'Transfert',
      ADJUSTMENT: 'Ajustement'
    };
    return labels[type];
  }
}
