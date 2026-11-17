import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { debounceTime, distinctUntilChanged, forkJoin } from 'rxjs';

import { MovementType, Product, Stock, StockMovement, Warehouse } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { ProductService } from '../../core/services/product.service';
import { StockService } from '../../core/services/stock.service';
import { WarehouseService } from '../../core/services/warehouse.service';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { PagerComponent } from '../../shared/pager.component';

type StockOperation = 'entry' | 'exit' | 'transfer' | 'adjustment';

@Component({
  selector: 'app-stocks',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
    PagerComponent
  ],
  templateUrl: './stocks.component.html',
  styleUrl: './stocks.component.scss'
})
export class StocksComponent implements OnInit {
  private readonly service = inject(StockService);
  private readonly warehouseService = inject(WarehouseService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);

  readonly search = new FormControl('', { nonNullable: true });
  readonly typeFilter = new FormControl<'ALL' | MovementType>('ALL');
  readonly stocks = signal<Stock[]>([]);
  readonly lowStocks = signal<Stock[]>([]);
  readonly movements = signal<StockMovement[]>([]);
  readonly warehouses = signal<Warehouse[]>([]);
  readonly loadingStocks = signal(true);
  readonly loadingLow = signal(true);
  readonly loadingMovements = signal(true);
  readonly stockPage = signal(0);
  readonly stockTotalPages = signal(0);
  readonly stockTotal = signal(0);
  readonly lowPage = signal(0);
  readonly lowTotalPages = signal(0);
  readonly lowTotal = signal(0);
  readonly movePage = signal(0);
  readonly moveTotalPages = signal(0);
  readonly moveTotal = signal(0);

  ngOnInit(): void {
    this.warehouseService.all().subscribe((warehouses) => this.warehouses.set(warehouses));
    this.search.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => this.loadStocks(0));
    this.typeFilter.valueChanges.subscribe(() => this.loadMovements(0));
    this.loadStocks(0);
    this.loadLowStock(0);
    this.loadMovements(0);
  }

  loadStocks(page = this.stockPage()): void {
    this.loadingStocks.set(true);
    this.service.list({ page, size: 10, sort: 'product.name', search: this.search.value }).subscribe({
      next: (response) => {
        this.stocks.set(response.content);
        this.stockPage.set(response.page);
        this.stockTotalPages.set(response.totalPages);
        this.stockTotal.set(response.totalElements);
        this.loadingStocks.set(false);
      },
      error: () => this.loadingStocks.set(false)
    });
  }

  loadLowStock(page = this.lowPage()): void {
    this.loadingLow.set(true);
    this.service.lowStock({ page, size: 10, sort: 'quantity,asc' }).subscribe({
      next: (response) => {
        this.lowStocks.set(response.content);
        this.lowPage.set(response.page);
        this.lowTotalPages.set(response.totalPages);
        this.lowTotal.set(response.totalElements);
        this.loadingLow.set(false);
      },
      error: () => this.loadingLow.set(false)
    });
  }

  loadMovements(page = this.movePage()): void {
    this.loadingMovements.set(true);
    const type = this.typeFilter.value;
    this.service
      .movements({ page, size: 10, sort: 'createdAt,desc', type: type === 'ALL' ? undefined : type })
      .subscribe({
        next: (response) => {
          this.movements.set(response.content);
          this.movePage.set(response.page);
          this.moveTotalPages.set(response.totalPages);
          this.moveTotal.set(response.totalElements);
          this.loadingMovements.set(false);
        },
        error: () => this.loadingMovements.set(false)
      });
  }

  openOperation(operation: StockOperation): void {
    const ref = this.dialog.open(StockOperationDialogComponent, {
      width: '620px',
      maxWidth: '96vw',
      data: operation
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.notification.success('Opération enregistrée.');
        this.refreshAll();
      }
    });
  }

  movementLabel(type: MovementType): string {
    return this.service.movementLabel(type);
  }

  movementDirection(movement: StockMovement): string {
    if (movement.type === 'IN') {
      return movement.destinationWarehouseName ?? '—';
    }
    if (movement.type === 'OUT') {
      return movement.sourceWarehouseName ?? '—';
    }
    if (movement.type === 'TRANSFER') {
      return `${movement.sourceWarehouseName ?? '—'} → ${movement.destinationWarehouseName ?? '—'}`;
    }
    return movement.destinationWarehouseName ?? movement.sourceWarehouseName ?? '—';
  }

  private refreshAll(): void {
    this.loadStocks();
    this.loadLowStock(0);
    this.loadMovements(0);
  }
}

