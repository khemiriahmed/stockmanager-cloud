import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { Customer, PageQuery, PageResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly api = inject(ApiService);

  list(query: PageQuery): Observable<PageResponse<Customer>> {
    return this.api.getPage<Customer>('/customers', query);
  }

  all(): Observable<Customer[]> {
    return this.api.get<Customer[]>('/customers/all');
  }

  create(body: Partial<Customer>): Observable<Customer> {
    return this.api.post<Customer>('/customers', body);
  }

  update(id: number, body: Partial<Customer>): Observable<Customer> {
    return this.api.put<Customer>(`/customers/${id}`, body);
  }

  setStatus(id: number, active: boolean): Observable<Customer> {
    return this.api.patch<Customer>(`/customers/${id}/status`, { active });
  }

  deactivate(id: number): Observable<void> {
    return this.api.delete<void>(`/customers/${id}`);
  }
}
