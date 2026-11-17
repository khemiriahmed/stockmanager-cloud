import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from './api.service';
import { DashboardOverview } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = inject(ApiService);

  overview(): Observable<DashboardOverview> {
    return this.api.get<DashboardOverview>('/dashboard/overview');
  }
}
