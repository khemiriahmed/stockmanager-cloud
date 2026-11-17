import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { Order, OrderStatus } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { OrderService } from '../../core/services/order.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { PageHeaderComponent } from '../../shared/page-header.component';

type OrderAction = 'confirm' | 'process' | 'ship' | 'deliver' | 'cancel';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    PageHeaderComponent
  ],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.scss'
})
export class OrderDetailComponent implements OnInit, OnDestroy {
  private readonly service = inject(OrderService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly order = signal<Order | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  private subscription?: Subscription;

  ngOnInit(): void {
    this.subscription = this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      if (id) {
        this.load(id);
      }
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  load(id: number): void {
    this.loading.set(true);
    this.service.get(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        void this.router.navigate(['/orders']);
      }
    });
  }

  statusLabel(status: OrderStatus): string {
    return this.service.statusLabel(status);
  }

  can(action: OrderAction): boolean {
    const order = this.order();
    if (!order) {
      return false;
    }
    const manager = this.auth.canManageCatalog();
    switch (action) {
      case 'confirm':
        return order.status === 'DRAFT';
      case 'cancel':
        return order.status === 'DRAFT' || order.status === 'CONFIRMED' || order.status === 'PROCESSING';
      case 'process':
        return manager && order.status === 'CONFIRMED';
      case 'ship':
        return manager && (order.status === 'CONFIRMED' || order.status === 'PROCESSING');
      case 'deliver':
        return manager && order.status === 'SHIPPED';
      default:
        return false;
    }
  }

  runAction(action: OrderAction): void {
    const order = this.order();
    if (!order || this.busy()) {
      return;
    }
    const labels: Record<OrderAction, string> = {
      confirm: 'Confirmer la commande',
      process: 'Mettre en préparation',
      ship: 'Expédier la commande',
      deliver: 'Livrer la commande',
      cancel: 'Annuler la commande'
    };
    const messages: Record<OrderAction, string> = {
      confirm: 'Le stock sera réservé pour chaque ligne. Continuer ?',
      process: 'La commande passera en préparation. Continuer ?',
      ship: 'Le stock physique sera décrémenté. Continuer ?',
      deliver: 'La commande sera livrée et une facture PDF sera générée. Continuer ?',
      cancel: 'Les réservations éventuelles seront libérées. Continuer ?'
    };
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '460px',
        data: {
          title: labels[action],
          message: messages[action],
          confirmLabel: labels[action],
          danger: action === 'cancel'
        }
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.busy.set(true);
        this.service[action](order.id).subscribe({
          next: (updated) => {
            this.order.set(updated);
            this.busy.set(false);
            this.notification.success(
              action === 'deliver' ? 'Commande livrée, facture générée.' : 'Commande mise à jour.'
            );
          },
          error: () => this.busy.set(false)
        });
      });
  }

  timeline(): Array<{ label: string; date?: string | null }> {
    const order = this.order();
    if (!order) {
      return [];
    }
    return [
      { label: 'Créée', date: order.createdAt },
      { label: 'Confirmée', date: order.confirmedAt },
      { label: 'En préparation', date: order.processingAt },
      { label: 'Expédiée', date: order.shippedAt },
      { label: 'Livrée', date: order.deliveredAt },
      { label: 'Annulée', date: order.cancelledAt }
    ].filter((step) => !!step.date);
  }
}
