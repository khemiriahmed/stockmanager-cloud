import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { Order, OrderStatus, OrderSummary, PageQuery, PageResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly api = inject(ApiService);

  list(query: PageQuery): Observable<PageResponse<OrderSummary>> {
    return this.api.getPage<OrderSummary>('/orders', query);
  }

  get(id: number): Observable<Order> {
    return this.api.get<Order>(`/orders/${id}`);
  }

  create(body: unknown): Observable<Order> {
    return this.api.post<Order>('/orders', body);
  }

  confirm(id: number): Observable<Order> {
    return this.api.post<Order>(`/orders/${id}/confirm`, {});
  }

  process(id: number): Observable<Order> {
    return this.api.post<Order>(`/orders/${id}/process`, {});
  }

  ship(id: number): Observable<Order> {
    return this.api.post<Order>(`/orders/${id}/ship`, {});
  }

  deliver(id: number): Observable<Order> {
    return this.api.post<Order>(`/orders/${id}/deliver`, {});
  }

  cancel(id: number): Observable<Order> {
    return this.api.post<Order>(`/orders/${id}/cancel`, {});
  }

  statusLabel(status: OrderStatus): string {
    const labels: Record<OrderStatus, string> = {
      DRAFT: 'Brouillon',
      CONFIRMED: 'Confirmée',
      PROCESSING: 'En préparation',
      SHIPPED: 'Expédiée',
      DELIVERED: 'Livrée',
      CANCELLED: 'Annulée'
    };
    return labels[status];
  }
}
