import { BreakpointObserver } from '@angular/cdk/layout';
import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

import { RoleName } from '../core/models/api.models';
import { AuthService } from '../core/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  path: string;
  roles?: RoleName[];
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Tableau de bord', icon: 'space_dashboard', path: '/dashboard' },
  { label: 'Produits', icon: 'inventory_2', path: '/products' },
  { label: 'Catégories', icon: 'category', path: '/categories' },
  { label: 'Fournisseurs', icon: 'local_shipping', path: '/suppliers' },
  { label: 'Clients', icon: 'groups', path: '/customers' },
  { label: 'Entrepôts', icon: 'warehouse', path: '/warehouses' },
  { label: 'Stocks & mouvements', icon: 'swap_vert', path: '/stocks' },
  { label: 'Commandes', icon: 'receipt_long', path: '/orders' },
  { label: 'Factures', icon: 'request_quote', path: '/invoices' },
  { label: 'Utilisateurs', icon: 'manage_accounts', path: '/users', roles: ['ADMIN', 'MANAGER'] }
];

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatListModule,
    MatMenuModule,
    MatDividerModule
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly breakpoint = inject(BreakpointObserver);

  readonly isHandset = toSignal(
    this.breakpoint.observe('(max-width: 959px)').pipe(map((result) => result.matches)),
    { initialValue: false }
  );
  readonly opened = signal(true);
  readonly user = this.auth.user;
  readonly navItems = computed(() => NAV_ITEMS.filter((item) => !item.roles || this.auth.hasRole(...item.roles)));
  readonly initials = computed(() => {
    const user = this.user();
    if (!user) {
      return '?';
    }
    return `${user.firstName?.charAt(0) ?? ''}${user.lastName?.charAt(0) ?? ''}`.toUpperCase();
  });
  readonly primaryRole = computed(() => {
    const roles = this.user()?.roles ?? [];
    if (roles.includes('ADMIN')) {
      return 'Administrateur';
    }
    if (roles.includes('MANAGER')) {
      return 'Manager';
    }
    return 'Employé';
  });

  constructor() {
    effect(() => this.opened.set(!this.isHandset()), { allowSignalWrites: true });
  }

  toggle(): void {
    this.opened.update((value) => !value);
  }

  onNavigate(): void {
    if (this.isHandset()) {
      this.opened.set(false);
    }
  }

  logout(): void {
    this.auth.logout();
  }
}
