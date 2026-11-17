import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { PageQuery, PageResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = '/api/v1';

  constructor(private readonly http: HttpClient) {}

  get<T>(path: string, params?: PageQuery): Observable<T> {
    return this.http.get<T>(this.url(path), { params: this.toParams(params) });
  }

  getPage<T>(path: string, params?: PageQuery): Observable<PageResponse<T>> {
    return this.get<PageResponse<T>>(path, params);
  }

  getBlob(path: string): Observable<Blob> {
    return this.http.get(this.url(path), { responseType: 'blob' });
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<T>(this.url(path), body);
  }

  put<T>(path: string, body: unknown): Observable<T> {
    return this.http.put<T>(this.url(path), body);
  }

  patch<T>(path: string, body: unknown): Observable<T> {
    return this.http.patch<T>(this.url(path), body);
  }

  delete<T>(path: string): Observable<T> {
    return this.http.delete<T>(this.url(path));
  }

  private url(path: string): string {
    return path.startsWith('/api/') ? path : `${this.baseUrl}${path}`;
  }

  private toParams(query?: PageQuery): HttpParams {
    let params = new HttpParams();
    if (!query) {
      return params;
    }
    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return params;
  }
}
