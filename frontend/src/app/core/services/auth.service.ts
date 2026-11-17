import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

import { ApiService } from './api.service';
import { LoginRequest, LoginResponse, RoleName, User } from '../models/api.models';

const TOKEN_KEY = 'sm.token';
const USER_KEY = 'sm.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly userSignal = signal<User | null>(this.readUser());

  readonly user = this.userSignal.asReadonly();
  readonly authenticated = computed(() => !!this.userSignal() && !!this.token);

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  login(credentials: LoginRequest) {
    return this.api.post<LoginResponse>('/auth/login', credentials).pipe(
      tap((response) => {
        localStorage.setItem(TOKEN_KEY, response.accessToken);
        localStorage.setItem(USER_KEY, JSON.stringify(response.user));
        this.userSignal.set(response.user);
      })
    );
  }

  logout(redirect = true): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.userSignal.set(null);
    if (redirect) {
      void this.router.navigate(['/login']);
    }
  }

  refreshUser(user: User): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.userSignal.set(user);
  }

  isAuthenticated(): boolean {
    return !!this.token && !!this.userSignal();
  }

  hasRole(...roles: RoleName[]): boolean {
    const user = this.userSignal();
    if (!user) {
      return false;
    }
    return roles.some((role) => user.roles.includes(role));
  }

  hasAnyRole(): boolean {
    return this.hasRole('ADMIN', 'MANAGER', 'EMPLOYEE');
  }

  canManageCatalog(): boolean {
    return this.hasRole('ADMIN', 'MANAGER');
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  private readUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  }
}
