import { Routes } from '@angular/router';

import { authGuard, roleGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    path: 'login',
    canMatch: [guestGuard],
    title: 'Connexion | StockManager Cloud',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Tableau de bord | StockManager Cloud',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'products',
        title: 'Produits | StockManager Cloud',
        loadComponent: () => import('./features/products/products.component').then((m) => m.ProductsComponent)
      },
      {
        path: 'categories',
        title: 'Catégories | StockManager Cloud',
        loadComponent: () => import('./features/categories/categories.component').then((m) => m.CategoriesComponent)
      },
      {
        path: 'suppliers',
        title: 'Fournisseurs | StockManager Cloud',
        loadComponent: () => import('./features/suppliers/suppliers.component').then((m) => m.SuppliersComponent)
      },
      {
        path: 'customers',
        title: 'Clients | StockManager Cloud',
        loadComponent: () => import('./features/customers/customers.component').then((m) => m.CustomersComponent)
      },
      {
        path: 'warehouses',
        title: 'Entrepôts | StockManager Cloud',
        loadComponent: () => import('./features/warehouses/warehouses.component').then((m) => m.WarehousesComponent)
      },
      {
        path: 'stocks',
        title: 'Stocks | StockManager Cloud',
        loadComponent: () => import('./features/stocks/stocks.component').then((m) => m.StocksComponent)
      },
      {
        path: 'orders',
        title: 'Commandes | StockManager Cloud',
        loadComponent: () => import('./features/orders/orders.component').then((m) => m.OrdersComponent)
      },
      {
        path: 'orders/:id',
        title: 'Détail commande | StockManager Cloud',
        loadComponent: () => import('./features/orders/order-detail.component').then((m) => m.OrderDetailComponent)
      },
      {
        path: 'invoices',
        title: 'Factures | StockManager Cloud',
        loadComponent: () => import('./features/invoices/invoices.component').then((m) => m.InvoicesComponent)
      },
      {
        path: 'users',
        canActivate: [roleGuard('ADMIN', 'MANAGER')],
        title: 'Utilisateurs | StockManager Cloud',
        loadComponent: () => import('./features/users/users.component').then((m) => m.UsersComponent)
      },
      {
        path: 'profile',
        title: 'Mon profil | StockManager Cloud',
        loadComponent: () => import('./features/profile/profile.component').then((m) => m.ProfileComponent)
      },
      {
        path: 'forbidden',
        title: 'Accès refusé | StockManager Cloud',
        loadComponent: () => import('./features/errors/forbidden.component').then((m) => m.ForbiddenComponent)
      }
    ]
  },
  {
    path: '**',
    title: 'Page introuvable | StockManager Cloud',
    loadComponent: () => import('./features/errors/not-found.component').then((m) => m.NotFoundComponent)
  }
];
