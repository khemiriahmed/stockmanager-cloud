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
import { debounceTime, distinctUntilChanged, forkJoin } from 'rxjs';

import { Category, Product, Supplier } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { CategoryService } from '../../core/services/category.service';
import { NotificationService } from '../../core/services/notification.service';
import { ProductService } from '../../core/services/product.service';
import { SupplierService } from '../../core/services/supplier.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { PagerComponent } from '../../shared/pager.component';

@Component({
  selector: 'app-products',
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
    PageHeaderComponent,
    PagerComponent
  ],
  templateUrl: './products.component.html',
  styleUrl: './products.component.scss'
})
export class ProductsComponent implements OnInit {
  private readonly service = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly supplierService = inject(SupplierService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);

  readonly search = new FormControl('', { nonNullable: true });
  readonly categoryFilter = new FormControl<number | null>(null);
  readonly statusFilter = new FormControl('', { nonNullable: true });
  readonly items = signal<Product[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);

  ngOnInit(): void {
    this.categoryService.all().subscribe((categories) => this.categories.set(categories));
    this.search.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => this.load(0));
    this.categoryFilter.valueChanges.subscribe(() => this.load(0));
    this.statusFilter.valueChanges.subscribe(() => this.load(0));
    this.load(0);
  }

  load(page = this.page()): void {
    this.loading.set(true);
    const status = this.statusFilter.value;
    this.service
      .list({
        page,
        size: 10,
        sort: 'name,asc',
        search: this.search.value,
        categoryId: this.categoryFilter.value ?? undefined,
        active: status === '' ? undefined : status === 'true'
      })
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

  openDialog(product?: Product): void {
    const ref = this.dialog.open(ProductFormDialogComponent, {
      width: '760px',
      maxWidth: '96vw',
      data: product ?? null
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.notification.success(product ? 'Produit mis à jour.' : 'Produit créé.');
        this.load(product ? this.page() : 0);
      }
    });
  }

  toggle(product: Product): void {
    this.service.setStatus(product.id, !product.active).subscribe(() => {
      this.notification.success(product.active ? 'Produit désactivé.' : 'Produit activé.');
      this.load();
    });
  }

  confirmDelete(product: Product): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '440px',
        data: {
          title: 'Désactiver le produit',
          message: `Le produit « ${product.name} » ne sera plus disponible à la vente. Continuer ?`,
          confirmLabel: 'Désactiver',
          danger: true
        }
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.service.deactivate(product.id).subscribe(() => {
            this.notification.success('Produit désactivé.');
            this.load();
          });
        }
      });
  }
}

@Component({
  selector: 'app-product-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data ? 'Modifier le produit' : 'Nouveau produit' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <div class="dialog-form">
          <div class="form-grid">
            <mat-form-field appearance="outline">
              <mat-label>Référence</mat-label>
              <input matInput formControlName="reference" maxlength="60" />
              @if (form.controls.reference.hasError('required')) {
                <mat-error>La référence est obligatoire.</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Nom du produit</mat-label>
              <input matInput formControlName="name" maxlength="180" />
              @if (form.controls.name.hasError('required')) {
                <mat-error>Le nom est obligatoire.</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Catégorie</mat-label>
              <mat-select formControlName="categoryId">
                <mat-option [value]="null">Sans catégorie</mat-option>
                @for (category of categories; track category.id) {
                  <mat-option [value]="category.id">{{ category.name }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Fournisseur</mat-label>
              <mat-select formControlName="supplierId">
                <mat-option [value]="null">Sans fournisseur</mat-option>
                @for (supplier of suppliers; track supplier.id) {
                  <mat-option [value]="supplier.id">{{ supplier.name }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Prix d'achat (€)</mat-label>
              <input matInput type="number" min="0" step="0.01" formControlName="purchasePrice" />
              @if (form.controls.purchasePrice.invalid) {
                <mat-error>Prix invalide.</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Prix de vente (€)</mat-label>
              <input matInput type="number" min="0" step="0.01" formControlName="sellingPrice" />
              @if (form.controls.sellingPrice.invalid) {
                <mat-error>Prix invalide.</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Seuil d'alerte</mat-label>
              <input matInput type="number" min="0" step="1" formControlName="alertThreshold" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>URL de l'image</mat-label>
              <input matInput formControlName="imageUrl" maxlength="500" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="full">
              <mat-label>Description</mat-label>
              <textarea matInput rows="3" formControlName="description" maxlength="5000"></textarea>
            </mat-form-field>
          </div>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Annuler</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="saving || loadingOptions">
          {{ data ? 'Enregistrer' : 'Créer' }}
        </button>
      </mat-dialog-actions>
    </form>
  `
})
export class ProductFormDialogComponent {
  private readonly service = inject(ProductService);
  private readonly fb = inject(FormBuilder);
  saving = false;
  loadingOptions = true;
  categories: Category[] = [];
  suppliers: Supplier[] = [];

  readonly form = this.fb.nonNullable.group({
    reference: ['', [Validators.required, Validators.maxLength(60)]],
    name: ['', [Validators.required, Validators.maxLength(180)]],
    description: ['', [Validators.maxLength(5000)]],
    purchasePrice: [0, [Validators.required, Validators.min(0)]],
    sellingPrice: [0, [Validators.required, Validators.min(0)]],
    alertThreshold: [0, [Validators.min(0)]],
    imageUrl: ['', [Validators.maxLength(500)]],
    categoryId: [null as number | null],
    supplierId: [null as number | null]
  });

  constructor(
    private readonly dialogRef: MatDialogRef<ProductFormDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public readonly data: Product | null
  ) {
    if (data) {
      this.form.patchValue({
        reference: data.reference,
        name: data.name,
        description: data.description ?? '',
        purchasePrice: Number(data.purchasePrice),
        sellingPrice: Number(data.sellingPrice),
        alertThreshold: data.alertThreshold,
        imageUrl: data.imageUrl ?? '',
        categoryId: data.categoryId ?? null,
        supplierId: data.supplierId ?? null
      });
    }
    forkJoin({ categories: inject(CategoryService).all(), suppliers: inject(SupplierService).all() }).subscribe({
      next: ({ categories, suppliers }) => {
        this.categories = categories;
        this.suppliers = suppliers;
        this.loadingOptions = false;
      },
      error: () => (this.loadingOptions = false)
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const body = this.form.getRawValue();
    const request = this.data ? this.service.update(this.data.id, body) : this.service.create(body);
    request.subscribe({
      next: () => this.dialogRef.close(true),
      error: () => (this.saving = false)
    });
  }
}