@Component({
  selector: 'app-stock-operation-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>{{ title }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <div class="dialog-form">
          <mat-form-field appearance="outline">
            <mat-label>Produit</mat-label>
            <mat-select formControlName="productId">
              @for (product of products; track product.id) {
                <mat-option [value]="product.id">{{ product.reference }} — {{ product.name }}</mat-option>
              }
            </mat-select>
            @if (form.controls.productId.hasError('required')) {
              <mat-error>Le produit est obligatoire.</mat-error>
            }
          </mat-form-field>

          @if (operation === 'transfer') {
            <div class="form-grid">
              <mat-form-field appearance="outline">
                <mat-label>Entrepôt source</mat-label>
                <mat-select formControlName="warehouseId">
                  @for (warehouse of warehouses; track warehouse.id) {
                    <mat-option [value]="warehouse.id">{{ warehouse.name }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Entrepôt destination</mat-label>
                <mat-select formControlName="destinationWarehouseId">
                  @for (warehouse of warehouses; track warehouse.id) {
                    <mat-option [value]="warehouse.id">{{ warehouse.name }}</mat-option>
                  }
                </mat-select>
                @if (form.controls.destinationWarehouseId.hasError('required')) {
                  <mat-error>L'entrepôt destination est obligatoire.</mat-error>
                } @else if (form.controls.destinationWarehouseId.hasError('same')) {
                  <mat-error>Les entrepôts doivent être différents.</mat-error>
                }
              </mat-form-field>
            </div>
          } @else {
            <mat-form-field appearance="outline">
              <mat-label>Entrepôt</mat-label>
              <mat-select formControlName="warehouseId">
                @for (warehouse of warehouses; track warehouse.id) {
                  <mat-option [value]="warehouse.id">{{ warehouse.name }}</mat-option>
                }
              </mat-select>
              @if (form.controls.warehouseId.hasError('required')) {
                <mat-error>L'entrepôt est obligatoire.</mat-error>
              }
            </mat-form-field>
          }

          @if (operation === 'adjustment') {
            <mat-form-field appearance="outline">
              <mat-label>Nouvelle quantité physique</mat-label>
              <input matInput type="number" min="0" step="1" formControlName="newQuantity" />
            </mat-form-field>
          } @else {
            <mat-form-field appearance="outline">
              <mat-label>Quantité</mat-label>
              <input matInput type="number" min="1" step="1" formControlName="quantity" />
              @if (form.controls.quantity.hasError('min')) {
                <mat-error>La quantité doit être au minimum de 1.</mat-error>
              }
            </mat-form-field>
          }

          <mat-form-field appearance="outline">
            <mat-label>Motif {{ operation === 'adjustment' ? '' : '(optionnel)' }}</mat-label>
            <textarea matInput rows="2" formControlName="reason" maxlength="250"></textarea>
            @if (form.controls.reason.hasError('required')) {
              <mat-error>Le motif est obligatoire pour un ajustement.</mat-error>
            }
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Annuler</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="saving">Valider</button>
      </mat-dialog-actions>
    </form>
  `
})
export class StockOperationDialogComponent {
  private readonly service = inject(StockService);
  private readonly fb = inject(FormBuilder);
  readonly operation: StockOperation;
  saving = false;
  products: Product[] = [];
  warehouses: Warehouse[] = [];

  readonly form = this.fb.nonNullable.group({
    productId: [null as number | null, [Validators.required]],
    warehouseId: [null as number | null, [Validators.required]],
    destinationWarehouseId: [null as number | null],
    quantity: [1, [Validators.required, Validators.min(1)]],
    newQuantity: [0, [Validators.required, Validators.min(0)]],
    reason: ['', [Validators.maxLength(250)]]
  });

  constructor(
    private readonly dialogRef: MatDialogRef<StockOperationDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) operation: StockOperation
  ) {
    this.operation = operation;
    if (operation === 'adjustment') {
      this.form.controls.reason.setValidators([Validators.required, Validators.maxLength(250)]);
      this.form.controls.reason.updateValueAndValidity();
    }
    if (operation === 'transfer') {
      this.form.controls.destinationWarehouseId.setValidators([Validators.required]);
      this.form.controls.destinationWarehouseId.updateValueAndValidity();
    }
    forkJoin({ products: inject(ProductService).all(), warehouses: inject(WarehouseService).all() }).subscribe({
      next: ({ products, warehouses }) => {
        this.products = products;
        this.warehouses = warehouses;
      }
    });
  }

  get title(): string {
    const titles: Record<StockOperation, string> = {
      entry: 'Entrée de stock',
      exit: 'Sortie de stock',
      transfer: 'Transfert entre entrepôts',
      adjustment: 'Ajustement de stock'
    };
    return titles[this.operation];
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.operation === 'transfer' && this.form.controls.warehouseId.value === this.form.controls.destinationWarehouseId.value) {
      this.form.controls.destinationWarehouseId.setErrors({ same: true });
      return;
    }
    this.form.controls.destinationWarehouseId.updateValueAndValidity({ onlySelf: true });
    this.saving = true;
    const value = this.form.getRawValue();
    const reason = value.reason || undefined;
    let request;
    if (this.operation === 'entry') {
      request = this.service.entry({ productId: value.productId, warehouseId: value.warehouseId, quantity: value.quantity, reason });
    } else if (this.operation === 'exit') {
      request = this.service.exit({ productId: value.productId, warehouseId: value.warehouseId, quantity: value.quantity, reason });
    } else if (this.operation === 'transfer') {
      request = this.service.transfer({
        productId: value.productId,
        sourceWarehouseId: value.warehouseId,
        destinationWarehouseId: value.destinationWarehouseId,
        quantity: value.quantity,
        reason
      });
    } else {
      request = this.service.adjustment({
        productId: value.productId,
        warehouseId: value.warehouseId,
        newQuantity: value.newQuantity,
        reason: value.reason
      });
    }
    request.subscribe({
      next: () => this.dialogRef.close(true),
      error: () => (this.saving = false)
    });
  }
}
