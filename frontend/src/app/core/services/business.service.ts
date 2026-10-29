import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { map, Observable, throwError } from 'rxjs';
import {
  BusinessLineRequest,
  BusinessSettings,
  AssetRecord,
  AssetOptions,
  AssetRequest,
  AssetSiteRequest,
  AssetStatusRequest,
  CashMovement,
  CashRegister,
  InventoryMovement,
  Receipt,
  Sale,
  TradeDocument,
  AssetCategory,
  AssetCategoryRequest,
  AssetDepreciationValuation,
  DepreciationCalculateRequest,
  DepreciationBatchRequest,
  DepreciationRecord,
  DepreciationReport,
  Warehouse,
  ProductStock
} from '../../models/business.model';

export type ApiHttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

interface OperationDto {
  id: number;
  type: string;
  status: string;
  customer: string | null;
  description: string | null;
  total: number;
  paidTotal: number;
  receiptNumber: string | null;
  createdAt: string;
  items: { productId: number; sku: string; name: string; quantity: number; unitPrice: number; lineTotal: number }[];
}

interface CashDto {
  id: number;
  status: string;
  openingAmount: number;
  balance: number;
  closingAmount: number | null;
  openedAt: string;
  closedAt: string | null;
}

interface CashMovementDto {
  id: number;
  direction: string;
  amount: number;
  description: string;
  createdBy: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class BusinessService {
  private http = inject(HttpClient);
  private api = `${window.location.protocol}//${window.location.hostname}:8080`;

  // Asset endpoints
  getAssets(): Observable<AssetRecord[]> {
    return this.http.get<AssetRecord[]>(`${this.api}/assets`);
  }
  getAssetOptions(): Observable<AssetOptions> {
    return this.http.get<AssetOptions>(`${this.api}/assets/options`);
  }
  createAssetSite(request: AssetSiteRequest): Observable<AssetOptions['sites'][number]> {
    return this.http.post<AssetOptions['sites'][number]>(`${this.api}/assets/sites`, request);
  }
  createAssetArea(siteId: number, name: string): Observable<AssetOptions['sites'][number]['areas'][number]> {
    return this.http.post<AssetOptions['sites'][number]['areas'][number]>(`${this.api}/assets/sites/${siteId}/areas`, { name });
  }
  createAsset(request: AssetRequest): Observable<AssetRecord> {
    return this.http.post<AssetRecord>(`${this.api}/assets`, request);
  }
  createAssetsBatch(requests: AssetRequest[]): Observable<AssetRecord[]> {
    return this.http.post<AssetRecord[]>(`${this.api}/assets/batch`, { assets: requests });
  }
  updateAsset(id: number, request: AssetRequest): Observable<AssetRecord> {
    return this.http.put<AssetRecord>(`${this.api}/assets/${id}`, request);
  }
  updateAssetStatus(id: number, request: AssetStatusRequest): Observable<AssetRecord> {
    return this.http.patch<AssetRecord>(`${this.api}/assets/${id}/status`, request);
  }

  // Asset Category endpoints
  getAssetCategories(): Observable<AssetCategory[]> {
    return this.http.get<AssetCategory[]>(`${this.api}/asset-categories`);
  }
  getAssetCategory(id: number): Observable<AssetCategory> {
    return this.http.get<AssetCategory>(`${this.api}/asset-categories/${id}`);
  }
  createAssetCategory(request: AssetCategoryRequest): Observable<AssetCategory> {
    return this.http.post<AssetCategory>(`${this.api}/asset-categories`, request);
  }
  updateAssetCategory(id: number, request: AssetCategoryRequest): Observable<AssetCategory> {
    return this.http.put<AssetCategory>(`${this.api}/asset-categories/${id}`, request);
  }
  deleteAssetCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/asset-categories/${id}`);
  }

  // Depreciation endpoints
  calculateDepreciation(request: DepreciationCalculateRequest): Observable<DepreciationRecord> {
    return this.http.post<DepreciationRecord>(`${this.api}/depreciation/calculate`, request);
  }
  depreciateAll(request: DepreciationBatchRequest): Observable<DepreciationReport> {
    return this.http.post<DepreciationReport>(`${this.api}/depreciation/batch`, request);
  }
  getAssetDepreciationHistory(assetId: number): Observable<DepreciationRecord[]> {
    return this.http.get<DepreciationRecord[]>(`${this.api}/depreciation/asset/${assetId}/history`);
  }
  getDepreciationReport(year: number, month: number): Observable<DepreciationReport> {
    return this.http.get<DepreciationReport>(`${this.api}/depreciation/report/${year}/${month}`);
  }
  calculateAssetDepreciation(assetId: number, request: DepreciationCalculateRequest): Observable<DepreciationRecord> {
    return this.http.post<DepreciationRecord>(`${this.api}/assets/${assetId}/depreciation/calculate`, request);
  }
  getAssetDepreciationHistoryByAsset(assetId: number): Observable<DepreciationRecord[]> {
    return this.http.get<DepreciationRecord[]>(`${this.api}/assets/${assetId}/depreciation/history`);
  }
  getAssetDepreciationValuation(asOf: string): Observable<AssetDepreciationValuation[]> {
    return this.http.get<AssetDepreciationValuation[]>(`${this.api}/assets/depreciation/valuation`, { params: { asOf } });
  }
  downloadDepreciationExcel(): Observable<Blob> {
    return this.http.get(`${this.api}/assets/export/excel`, { responseType: 'blob' });
  }

  executeApiRequest(method: ApiHttpMethod, path: string, body?: unknown): Observable<HttpResponse<string>> {
    const apiBase = new URL(this.api);
    if (!path.startsWith('/') || path.startsWith('//') || path.includes('\\')) {
      return throwError(() => new Error('La ruta debe ser relativa a la API local y comenzar con /'));
    }
    const url = new URL(path, apiBase);
    if (url.origin !== apiBase.origin) {
      return throwError(() => new Error('Solo se permiten solicitudes a la API local'));
    }
    return this.http.request(method, url.toString(), {
      body: method === 'GET' ? undefined : body,
      observe: 'response',
      responseType: 'text'
    });
  }

  getQuotes(): Observable<TradeDocument[]> {
    return this.http.get<OperationDto[]>(`${this.api}/quotes`).pipe(map(records => records.map(record => this.toDocument(record))));
  }
  createQuote(request: { customerName: string; items: BusinessLineRequest[] }): Observable<TradeDocument> {
    return this.http.post<OperationDto>(`${this.api}/quotes`, {
      customer: request.customerName,
      items: request.items.map(({ productId, quantity }) => ({ productId, quantity }))
    }).pipe(map(record => this.toDocument(record)));
  }

  getPurchases(): Observable<TradeDocument[]> {
    return this.http.get<OperationDto[]>(`${this.api}/purchases`).pipe(map(records => records.map(record => this.toDocument(record))));
  }
  createPurchase(request: { supplierName: string; items: (BusinessLineRequest & { unitCost: number })[] }): Observable<TradeDocument> {
    return this.http.post<OperationDto>(`${this.api}/purchases`, {
      supplier: request.supplierName,
      items: request.items.map(({ productId, quantity, unitCost }) => ({ productId, quantity, unitCost }))
    }).pipe(map(record => this.toDocument(record)));
  }
  receivePurchase(id: number): Observable<TradeDocument> {
    return this.http.post<OperationDto>(`${this.api}/purchases/${id}/receive`, {}).pipe(map(record => this.toDocument(record)));
  }

  getActiveRegister(): Observable<CashRegister | null> {
    return this.http.get<CashDto | null>(`${this.api}/cash/active`).pipe(map(register => register ? this.toCashRegister(register) : null));
  }
  openRegister(request: { openingAmount: number }): Observable<CashRegister> {
    return this.http.post<CashDto>(`${this.api}/cash/open`, request).pipe(map(register => this.toCashRegister(register)));
  }
  closeRegister(): Observable<CashRegister> {
    return this.http.post<CashDto>(`${this.api}/cash/close`, {}).pipe(map(register => this.toCashRegister(register)));
  }
  getCashMovements(): Observable<CashMovement[]> {
    return this.http.get<CashMovementDto[]>(`${this.api}/cash/movements`).pipe(map(movements => movements.map(movement => ({
      id: movement.id, type: movement.direction, amount: movement.amount, description: movement.description,
      createdBy: movement.createdBy, createdAt: movement.createdAt
    }))));
  }
  createCashMovement(direction: 'IN' | 'OUT', request: { description: string; amount: number }): Observable<CashRegister> {
    return this.http.post<CashDto>(`${this.api}/cash/movements/${direction.toLocaleLowerCase()}`, request)
      .pipe(map(register => this.toCashRegister(register)));
  }

  getSales(): Observable<Sale[]> {
    return this.http.get<OperationDto[]>(`${this.api}/sales`).pipe(map(records => records.map(record => this.toSale(record))));
  }
  createSale(request: { paymentMethod: string; items: BusinessLineRequest[] }): Observable<Sale> {
    return this.http.post<OperationDto>(`${this.api}/sales`, {
      paymentMethod: request.paymentMethod,
      items: request.items.map(({ productId, quantity }) => ({ productId, quantity }))
    }).pipe(map(record => this.toSale(record)));
  }

  getReservations(): Observable<TradeDocument[]> {
    return this.http.get<OperationDto[]>(`${this.api}/reservations`).pipe(map(records => records.map(record => this.toDocument(record))));
  }
  createReservation(request: { customerName: string; items: BusinessLineRequest[] }): Observable<TradeDocument> {
    return this.http.post<OperationDto>(`${this.api}/reservations`, {
      customer: request.customerName,
      items: request.items.map(({ productId, quantity }) => ({ productId, quantity }))
    }).pipe(map(record => this.toDocument(record)));
  }
  cancelReservation(id: number): Observable<TradeDocument> {
    return this.http.patch<OperationDto>(`${this.api}/reservations/${id}/status`, { status: 'CANCELLED' })
      .pipe(map(record => this.toDocument(record)));
  }
  fulfillReservation(id: number): Observable<TradeDocument> {
    return this.http.patch<OperationDto>(`${this.api}/reservations/${id}/status`, { status: 'FULFILLED' })
      .pipe(map(record => this.toDocument(record)));
  }

  getInventoryMovements(): Observable<InventoryMovement[]> { return this.http.get<InventoryMovement[]>(`${this.api}/inventory/movements`); }
  createInventoryMovement(request: { productId: number; type: 'IN' | 'OUT' | 'ADJUSTMENT'; quantity: number; reason: string }): Observable<InventoryMovement> {
    return this.http.post<InventoryMovement>(`${this.api}/inventory/movements`, request);
  }

  getWarehouses(): Observable<Warehouse[]> { return this.http.get<Warehouse[]>(`${this.api}/warehouses`); }
  getWarehouseStock(warehouseId: number): Observable<ProductStock[]> {
    return this.http.get<ProductStock[]>(`${this.api}/warehouses/${warehouseId}/stock`);
  }
  createWarehouse(request: { code: string; name: string; address?: string }): Observable<Warehouse> {
    return this.http.post<Warehouse>(`${this.api}/warehouses`, request);
  }

  getReceipts(): Observable<Receipt[]> {
    return this.http.get<OperationDto[]>(`${this.api}/receipts`).pipe(map(records => records.map(record => ({
      ...this.toDocument(record), paymentMethod: record.description ?? ''
    }))));
  }
  getReceiptPdf(number: string): Observable<Blob> { return this.http.get(`${this.api}/receipts/${encodeURIComponent(number)}/pdf`, { responseType: 'blob' }); }

  getSettings(): Observable<BusinessSettings> { return this.http.get<BusinessSettings>(`${this.api}/settings`); }
  saveSettings(request: BusinessSettings): Observable<BusinessSettings> { return this.http.put<BusinessSettings>(`${this.api}/settings`, request); }

  private toSale(record: OperationDto): Sale {
    return {
      ...this.toDocument(record),
      receiptNumber: record.receiptNumber ?? String(record.id),
      paymentMethod: record.description ?? ''
    };
  }

  private toDocument(record: OperationDto): TradeDocument {
    const statuses: Record<string, string> = {
      DRAFT: 'OPEN',
      ACCEPTED: 'APPROVED',
      OPEN: 'PENDING',
      RESERVED: 'ACTIVE'
    };
    return {
      id: record.id,
      number: record.receiptNumber ?? `OP-${record.id}`,
      customerName: record.customer ?? undefined,
      supplierName: record.type === 'PURCHASE' ? record.customer ?? undefined : undefined,
      status: statuses[record.status] ?? record.status,
      items: record.items.map(item => ({
        productId: item.productId, productName: item.name, sku: item.sku, quantity: item.quantity,
        unitPrice: item.unitPrice, subtotal: item.lineTotal
      })),
      total: record.total,
      deposit: record.paidTotal,
      createdAt: record.createdAt,
      paymentMethod: record.type === 'SALE' ? record.description ?? undefined : undefined,
      receiptNumber: record.receiptNumber ?? undefined
    };
  }

  private toCashRegister(register: CashDto): CashRegister {
    return {
      id: register.id, status: register.status, openingAmount: register.openingAmount,
      cashIn: 0, cashOut: 0, openedAt: register.openedAt, closedAt: register.closedAt ?? undefined
    };
  }
}
