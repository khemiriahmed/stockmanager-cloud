import { CommonModule, CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import Chart from 'chart.js/auto';

import { DashboardOverview } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { NotificationService } from '../../core/services/notification.service';
import { OrderService } from '../../core/services/order.service';
import { PageHeaderComponent } from '../../shared/page-header.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    PageHeaderComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private readonly notification = inject(NotificationService);
  private readonly orderService = inject(OrderService);
  readonly auth = inject(AuthService);

  @ViewChild('salesCanvas') salesCanvas?: ElementRef<HTMLCanvasElement>;

  overview: DashboardOverview | null = null;
  loading = true;
  private chart?: Chart;

  ngOnInit(): void {
    this.dashboardService.overview().subscribe({
      next: (data) => {
        this.overview = data;
        this.loading = false;
        setTimeout(() => this.renderChart());
      },
      error: () => {
        this.loading = false;
        this.notification.error('Impossible de charger les indicateurs.');
      }
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  statusLabel = (status: string): string => this.orderService.statusLabel(status as never);

  private renderChart(): void {
    const canvas = this.salesCanvas?.nativeElement;
    if (!canvas || !this.overview) {
      return;
    }
    this.chart?.destroy();
    const labels = this.overview.sales.map((point) =>
      new Date(`${point.period}-01T00:00:00Z`).toLocaleDateString('fr-FR', { month: 'short', year: '2-digit' })
    );
    this.chart = new Chart(canvas, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            type: 'bar',
            label: "Chiffre d'affaires TTC",
            data: this.overview.sales.map((point) => Number(point.sales)),
            backgroundColor: 'rgba(63, 94, 251, 0.75)',
            borderRadius: 6,
            maxBarThickness: 26
          },
          {
            type: 'line',
            label: 'Commandes',
            data: this.overview.sales.map((point) => point.orders),
            borderColor: '#12b886',
            backgroundColor: '#12b886',
            tension: 0.35,
            pointRadius: 3,
            yAxisID: 'y1'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: { position: 'bottom', labels: { boxWidth: 12, usePointStyle: true } }
        },
        scales: {
          x: { grid: { display: false } },
          y: {
            beginAtZero: true,
            grid: { color: 'rgba(23, 35, 63, 0.06)' },
            ticks: { callback: (value) => `${Number(value) / 1000}k` }
          },
          y1: {
            position: 'right',
            beginAtZero: true,
            grid: { display: false },
            ticks: { precision: 0 }
          }
        }
      }
    });
  }
}
