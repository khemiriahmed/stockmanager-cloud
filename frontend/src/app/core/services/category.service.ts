import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { Category, PageQuery, PageResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly api = inject(ApiService);

  list(query: PageQuery): Observable<PageResponse<Category>> {
    return this.api.getPage<Category>('/categories', query);
  }

  all(): Observable<Category[]> {
    return this.api.get<Category[]>('/categories/all');
  }

  create(body: Partial<Category>): Observable<Category> {
    return this.api.post<Category>('/categories', body);
  }

  update(id: number, body: Partial<Category>): Observable<Category> {
    return this.api.put<Category>(`/categories/${id}`, body);
  }

  setStatus(id: number, active: boolean): Observable<Category> {
    return this.api.patch<Category>(`/categories/${id}/status`, { active });
  }

  deactivate(id: number): Observable<void> {
    return this.api.delete<void>(`/categories/${id}`);
  }
}
