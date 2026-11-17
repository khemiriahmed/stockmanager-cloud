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
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';

import { RoleName, User } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { UserService } from '../../core/services/user.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { PageHeaderComponent } from '../../shared/page-header.component';
import { PagerComponent } from '../../shared/pager.component';

@Component({
  selector: 'app-users',
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
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent implements OnInit {
  private readonly service = inject(UserService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);

  readonly search = new FormControl('', { nonNullable: true });
  readonly items = signal<User[]>([]);
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
    this.service.list({ page, size: 10, sort: 'email,asc', search: this.search.value }).subscribe({
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

  openDialog(user?: User): void {
    const ref = this.dialog.open(UserFormDialogComponent, {
      width: '620px',
      maxWidth: '96vw',
      data: user ?? null
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.notification.success(user ? 'Utilisateur mis à jour.' : 'Utilisateur créé.');
        this.load(user ? this.page() : 0);
      }
    });
  }

  toggle(user: User): void {
    const action = user.active ? 'désactiver' : 'activer';
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '450px',
        data: {
          title: `Voulez-vous ${action} cet utilisateur ?`,
          message: `${user.fullName} (${user.email}) ${user.active ? 'ne pourra plus se connecter' : 'pourra à nouveau se connecter'}.`,
          confirmLabel: action === 'désactiver' ? 'Désactiver' : 'Activer',
          danger: user.active
        }
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.service.setStatus(user.id, !user.active).subscribe(() => {
            this.notification.success(`Utilisateur ${user.active ? 'désactivé' : 'activé'}.`);
            this.load();
          });
        }
      });
  }

  roleClass(role: RoleName): string {
    return `role-${role.toLowerCase()}`;
  }
}

@Component({
  selector: 'app-user-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data ? "Modifier l'utilisateur" : 'Nouvel utilisateur' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <div class="dialog-form">
          <div class="form-grid">
            <mat-form-field appearance="outline">
              <mat-label>Prénom</mat-label>
              <input matInput formControlName="firstName" maxlength="80" />
              @if (form.controls.firstName.hasError('required')) {
                <mat-error>Le prénom est obligatoire.</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Nom</mat-label>
              <input matInput formControlName="lastName" maxlength="80" />
              @if (form.controls.lastName.hasError('required')) {
                <mat-error>Le nom est obligatoire.</mat-error>
              }
            </mat-form-field>
          </div>
          <mat-form-field appearance="outline">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" maxlength="160" />
            @if (form.controls.email.hasError('required')) {
              <mat-error>L'email est obligatoire.</mat-error>
            } @else if (form.controls.email.hasError('email')) {
              <mat-error>Format d'email invalide.</mat-error>
            }
          </mat-form-field>
          @if (!data) {
            <mat-form-field appearance="outline">
              <mat-label>Mot de passe</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="new-password" />
              @if (form.controls.password.hasError('minlength')) {
                <mat-error>8 caractères minimum.</mat-error>
              }
            </mat-form-field>
          }
          <mat-form-field appearance="outline">
            <mat-label>Rôles</mat-label>
            <mat-select formControlName="roles" multiple>
              <mat-option value="ADMIN">Administrateur</mat-option>
              <mat-option value="MANAGER">Manager</mat-option>
              <mat-option value="EMPLOYEE">Employé</mat-option>
            </mat-select>
            @if (form.controls.roles.hasError('required')) {
              <mat-error>Au moins un rôle est obligatoire.</mat-error>
            }
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
export class UserFormDialogComponent {
  private readonly service = inject(UserService);
  private readonly fb = inject(FormBuilder);
  saving = false;

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
    roles: [['EMPLOYEE'] as RoleName[], [Validators.required]]
  });

  constructor(
    private readonly dialogRef: MatDialogRef<UserFormDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public readonly data: User | null
  ) {
    if (data) {
      this.form.patchValue({
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        roles: [...data.roles]
      });
      this.form.controls.password.clearValidators();
      this.form.controls.password.updateValueAndValidity();
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const value = this.form.getRawValue();
    const request = this.data
      ? this.service
          .update(this.data.id, { firstName: value.firstName, lastName: value.lastName, email: value.email })
          .pipe(switchMap(() => this.service.updateRoles(this.data!.id, value.roles)))
      : this.service.create({
          firstName: value.firstName,
          lastName: value.lastName,
          email: value.email,
          password: value.password,
          roles: value.roles
        });
    request.subscribe({
      next: () => this.dialogRef.close(true),
      error: () => (this.saving = false)
    });
  }
}
