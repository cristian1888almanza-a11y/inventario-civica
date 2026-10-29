import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { InventorySummary, Product, ProductRequest } from '../../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);
  private api = `${window.location.protocol}//${window.location.hostname}:8080/products`;

  getAll(): Observable<Product[]> {
    return this.http.get<Product[]>(this.api);
  }

  getSummary(): Observable<InventorySummary> {
    return this.http.get<InventorySummary>(`${this.api}/summary`);
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.api, request);
  }

  update(id: number, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.api}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  downloadReport(): Observable<Blob> {
    return this.http.get(`${this.api}/report`, { responseType: 'blob' });
  }

  downloadExcel(): Observable<Blob> {
    return this.http.get(`${this.api}/export/excel`, { responseType: 'blob' });
  }
}
