import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Account, AccountRequest, LoginRequest } from '../../models/account.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private api = `${window.location.protocol}//${window.location.hostname}:8080`;

  csrf(): Observable<{ token: string }> {
    return this.http.get<{ token: string }>(`${this.api}/auth/csrf`);
  }

  setupStatus(): Observable<{ setupRequired: boolean }> {
    return this.http.get<{ setupRequired: boolean }>(`${this.api}/auth/setup-status`);
  }

  setupAdministrator(request: AccountRequest): Observable<Account> {
    return this.http.post<Account>(`${this.api}/auth/setup`, request);
  }

  login(request: LoginRequest): Observable<Account> {
    return this.http.post<Account>(`${this.api}/auth/login`, request);
  }

  currentAccount(): Observable<Account> {
    return this.http.get<Account>(`${this.api}/auth/me`);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.api}/auth/logout`, {});
  }

  getUsers(): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.api}/users`);
  }

  createUser(request: AccountRequest): Observable<Account> {
    return this.http.post<Account>(`${this.api}/users`, request);
  }

  setUserEnabled(id: number, enabled: boolean): Observable<Account> {
    return this.http.patch<Account>(`${this.api}/users/${id}/status`, null, { params: { enabled } });
  }
}
