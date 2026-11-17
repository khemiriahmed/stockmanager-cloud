import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { Warehouse } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { WarehouseService } from '../../core/services/warehouse.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { PagerComponent } from '../../shared/pager.component';

@Component({
  selector: 'app-warehouses',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
    PagerComponent
  ],
  templateUrl: './warehouses.component.html',
  styleUrl: '../categories/categories.component.scss'
})
export class WarehousesComponent implements OnInit {
  private readonly service = inject(WarehouseService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);

  readonly search = new FormControl('', { nonNullable: true });
  readonly items = signal<Warehouse[]>([]);
  readonly loading = signal(true);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);

  ngOnInit(): void {
    this.search.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => this.load(0));
    this.load(0);
  }

  load(page = this.page()): void {
    this.loading.set(true);
    this.service.list({ page, size: 10, sort: 'name,asc', search: this.search.value }).subscribe({
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

  openDialog(warehouse?: Warehouse): void {
    const ref = this.dialog.open(WarehouseFormDialogComponent, {
      width: '580px',
      maxWidth: '95vw',
      data: warehouse ?? null
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.notification.success(warehouse ? 'Entrepôt mis à jour.' : 'Entrepôt créé.');
        this.load(warehouse ? this.page() : 0);
      }
    });
  }

  toggle(warehouse: Warehouse): void {
    this.service.setStatus(warehouse.id, !warehouse.active).subscribe(() => {
      this.notification.success(warehouse.active ? 'Entrepôt désactivé.' : 'Entrepôt activé.');
      this.load();
    });
  }

  confirmDelete(warehouse: Warehouse): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '440px',
        data: {
          title: "Désactiver l'entrepôt",
          message: `L'entrepôt « ${warehouse.name} » sera désactivé. Continuer ?`,
          confirmLabel: 'Désactiver',
          danger: true
        }
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.service.deactivate(warehouse.id).subscribe(() => {
            this.notification.success('Entrepôt désactivé.');
            this.load();
          });
        }
      });
  }
}

@Component({
  selector: 'app-warehouse-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data ? "Modifier l'entrepôt" : 'Nouvel entrepôt' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <div class="dialog-form">
          <mat-form-field appearance="outline">
            <mat-label>Nom</mat-label>
            <input matInput formControlName="name" maxlength="120" />
            @if (form.controls.name.hasError('required')) {
              <mat-error>Le nom est obligatoire.</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Responsable</mat-label>
            <input matInput formControlName="manager" maxlength="160" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Adresse</mat-label>
            <textarea matInput rows="2" formControlName="address" maxlength="300"></textarea>
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Annuler</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="saving">
          {{ data ? 'Enregistrer' : 'Créer' }}
        </button>
      </mat-dialog-actions>
    </form>
  `
})
export class WarehouseFormDialogComponent {
  private readonly service = inject(WarehouseService);
  private readonly fb = inject(FormBuilder);
  saving = false;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    manager: ['', [Validators.maxLength(160)]],
    address: ['', [Validators.maxLength(300)]]
  });

  constructor(
    private readonly dialogRef: MatDialogRef<WarehouseFormDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public readonly data: Warehouse | null
  ) {
    if (data) {
      this.form.patchValue({
        name: data.name,
        manager: data.manager ?? '',
        address: data.address ?? ''
      });
    }
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
