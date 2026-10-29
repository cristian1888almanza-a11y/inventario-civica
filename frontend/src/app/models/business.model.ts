export type BusinessModuleKey = 'dashboard' | 'assets' | 'asset-management' | 'depreciation' | 'admin-config' | 'quotes' | 'purchases' | 'cash' | 'sales' | 'reservations' | 'inventory' | 'receipts' | 'report' | 'users' | 'api';

export interface AssetStatusRequest {
  condition: string;
  reason: string;
}

export interface AssetOptions {
  companyId: number;
  companyName: string;
  sites: {
    id: number;
    name: string;
    department: string | null;
    city: string | null;
    areas: { id: number; name: string }[];
  }[];
  responsibles: { id: number; fullName: string }[];
  categories: { id: number; name: string }[];
}

export interface AssetCategory {
  id: number;
  name: string;
  usefulLifeYears: number;
  annualDepreciationRate: number;
  depreciationMethod: string;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AssetRecord {
  id: number;
  inventoryCode: string;
  assetType: string;
  categoryId?: number | null;
  categoryName?: string | null;
  name: string;
  serialNumber?: string | null;
  brand?: string | null;
  model?: string | null;
  material?: string | null;
  color?: string | null;
  dimensions?: string | null;
  itemDetails?: string | null;
  location: string;
  department?: string | null;
  city?: string | null;
  area: string;
  condition: string;
  responsible?: string | null;
  processor?: string | null;
  ram?: string | null;
  storage?: string | null;
  operatingSystem?: string | null;
  purchaseDate?: string | null;
  purchaseValue?: number | null;
  currency?: string | null;
  warrantyUntil?: string | null;
  vendor?: string | null;
  purchaseReference?: string | null;
  observations?: string | null;
  startUseDate?: string | null;
  residualValue?: number | null;
  usefulLifeYears?: number | null;
  accumulatedDepreciation?: number | null;
  bookValue?: number | null;
  assetStatus?: string | null;
  companyId?: number | null;
  siteId?: number | null;
  areaId?: number | null;
  responsibleId?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface AssetRequest {
  assetType: string;
  categoryId: number;
  name: string;
  serialNumber: string;
  brand: string;
  model: string;
  material: string;
  color: string;
  dimensions: string;
  itemDetails: string;
  companyId: number;
  siteId: number;
  areaId: number;
  responsibleId: number | null;
  location: string;
  area: string;
  condition: string;
  responsible: string;
  processor: string;
  ram: string;
  storage: string;
  operatingSystem: string;
  purchaseDate: string | null;
  purchaseValue: number | null;
  currency: string;
  warrantyUntil: string | null;
  vendor: string;
  purchaseReference: string;
  observations: string;
  startUseDate: string | null;
  residualValue: number | null;
  usefulLifeYears: number | null;
}

export interface AssetCategoryRequest {
  name: string;
  usefulLifeYears: number;
  annualDepreciationRate: number;
  depreciationMethod: string;
  description: string;
}

export interface AssetSiteRequest {
  name: string;
  department: string;
  city: string;
}

export interface DepreciationCalculateRequest {
  assetId: number;
  depreciationDate: string;
  processedBy: string;
}

export interface DepreciationBatchRequest {
  depreciationDate: string;
  processedBy: string;
}

export interface DepreciationRecord {
  id: number;
  assetId: number;
  assetInventoryCode: string;
  assetName: string;
  depreciationDate: string;
  periodYear: number;
  periodMonth: number;
  monthlyDepreciation: number;
  accumulatedDepreciation: number;
  bookValue: number;
  processedBy?: string | null;
  isBatchProcess: boolean;
  batchReference?: string | null;
  createdAt: string;
}

export interface DepreciationReport {
  periodYear: number;
  periodMonth: number;
  totalAssets: number;
  totalMonthlyDepreciation: number;
  totalAccumulatedDepreciation: number;
  totalBookValue: number;
  records: DepreciationRecord[];
}

export interface AssetDepreciationValuation {
  id: number;
  inventoryCode: string;
  name: string;
  categoryName?: string | null;
  purchaseDate?: string | null;
  startUseDate?: string | null;
  purchaseValue?: number | null;
  usefulLifeYears?: number | null;
  annualDepreciationRate?: number | null;
  monthlyDepreciation: number;
  accumulatedDepreciation: number;
  bookValue?: number | null;
  condition: string;
  responsible?: string | null;
  location: string;
  department?: string | null;
  city?: string | null;
  assetStatus?: string | null;
  valuationDate: string;
}

export interface BusinessLineRequest {
  productId: number;
  quantity: number;
  unitCost?: number;
}

export interface BusinessLine {
  productId: number;
  productName: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface TradeDocument {
  id: number;
  number: string;
  receiptNumber?: string;
  customerName?: string;
  supplierName?: string;
  status: string;
  items: BusinessLine[];
  total: number;
  deposit?: number;
  createdAt: string;
  paymentMethod?: string;
}

export interface CashRegister {
  id: number;
  status: string;
  openingAmount: number;
  cashIn: number;
  cashOut: number;
  openedAt: string;
  closedAt?: string;
}

export interface CashMovement {
  id: number;
  type: string;
  description: string;
  amount: number;
  createdBy: string;
  createdAt: string;
}

export interface InventoryMovement {
  id: number;
  productId: number;
  productName: string;
  type: string;
  quantity: number;
  previousQuantity?: number | null;
  resultingQuantity?: number | null;
  reason: string;
  createdBy: string;
  createdAt: string;
}

export interface Warehouse {
  id: number;
  code: string;
  name: string;
  address?: string | null;
  active: boolean;
}

export interface ProductStock {
  productId: number;
  sku: string;
  productName: string;
  warehouseId: number;
  warehouseCode: string;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
}

export interface Sale extends TradeDocument {
  receiptNumber: string;
  paymentMethod: string;
}

export interface Receipt extends TradeDocument {
  paymentMethod: string;
}

export interface BusinessSettings {
  businessName: string;
  currency: string;
}
