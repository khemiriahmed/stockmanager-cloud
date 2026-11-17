import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { PageQuery, PageResponse, RoleName, User } from '../models/api.models';

export interface CreateUserBody {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  roles: RoleName[];
}

export interface UpdateUserBody {
  firstName: string;
  lastName: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly api = inject(ApiService);

  me(): Observable<User> {
    return this.api.get<User>('/users/me');
  }

  updateProfile(body: { firstName: string; lastName: string }): Observable<User> {
    return this.api.put<User>('/users/me', body);
  }

  changePassword(body: { currentPassword: string; newPassword: string }): Observable<void> {
    return this.api.put<void>('/users/me/password', body);
  }

  list(query: PageQuery): Observable<PageResponse<User>> {
    return this.api.getPage<User>('/users', query);
  }

  create(body: CreateUserBody): Observable<User> {
    return this.api.post<User>('/users', body);
  }

  update(id: number, body: UpdateUserBody): Observable<User> {
    return this.api.put<User>(`/users/${id}`, body);
  }

  setStatus(id: number, active: boolean): Observable<User> {
    return this.api.patch<User>(`/users/${id}/status`, { active });
  }

  updateRoles(id: number, roles: RoleName[]): Observable<User> {
    return this.api.put<User>(`/users/${id}/roles`, { roles });
  }
}
