import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { PageQuery, PageResponse, Product } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly api = inject(ApiService);

  list(query: PageQuery): Observable<PageResponse<Product>> {
    return this.api.getPage<Product>('/products', query);
  }

  all(): Observable<Product[]> {
    return this.api.get<Product[]>('/products/all');
  }

  get(id: number): Observable<Product> {
    return this.api.get<Product>(`/products/${id}`);
  }

  create(body: unknown): Observable<Product> {
    return this.api.post<Product>('/products', body);
  }

  update(id: number, body: unknown): Observable<Product> {
    return this.api.put<Product>(`/products/${id}`, body);
  }

  setStatus(id: number, active: boolean): Observable<Product> {
    return this.api.patch<Product>(`/products/${id}/status`, { active });
  }

  deactivate(id: number): Observable<void> {
    return this.api.delete<void>(`/products/${id}`);
  }
}
