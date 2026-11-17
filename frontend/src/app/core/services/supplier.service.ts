import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { PageQuery, PageResponse, Supplier } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly api = inject(ApiService);

  list(query: PageQuery): Observable<PageResponse<Supplier>> {
    return this.api.getPage<Supplier>('/suppliers', query);
  }

  all(): Observable<Supplier[]> {
    return this.api.get<Supplier[]>('/suppliers/all');
  }

  create(body: Partial<Supplier>): Observable<Supplier> {
    return this.api.post<Supplier>('/suppliers', body);
  }

  update(id: number, body: Partial<Supplier>): Observable<Supplier> {
    return this.api.put<Supplier>(`/suppliers/${id}`, body);
  }

  setStatus(id: number, active: boolean): Observable<Supplier> {
    return this.api.patch<Supplier>(`/suppliers/${id}/status`, { active });
  }

  deactivate(id: number): Observable<void> {
    return this.api.delete<void>(`/suppliers/${id}`);
  }
}
