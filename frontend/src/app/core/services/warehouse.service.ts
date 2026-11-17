import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { PageQuery, PageResponse, Warehouse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class WarehouseService {
  private readonly api = inject(ApiService);

  list(query: PageQuery): Observable<PageResponse<Warehouse>> {
    return this.api.getPage<Warehouse>('/warehouses', query);
  }

  all(): Observable<Warehouse[]> {
    return this.api.get<Warehouse[]>('/warehouses/all');
  }

  create(body: Partial<Warehouse>): Observable<Warehouse> {
    return this.api.post<Warehouse>('/warehouses', body);
  }

  update(id: number, body: Partial<Warehouse>): Observable<Warehouse> {
    return this.api.put<Warehouse>(`/warehouses/${id}`, body);
  }

  setStatus(id: number, active: boolean): Observable<Warehouse> {
    return this.api.patch<Warehouse>(`/warehouses/${id}/status`, { active });
  }

  deactivate(id: number): Observable<void> {
    return this.api.delete<void>(`/warehouses/${id}`);
  }
}
