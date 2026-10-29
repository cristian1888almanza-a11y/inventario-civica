export interface Product {
  id: number;
  sku: string;
  name: string;
  category: string;
  description?: string | null;
  price: number;
  stock: number;
  minStock: number;
  supplier?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  sku: string;
  name: string;
  category: string;
  description: string;
  price: number;
  stock: number;
  minStock: number;
  supplier: string;
}

export interface InventorySummary {
  totalProducts: number;
  totalUnits: number;
  inventoryValue: number;
  lowStock: number;
}
