export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
  [key: string]: unknown;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  path?: string;
  errors?: Record<string, string>;
}

export type RoleName = 'ADMIN' | 'MANAGER' | 'EMPLOYEE';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  active: boolean;
  roles: RoleName[];
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Supplier {
  id: number;
  name: string;
  company?: string;
  email?: string;
  phone?: string;
  address?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Customer {
  id: number;
  name: string;
  company?: string;
  email?: string;
  phone?: string;
  address?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Warehouse {
  id: number;
  name: string;
  address?: string;
  manager?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Product {
  id: number;
  reference: string;
  name: string;
  description?: string;
  purchasePrice: number;
  sellingPrice: number;
  alertThreshold: number;
  imageUrl?: string;
  categoryId?: number;
  categoryName?: string;
  supplierId?: number;
  supplierName?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export type MovementType = 'IN' | 'OUT' | 'TRANSFER' | 'ADJUSTMENT';

export interface Stock {
  id: number;
  productId: number;
  productReference: string;
  productName: string;
  warehouseId: number;
  warehouseName: string;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  alertThreshold: number;
  lowStock: boolean;
}

export interface StockMovement {
  id: number;
  type: MovementType;
  productId: number;
  productReference: string;
  productName: string;
  sourceWarehouseId?: number;
  sourceWarehouseName?: string;
  destinationWarehouseId?: number;
  destinationWarehouseName?: string;
  quantity: number;
  reason?: string;
  createdBy?: string;
  createdAt: string;
}

export type OrderStatus = 'DRAFT' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderSummary {
  id: number;
  reference: string;
  customerId: number;
  customerName: string;
  customerCompany?: string;
  status: OrderStatus;
  totalHt: number;
  vatRate: number;
  totalVat: number;
  totalTtc: number;
  itemCount: number;
  createdBy?: string;
  createdAt: string;
  confirmedAt?: string;
  processingAt?: string;
  shippedAt?: string;
  deliveredAt?: string;
  cancelledAt?: string;
}

export interface OrderItem {
  id: number;
  productId: number;
  productReference: string;
  productName: string;
  warehouseId: number;
  warehouseName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order extends OrderSummary {
  customerEmail?: string;
  customerAddress?: string;
  notes?: string;
  updatedAt: string;
  items: OrderItem[];
}

export interface Invoice {
  id: number;
  number: string;
  orderId: number;
  orderReference: string;
  customerId: number;
  customerName: string;
  customerCompany?: string;
  totalHt: number;
  vatRate: number;
  totalVat: number;
  totalTtc: number;
  issuedAt: string;
  downloadPath: string;
}

export interface SalesPoint {
  period: string;
  sales: number;
  orders: number;
}

export interface DashboardOverview {
  products: number;
  customers: number;
  suppliers: number;
  warehouses: number;
  users: number;
  orders: number;
  stockValue: number;
  lowStockProducts: number;
  outOfStock: number;
  pendingOrders: number;
  ordersByStatus: Record<string, number>;
  sales: SalesPoint[];
  lowStock: Stock[];
  recentOrders: OrderSummary[];
}
