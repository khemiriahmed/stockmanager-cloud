import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, forkJoin } from 'rxjs';

import { Customer, OrderStatus, OrderSummary, Product, Warehouse } from '../../core/models/api.models';
import { CustomerService } from '../../core/services/customer.service';
import { NotificationService } from '../../core/services/notification.service';
import { OrderService } from '../../core/services/order.service';
import { ProductService } from '../../core/services/product.service';
import { WarehouseService } from '../../core/services/warehouse.service';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { PagerComponent } from '../../shared/pager.component';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    RouterLink,
    PageHeaderComponent,
    PagerComponent
  ],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.scss'
})
export class OrdersComponent implements OnInit {
  private readonly service = inject(OrderService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  readonly search = new FormControl('', { nonNullable: true });
  readonly statusFilter = new FormControl<'ALL' | OrderStatus>('ALL');
  readonly items = signal<OrderSummary[]>([]);
  readonly loading = signal(true);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);

  ngOnInit(): void {
    this.search.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => this.load(0));
    this.statusFilter.valueChanges.subscribe(() => this.load(0));
    this.load(0);
  }

  load(page = this.page()): void {
    this.loading.set(true);
    const status = this.statusFilter.value;
    this.service
      .list({ page, size: 10, sort: 'createdAt,desc', search: this.search.value, status: status === 'ALL' ? undefined : status })
      .subscribe({
        next: (response) => {
          this.items.set(response.content);
          this.page.set(response.page);
          this.totalPages.set(response.totalPages);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  openCreate(): void {
    const ref = this.dialog.open(OrderFormDialogComponent, { width: '860px', maxWidth: '96vw' });
    ref.afterClosed().subscribe((orderId) => {
      if (orderId) {
        this.notification.success('Commande créée en brouillon.');
        void this.router.navigate(['/orders', orderId]);
      }
    });
  }

  statusLabel(status: OrderStatus): string {
    return this.service.statusLabel(status);
  }
}

@Component({
  selector: 'app-order-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Nouvelle commande</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <div class="dialog-form order-form">
          <div class="form-grid">
            <mat-form-field appearance="outline">
              <mat-label>Client</mat-label>
              <mat-select formControlName="customerId">
                @for (customer of customers; track customer.id) {
                  <mat-option [value]="customer.id">{{ customer.name }}{{ customer.company ? ' — ' + customer.company : '' }}</mat-option>
                }
              </mat-select>
              @if (form.controls.customerId.hasError('required')) {
                <mat-error>Le client est obligatoire.</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Taux de TVA (%)</mat-label>
              <input matInput type="number" min="0" max="100" step="0.1" formControlName="vatRate" />
            </mat-form-field>
          </div>

          <div class="lines-head">
            <strong>Lignes de commande</strong>
            <button mat-stroked-button type="button" (click)="addLine()">
              <mat-icon>add</mat-icon>
              Ajouter une ligne
            </button>
          </div>

          <div formArrayName="items" class="lines">
            @for (line of items.controls; track line; let i = $index) {
              <div [formGroupName]="i" class="line">
                <mat-form-field appearance="outline" class="line-product">
                  <mat-label>Produit</mat-label>
                  <mat-select formControlName="productId">
                    @for (product of products; track product.id) {
                      <mat-option [value]="product.id">{{ product.reference }} — {{ product.name }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="outline" class="line-warehouse">
                  <mat-label>Entrepôt</mat-label>
                  <mat-select formControlName="warehouseId">
                    @for (warehouse of warehouses; track warehouse.id) {
                      <mat-option [value]="warehouse.id">{{ warehouse.name }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="outline" class="line-qty">
                  <mat-label>Qté</mat-label>
                  <input matInput type="number" min="1" step="1" formControlName="quantity" />
                </mat-form-field>
                <div class="line-total">
                  <span>Total HT</span>
                  <strong>{{ lineTotal(line.value) | currency: 'EUR' }}</strong>
                </div>
                <button
                  mat-icon-button
                  type="button"
                  color="warn"
                  [disabled]="items.length === 1"
                  (click)="removeLine(i)"
                  aria-label="Supprimer la ligne"
                >
                  <mat-icon>delete_outline</mat-icon>
                </button>
              </div>
            }
          </div>

          <mat-form-field appearance="outline">
            <mat-label>Notes internes</mat-label>
            <textarea matInput rows="2" formControlName="notes" maxlength="2000"></textarea>
          </mat-form-field>

          <div class="order-totals">
            <div><span>Total HT</span><strong>{{ totalHt() | currency: 'EUR' }}</strong></div>
            <div><span>TVA ({{ form.controls.vatRate.value }}%)</span><strong>{{ totalVat() | currency: 'EUR' }}</strong></div>
            <div class="ttc"><span>Total TTC</span><strong>{{ totalTtc() | currency: 'EUR' }}</strong></div>
          </div>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Annuler</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="saving || loading">Créer la commande</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: [
    `
      .order-form {
        width: 100%;
        max-width: none;
      }
      .lines-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin: 10px 0;
      }
      .lines {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .line {
        display: grid;
        grid-template-columns: minmax(0, 2.2fr) minmax(0, 1.4fr) 90px 120px 40px;
        gap: 10px;
        align-items: center;
        padding: 10px;
        border: 1px solid #e3e9f3;
        border-radius: 12px;
        background: #fbfcff;
      }
      .line mat-form-field {
        font-size: 13px;
      }
      .line-total {
        display: flex;
        flex-direction: column;
        text-align: right;
      }
      .line-total span {
        font-size: 10px;
        text-transform: uppercase;
        color: #6b7a99;
      }
      .order-totals {
        display: flex;
        flex-direction: column;
        gap: 6px;
        align-items: flex-end;
        padding: 12px 4px 0;
      }
      .order-totals div {
        display: flex;
        gap: 24px;
        justify-content: space-between;
        min-width: 260px;
        font-size: 13px;
      }
      .order-totals .ttc {
        border-top: 1px solid #e3e9f3;
        padding-top: 8px;
        font-size: 16px;
      }
      @media (max-width: 760px) {
        .line {
          grid-template-columns: 1fr 1fr;
        }
        .line-total {
          text-align: left;
        }
      }
    `
  ]
})
export class OrderFormDialogComponent {
  private readonly service = inject(OrderService);
  private readonly fb = inject(FormBuilder);
  loading = true;
  saving = false;
  customers: Customer[] = [];
  products: Product[] = [];
  warehouses: Warehouse[] = [];

  readonly form = this.fb.nonNullable.group({
    customerId: [null as number | null, [Validators.required]],
    vatRate: [20, [Validators.required, Validators.min(0), Validators.max(100)]],
    notes: ['', [Validators.maxLength(2000)]],
    items: this.fb.array([this.createLine()])
  });

  constructor(private readonly dialogRef: MatDialogRef<OrderFormDialogComponent, number>) {
    forkJoin({
      customers: inject(CustomerService).all(),
      products: inject(ProductService).all(),
      warehouses: inject(WarehouseService).all()
    }).subscribe({
      next: ({ customers, products, warehouses }) => {
        this.customers = customers;
        this.products = products;
        this.warehouses = warehouses;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  get items(): FormArray {
    return this.form.controls.items;
  }

  addLine(): void {
    this.items.push(this.createLine());
  }

  removeLine(index: number): void {
    if (this.items.length > 1) {
      this.items.removeAt(index);
    }
  }

  lineTotal(value: { productId: number | null; quantity: number }): number {
    const product = this.products.find((item) => item.id === value.productId);
    return Number(product?.sellingPrice ?? 0) * Number(value.quantity || 0);
  }

  totalHt(): number {
    return this.items.controls.reduce((sum, line) => sum + this.lineTotal(line.value), 0);
  }

  totalVat(): number {
    return (this.totalHt() * Number(this.form.controls.vatRate.value || 0)) / 100;
  }

  totalTtc(): number {
    return this.totalHt() + this.totalVat();
  }

  submit(): void {
    if (this.form.invalid || this.loading) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const value = this.form.getRawValue();
    const body = {
      customerId: value.customerId,
      vatRate: value.vatRate,
      notes: value.notes || undefined,
      items: value.items.map((line) => ({
        productId: line.productId,
        warehouseId: line.warehouseId,
        quantity: line.quantity
      }))
    };
    this.service.create(body).subscribe({
      next: (order) => this.dialogRef.close(order.id),
      error: () => (this.saving = false)
    });
  }

  private createLine() {
    return this.fb.nonNullable.group({
      productId: [null as number | null, [Validators.required]],
      warehouseId: [null as number | null, [Validators.required]],
      quantity: [1, [Validators.required, Validators.min(1)]]
    });
  }
}
