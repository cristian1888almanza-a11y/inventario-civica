import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, finalize, Observable } from 'rxjs';
import { Account, AccountRequest, PermissionCode, UserRole } from '../../models/account.model';
import {
  BusinessLineRequest,
  BusinessModuleKey,
  AssetRecord,
  AssetOptions,
  AssetRequest,
  AssetSiteRequest,
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
  DepreciationReport
} from '../../models/business.model';
import { ApiHttpMethod, BusinessService } from '../../core/services/business.service';
import { InventorySummary, Product, ProductRequest } from '../../models/product.model';
import { AuthService } from '../../core/services/auth.service';
import { ProductService } from '../../core/services/product.service';

@Component({selector:'app-inventory', standalone:true, imports:[CommonModule, FormsModule, ReactiveFormsModule], templateUrl:'./task-manager.component.html'})
export class TaskManagerComponent implements OnInit {
  private fb = inject(FormBuilder);
  private service = inject(ProductService);
  private business = inject(BusinessService);
  private auth = inject(AuthService);
  currentUser: Account | null = null;
  setupRequired = false;
  authBusy = false;
  authError = '';
  savingUser = false;
  usersLoading = false;
  usersSearch = '';
  passwordVisible = false;
  adminMenuExpanded = false;
  filesMenuExpanded = false;
  goodsMenuExpanded = false;
  users: Account[] = [];
  quotes: TradeDocument[] = [];
  purchases: TradeDocument[] = [];
  reservations: TradeDocument[] = [];
  sales: Sale[] = [];
  receipts: Receipt[] = [];
  assets: AssetRecord[] = [];
  assetValuations: AssetDepreciationValuation[] = [];
  assetOptions: AssetOptions | null = null;
  assetCategories: AssetCategory[] = [];
  assetEditingId: number | null = null;
  assetLoading = false;
  valuationLoading = false;
  downloadingDepreciationExcel = false;
  valuationDate = this.localDateString();
  readonly todayDate = this.localDateString();
  purchaseDateFrom = '';
  purchaseDateTo = '';
  depreciationCategoryFilter = '';
  depreciationStatusFilter = '';
  depreciationResponsibleFilter = '';
  depreciationLocationFilter = '';
  showCategoryForm = false;
  savingCategory = false;
  showSiteForm = false;
  savingSite = false;
  showAreaForm = false;
  savingArea = false;
  adminSiteId = 0;
  savingAsset = false;
  assetDistributions: { siteId: number; areaId: number; quantity: number }[] = [];
  entryQuantity = 1;
  inventoryMovements: InventoryMovement[] = [];
  activeRegister: CashRegister | null = null;
  cashMovements: CashMovement[] = [];
  moduleLoading = false;
  savingModule = false;
  showModuleForm = false;
  moduleError = '';
  moduleMessage = '';
  basket: BusinessLineRequest[] = [];
  userError = '';
  userMessage = '';
  apiBusy = false;
  apiError = '';
  apiResponse: { method: ApiHttpMethod; path: string; status: number; statusText: string; body: string; failed: boolean } | null = null;
  readonly apiMethods: ApiHttpMethod[] = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];
  products: Product[] = [];
  summary: InventorySummary = { totalProducts: 0, totalUnits: 0, inventoryValue: 0, lowStock: 0 };
  readonly statusOptions = ['Activo', 'Inactivo', 'En mantenimiento', 'Dañado', 'Reemplazado', 'En revisión', 'Retirado'];
  private readonly defaultCategories: string[] = [];
  loading = false;
  saving = false;
  downloadingReport = false;
  downloadingExcel = false;
  showForm = false;
  assetKind: 'computer' | 'enser' = 'computer';
  private goodsEntryInitialized = false;
  goodsPreview = false;
  editingId: number | null = null;
  activeMenu: BusinessModuleKey = 'dashboard';
  selectedCategory = 'all';
  availabilityFilter: 'all' | 'available' | 'out-of-stock' = 'all';
  searchTerm = '';
  readonly pageSize = 10;
  productPage = 1;
  assetPage = 1;
  retiredAssetPage = 1;
  valuationPage = 1;
  inventoryPage = 1;
  depreciationHistoryPage = 1;
  depreciationReportPage = 1;
  message = '';
  error = '';
  todayLabel = new Intl.DateTimeFormat('es', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());

  // Depreciation properties
  showDepreciationForm = false;
  depreciationLoading = false;
  depreciationError = '';
  depreciationMessage = '';
  depreciationReport: DepreciationReport | null = null;
  assetDepreciationHistory: DepreciationRecord[] = [];
  selectedAssetForDepreciation: AssetRecord | null = null;
  batchDepreciationDate = '';
  batchProcessedBy = '';
  singleAssetDepreciationDate = '';
  singleAssetProcessedBy = '';
  depreciationYear = new Date().getFullYear();
  depreciationMonth = new Date().getMonth() + 1;

  get isCoreModule(): boolean {
    return this.activeMenu === 'dashboard';
  }

  can(permission: PermissionCode): boolean {
    return this.currentUser?.role === 'ADMIN' || this.currentUser?.permissions?.includes(permission) === true;
  }

  get canAdminConfiguration(): boolean {
    return this.can('CONFIG_WRITE') || this.can('CATEGORIAS_WRITE') || this.can('DEPRECIACION_WRITE');
  }

  get currentModuleTitle(): string {
    const titles: Record<BusinessModuleKey, string> = {
      dashboard: 'Inicio',
      assets: 'Ingreso de productos',
      'asset-management': 'Gestión de activos',
      depreciation: 'Consulta de depreciaci\u00f3n',
      'admin-config': 'Configuraci\u00f3n de activos',
      quotes: 'Cotizaciones',
      purchases: 'Compras',
      cash: 'Caja',
      sales: 'Ventas',
      reservations: 'Apartados',
      inventory: 'Inventario',
      receipts: 'Comprobantes',
      report: 'Reporte',
      users: 'Administración',
      api: 'API'
    };
    return titles[this.activeMenu];
  }

  get currentModuleEyebrow(): string {
    return this.activeMenu === 'assets' ? 'ALTA DE INVENTARIO' : this.activeMenu === 'cash' ? 'CONTROL DE CAJA' : this.activeMenu === 'inventory' ? 'CONTROL DE EXISTENCIAS' : 'GESTIÓN COMERCIAL';
  }

  get currentModuleDescription(): string {
    const descriptions: Record<BusinessModuleKey, string> = {
      dashboard: 'Resumen de los movimientos y existencias de tu negocio.',
      assets: 'Registra cada equipo o enser como un bien individual.',
      'asset-management': 'Consulta los bienes registrados y actualiza su estado con el motivo del cambio.',

      depreciation: 'Consulta la depreciaci\u00f3n calculada de los bienes a una fecha determinada.',
      'admin-config': 'Administra sedes, \u00e1reas y categor\u00edas de inventario.',
      quotes: 'Prepara propuestas para tus clientes y conviértelas en operaciones.',
      purchases: 'Registra pedidos a proveedores y recibe mercancía en el almacén.',
      cash: 'Abre tu caja, controla movimientos y realiza el corte de jornada.',
      sales: 'Registra ventas, cobra al cliente, descuenta stock y emite comprobantes.',
      reservations: 'Aparta productos disponibles y gestiona su entrega a clientes.',
      inventory: 'Consulta el historial de movimiento y registra ajustes justificados del inventario.',
      receipts: 'Consulta y descarga los comprobantes PDF de las ventas.',
      report: 'Genera y descarga en PDF el catálogo actual del inventario.',
      users: 'Crea cuentas, asigna perfiles y administra el acceso del equipo.',
      api: 'Consulta y ejecuta solicitudes autenticadas en la API local.'
    };
    return descriptions[this.activeMenu];
  }

  get moduleHasCreate(): boolean {
    return ['quotes', 'purchases', 'sales', 'reservations', 'inventory'].includes(this.activeMenu)
      || (this.activeMenu === 'cash' && !this.activeRegister);
  }

  get moduleCreateLabel(): string {
    const labels: Partial<Record<BusinessModuleKey, string>> = {
      quotes: 'Nueva cotización',
      purchases: 'Nueva compra',
      sales: 'Registrar venta',
      reservations: 'Nuevo apartado',
      inventory: 'Ajustar existencias',
      cash: 'Abrir caja'
    };
    return labels[this.activeMenu] ?? 'Nuevo';
  }

  get salesTotal(): number {
    return this.sales.reduce((sum, sale) => sum + sale.total, 0);
  }

  get salesUnits(): number {
    return this.sales.reduce((sum, sale) => sum + sale.items.reduce((items, item) => items + item.quantity, 0), 0);
  }

  get cashInTotal(): number {
    return this.cashMovements.filter(movement => movement.type === 'IN')
      .reduce((total, movement) => total + movement.amount, 0);
  }

  get cashOutTotal(): number {
    return this.cashMovements.filter(movement => movement.type === 'OUT')
      .reduce((total, movement) => total + movement.amount, 0);
  }

  get filteredUsers(): Account[] {
    const query = this.usersSearch.trim().toLocaleLowerCase('es');
    if (!query) return this.users;
    return this.users.filter(user => `${user.fullName} ${user.username} ${user.role}`.toLocaleLowerCase('es').includes(query));
  }

  get activeUsersCount(): number {
    return this.users.filter(user => user.enabled).length;
  }

  get employeeUsersCount(): number {
    return this.users.filter(user => user.role === 'EMPLOYEE').length;
  }

  authForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.minLength(2), Validators.maxLength(120)]],
    username: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9._-]{3,50}$/)]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]]
  });

  moduleForm = this.fb.nonNullable.group({
    customerName: ['', [Validators.maxLength(120)]],
    supplierName: ['', [Validators.maxLength(120)]],
    productId: [0, [Validators.min(1)]],
    quantity: [1, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]],
    deposit: [0, [Validators.required, Validators.min(0)]],
    openingAmount: [0, [Validators.required, Validators.min(0)]],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    movementType: ['IN' as 'IN' | 'OUT' | 'ADJUSTMENT'],
    paymentMethod: ['CASH', Validators.required],
    description: ['', [Validators.maxLength(250)]],
    reason: ['', [Validators.required, Validators.maxLength(250)]]
  });

  userForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    username: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9._-]{3,50}$/)]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]],
    role: ['EMPLOYEE' as UserRole, Validators.required],
    permissions: this.fb.nonNullable.control<PermissionCode[]>([])
  });

  readonly permissionOptions: Array<{ code: PermissionCode; label: string }> = [
    { code: 'CONFIG_WRITE', label: 'Sedes y áreas' },
    { code: 'CATEGORIAS_WRITE', label: 'Categorías' },
    { code: 'DEPRECIACION_WRITE', label: 'Depreciación' }
  ];
  permissionsOpen = false;

  get selectedPermissionSummary(): string {
    const selected = this.permissionOptions
      .filter(permission => this.userForm.controls.permissions.value.includes(permission.code))
      .map(permission => permission.label);
    return selected.length ? selected.join(', ') : 'Selecciona permisos';
  }

  apiForm = this.fb.nonNullable.group({
    method: ['GET' as ApiHttpMethod, Validators.required],
    path: ['/products', [Validators.required, Validators.pattern(/^\/(?!\/)/)]],
    body: ['']
  });

  assetForm = this.fb.nonNullable.group({
    categoryId: [0, [Validators.required, Validators.min(1)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    serialNumber: ['', Validators.maxLength(120)],
    brand: ['', Validators.maxLength(80)],
    model: ['', Validators.maxLength(80)],
    processor: ['', Validators.maxLength(120)],
    ram: ['', Validators.maxLength(80)],
    storage: ['', Validators.maxLength(120)],
    operatingSystem: ['', Validators.maxLength(120)],
    material: ['', Validators.maxLength(120)],
    color: ['', Validators.maxLength(80)],
    dimensions: ['', Validators.maxLength(120)],
    itemDetails: ['', Validators.maxLength(160)],
    companyId: [0, [Validators.required, Validators.min(1)]],
    siteId: [0, [Validators.required, Validators.min(1)]],
    areaId: [0, [Validators.required, Validators.min(1)]],
    responsibleId: [0],
    location: ['', [Validators.required, Validators.maxLength(160)]],
    area: ['', [Validators.required, Validators.maxLength(100)]],
    condition: ['Activo', [Validators.required, Validators.maxLength(40)]],
    responsible: ['', Validators.maxLength(120)],
    purchaseDate: [''],
    purchaseDateUnknown: [false],
    purchaseValue: ['', [Validators.maxLength(32), Validators.pattern(/^(?:\d+|\d{1,3}(?:\.\d{3})+)(?:,\d{1,2}|\.\d{1,2})?$/)]],
    currency: ['COP'],
    vendor: ['', Validators.maxLength(120)],
    purchaseReference: ['', Validators.maxLength(120)],
    warrantyUntil: [''],
    observations: ['', Validators.maxLength(1500)],
    damageReason: ['', Validators.maxLength(500)],
    initialInventory: [false],
    approximateAge: ['', Validators.maxLength(80)],
    startUseDate: [''],
    residualValue: [0, [Validators.min(0)]],
    usefulLifeYears: [0, [Validators.required, Validators.min(1)]]
  });

  assetCategoryForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    usefulLifeYears: [5, [Validators.required, Validators.min(1)]],
    annualDepreciationPercent: [20, [Validators.required, Validators.min(0.01), Validators.max(100)]],
    description: ['', Validators.maxLength(500)]
  });

  assetSiteForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    department: ['', [Validators.required, Validators.maxLength(100)]],
    city: ['', [Validators.required, Validators.maxLength(100)]]
  });

  assetAreaForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]]
  });

  assetStatusForm = this.fb.nonNullable.group({
    assetId: [0, [Validators.required, Validators.min(1)]],
    condition: ['Activo', Validators.required],
    reason: ['', [Validators.required, Validators.maxLength(500)]]
  });

  readonly computerCategories = ['Portátil', 'Computador de escritorio', 'Monitor', 'Impresora', 'Servidor', 'Redes y comunicaciones', 'Periférico', 'Otro'];
  readonly enserCategories = ['Mobiliario', 'Silla', 'Escritorio', 'Archivador', 'Electrodoméstico', 'Herramienta', 'Enser de oficina', 'Otro'];
  readonly assetConditions = ['Nuevo', 'Bueno', 'Regular', 'Dañado', 'En reparación'];

  get categoryOptions(): AssetOptions['categories'] {
    return this.assetOptions?.categories ?? [];
  }

  get assetCategoriesForKind(): AssetCategory[] {
    return this.assetCategories;
  }

  get selectedAssetCategory(): AssetCategory | undefined {
    return this.assetCategories.find(category => category.id === this.assetForm.controls.categoryId.value);
  }

  get siteOptions(): AssetOptions['sites'] {
    return this.assetOptions?.sites ?? [];
  }

  get selectedAssetSite(): AssetOptions['sites'][number] | undefined {
    return this.siteOptions.find(site => site.id === this.assetForm.controls.siteId.value);
  }

  get areaOptions(): { id: number; name: string }[] {
    const siteId = this.assetForm.controls.siteId.value;
    return this.siteOptions.find(site => site.id === siteId)?.areas ?? [];
  }

  get responsibleOptions(): AssetOptions['responsibles'] {
    return this.assetOptions?.responsibles ?? [];
  }

  get managedAssets(): AssetRecord[] {
    return this.assets.filter(asset => asset.condition !== 'Retirado');
  }

  get retiredAssets(): AssetRecord[] {
    return this.assets.filter(asset => asset.condition === 'Retirado');
  }

  get pagedManagedAssets(): AssetRecord[] {
    return this.paginate(this.managedAssets, this.assetPage);
  }

  get pagedRetiredAssets(): AssetRecord[] {
    return this.paginate(this.retiredAssets, this.retiredAssetPage);
  }

  get filteredAssetValuations(): AssetDepreciationValuation[] {
    const responsible = this.depreciationResponsibleFilter.trim().toLocaleLowerCase();
    const location = this.depreciationLocationFilter.trim().toLocaleLowerCase();
    return this.assetValuations.filter(asset =>
      (!this.depreciationCategoryFilter || asset.categoryName === this.depreciationCategoryFilter)
      && (!this.depreciationStatusFilter || asset.assetStatus === this.depreciationStatusFilter)
      && (!this.purchaseDateFrom || (asset.purchaseDate ?? '') >= this.purchaseDateFrom)
      && (!this.purchaseDateTo || (asset.purchaseDate ?? '') <= this.purchaseDateTo)
      && (!responsible || (asset.responsible ?? '').toLocaleLowerCase().includes(responsible))
      && (!location || asset.location.toLocaleLowerCase().includes(location))
    );
  }

  get pagedAssetValuations(): AssetDepreciationValuation[] {
    return this.paginate(this.filteredAssetValuations, this.valuationPage);
  }

  get pagedInventoryMovements(): InventoryMovement[] {
    return this.paginate(this.inventoryMovements, this.inventoryPage);
  }

  get pagedDepreciationHistory(): DepreciationRecord[] {
    return this.paginate(this.assetDepreciationHistory, this.depreciationHistoryPage);
  }

  get pagedDepreciationReport(): DepreciationRecord[] {
    return this.paginate(this.depreciationReport?.records ?? [], this.depreciationReportPage);
  }

  private paginate<T>(records: T[], page: number): T[] {
    const start = (Math.max(1, page) - 1) * this.pageSize;
    return records.slice(start, start + this.pageSize);
  }

  changePage(page: number, total: number, target: 'asset' | 'retiredAsset' | 'valuation' | 'inventory' | 'depreciationHistory' | 'depreciationReport'): void {
    const nextPage = Math.max(1, Math.min(page, this.pageCount(total)));
    if (target === 'asset') this.assetPage = nextPage;
    if (target === 'retiredAsset') this.retiredAssetPage = nextPage;
    if (target === 'valuation') this.valuationPage = nextPage;
    if (target === 'inventory') this.inventoryPage = nextPage;
    if (target === 'depreciationHistory') this.depreciationHistoryPage = nextPage;
    if (target === 'depreciationReport') this.depreciationReportPage = nextPage;
  }

  form = this.fb.nonNullable.group({
    sku: ['', [Validators.required, Validators.maxLength(30)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    category: ['', [Validators.required, Validators.maxLength(80)]],
    description: ['', Validators.maxLength(500)],
    price: [0, [Validators.required, Validators.min(0)]],
    stock: [0, [Validators.required, Validators.min(0), Validators.pattern(/^\d+$/)]],
    minStock: [5, [Validators.required, Validators.min(0), Validators.pattern(/^\d+$/)]],
    supplier: ['', Validators.maxLength(120)]
  });

  ngOnInit(): void {
    this.checkSession();
  }

  private checkSession(): void {
    this.auth.csrf().subscribe({
      next: () => this.auth.setupStatus().subscribe({
        next: status => {
          this.setupRequired = status.setupRequired;
          if (!status.setupRequired) {
            this.auth.currentAccount().subscribe({
              next: account => this.setCurrentUser(account),
              error: error => {
                if (error.status !== 401) this.authError = 'No se pudo verificar tu sesión. Revisa que el servidor esté disponible.';
              }
            });
          }
        },
        error: () => this.authError = 'No se pudo comprobar la configuración de cuentas. Verifica que el servidor esté disponible.'
      }),
      error: () => this.authError = 'No se pudo conectar con el servidor de inventario.'
    });
  }

  submitAuthentication(): void {
    if (this.authForm.invalid || (this.setupRequired && this.authForm.controls.fullName.invalid)) {
      this.authForm.markAllAsTouched();
      return;
    }
    const credentials = this.authForm.getRawValue();
    this.authBusy = true;
    this.authError = '';
    const request = this.setupRequired
      ? this.auth.setupAdministrator({ fullName: credentials.fullName, username: credentials.username, password: credentials.password })
      : this.auth.login({ username: credentials.username, password: credentials.password });
    request.pipe(finalize(() => this.authBusy = false)).subscribe({
      next: account => {
        this.setupRequired = false;
        this.authForm.reset({ fullName: '', username: '', password: '' });
        this.setCurrentUser(account);
      },
      error: error => this.authError = error?.error?.message ?? 'No se pudo validar la cuenta.'
    });
  }

  private setCurrentUser(account: Account): void {
    this.currentUser = account;
    this.authError = '';
    this.load();
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => {
        this.currentUser = null;
        this.authForm.reset({ fullName: '', username: '', password: '' });
        this.checkSession();
      },
      error: () => this.authError = 'No se pudo cerrar la sesión. Intenta nuevamente.'
    });
  }

  userInitials(name: string): string {
    return name.trim().split(/\s+/).slice(0, 2).map(part => part.charAt(0).toUpperCase()).join('');
  }

  get filteredProducts(): Product[] {
    const query = this.searchTerm.trim().toLocaleLowerCase();
    return this.products.filter(product => {
      const matchesQuery = !query || `${product.name} ${product.sku} ${product.supplier ?? ''}`.toLocaleLowerCase().includes(query);
      const matchesCategory = this.selectedCategory === 'all' || product.category === this.selectedCategory;
      const matchesAvailability = this.availabilityFilter === 'all'
        || (this.availabilityFilter === 'available' ? product.stock > 0 : product.stock === 0);
      return matchesQuery && matchesCategory && matchesAvailability;
    });
  }

  get pagedProducts(): Product[] {
    const start = (this.productPage - 1) * this.pageSize;
    return this.filteredProducts.slice(start, start + this.pageSize);
  }

  pageCount(total: number): number {
    return Math.max(1, Math.ceil(total / this.pageSize));
  }

  pageStart(total: number, page: number): number {
    return total ? (Math.min(page, this.pageCount(total)) - 1) * this.pageSize + 1 : 0;
  }

  pageEnd(total: number, page: number): number {
    return Math.min(total, Math.min(page, this.pageCount(total)) * this.pageSize);
  }

  changeProductPage(page: number): void {
    this.productPage = Math.max(1, Math.min(page, this.pageCount(this.filteredProducts.length)));
  }

  get availableProductsCount(): number {
    return this.products.filter(product => product.stock > 0).length;
  }

  get outOfStockProductsCount(): number {
    return this.products.filter(product => product.stock === 0).length;
  }

  get categories(): string[] {
    return Array.from(new Set([...this.defaultCategories, ...this.products.map(product => product.category)]))
      .filter(category => category.trim().length > 0)
      .sort((first, second) => first.localeCompare(second, 'es'));
  }

  get categoryBreakdown(): { name: string; count: number; percent: number; color: string }[] {
    const counts = new Map<string, number>();
    this.products.forEach(product => counts.set(product.category, (counts.get(product.category) ?? 0) + 1));
    const colors = ['#6658e8', '#39a7a2', '#ef9d54', '#6e91da', '#dc6b87', '#8b9aaf'];
    const entries = Array.from(counts.entries()).sort((a, b) => b[1] - a[1]).slice(0, 5);
    const maxCount = Math.max(...entries.map(([, count]) => count), 1);
    return entries.map(([name, count], index) => ({
      name,
      count,
      percent: Math.max(7, (count / maxCount) * 100),
      color: colors[index % colors.length]
    }));
  }

  get recentInventoryMovements(): InventoryMovement[] {
    return [...this.inventoryMovements]
      .sort((first, second) => new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime())
      .slice(0, 6);
  }

  inventoryMovementLabel(type: string): string {
    const labels: Record<string, string> = { IN: 'Entrada', OUT: 'Salida', ADJUSTMENT: 'Ajuste' };
    return labels[type] ?? type;
  }

  activityDate(value: string): string {
    return new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short' }).format(new Date(value));
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.business.getInventoryMovements().subscribe({
      next: movements => this.inventoryMovements = movements,
      error: () => this.inventoryMovements = []
    });
    forkJoin({ products: this.service.getAll(), summary: this.service.getSummary() })
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: data => {
          this.products = data.products;
          this.summary = data.summary;
        },
        error: () => this.error = 'No se pudo cargar el inventario. Verifica que el backend y la base de datos estén disponibles.'
      });
  }

  selectMenu(): void {
    this.activeMenu = 'dashboard';
  }

  openGoodsEntry(kind: 'computer' | 'enser' = this.assetKind): void {
    this.activeMenu = 'assets';
    this.goodsMenuExpanded = true;
    if (!this.goodsEntryInitialized || kind !== this.assetKind) {
      this.assetKind = kind;
      this.resetGoodsEntry();
      this.goodsEntryInitialized = true;
    }
    this.moduleError = '';
    this.moduleMessage = '';
    this.loadAssetOptions();
  }

  loadAssetOptions(): void {
    this.business.getAssetOptions().subscribe({
      next: options => {
        this.assetOptions = options;
        if (this.assetEditingId !== null) {
          const asset = this.assets.find(existing => existing.id === this.assetEditingId);
          if (asset) this.populateAssetForm(asset);
          return;
        }
        this.assetForm.controls.companyId.setValue(options.companyId);
      },
      error: error => {
        if (!this.handleExpiredAssetSession(error)) {
          this.moduleError = this.apiMessage(error, 'No se pudieron cargar las empresas, sedes, áreas y responsables.');
        }
      }
    });
    this.loadAssetCategories();
  }

  loadAssetCategories(): void {
    this.business.getAssetCategories().subscribe({
      next: categories => this.assetCategories = categories,
      error: error => this.moduleError = this.apiMessage(error, 'No se pudieron cargar las categorías de activos.')
    });
  }

  selectAssetCategory(): void {
    const category = this.selectedAssetCategory;
    this.assetForm.controls.usefulLifeYears.setValue(category?.usefulLifeYears ?? 0);
  }

  updateAnnualDepreciation(): void {
    const usefulLifeYears = Number(this.assetCategoryForm.controls.usefulLifeYears.value);
    const annualPercent = usefulLifeYears > 0 ? Number((100 / usefulLifeYears).toFixed(2)) : 0;
    this.assetCategoryForm.controls.annualDepreciationPercent.setValue(annualPercent, { emitEvent: false });
  }

  saveAssetCategory(): void {
    if (this.assetCategoryForm.invalid) {
      this.assetCategoryForm.markAllAsTouched();
      return;
    }
    const values = this.assetCategoryForm.getRawValue();
    const request: AssetCategoryRequest = {
      name: values.name.trim(),
      usefulLifeYears: values.usefulLifeYears,
      annualDepreciationRate: 1 / values.usefulLifeYears,
      depreciationMethod: 'STRAIGHT_LINE',
      description: values.description.trim()
    };
    this.savingCategory = true;
    this.business.createAssetCategory(request).pipe(finalize(() => this.savingCategory = false)).subscribe({
      next: category => {
        this.assetCategories = [...this.assetCategories.filter(existing => existing.id !== category.id), category]
          .sort((left, right) => left.name.localeCompare(right.name, 'es'));
        this.assetForm.patchValue({ categoryId: category.id, usefulLifeYears: category.usefulLifeYears });
        this.assetCategoryForm.reset({ name: '', usefulLifeYears: 5, annualDepreciationPercent: 20, description: '' });
        this.showCategoryForm = false;
        this.moduleMessage = `Categoría "${category.name}" creada.`;
        this.moduleError = '';
      },
      error: error => this.moduleError = this.apiMessage(error, 'No se pudo crear la categoría.')
    });
  }

  createAssetSite(): void {
    if (this.assetSiteForm.invalid) {
      this.assetSiteForm.markAllAsTouched();
      return;
    }
    const values = this.assetSiteForm.getRawValue();
    const request: AssetSiteRequest = {
      name: values.name.trim(),
      department: values.department.trim(),
      city: values.city.trim()
    };
    this.savingSite = true;
    this.business.createAssetSite(request).pipe(finalize(() => this.savingSite = false)).subscribe({
      next: site => {
        if (this.assetOptions) {
          this.assetOptions = { ...this.assetOptions, sites: [...this.assetOptions.sites, site].sort((a, b) => a.name.localeCompare(b.name, 'es')) };
        }
        this.selectAssetSite(site.id);
        this.showAreaForm = true;
        this.assetSiteForm.reset({ name: '', department: '', city: '' });
        this.showSiteForm = false;
        this.moduleError = '';
        this.moduleMessage = `Sede "${site.name}" creada. Completa el área para continuar.`;
      },
      error: error => this.moduleError = this.apiMessage(error, 'No se pudo crear la sede.')
    });
  }

  get assetDistributionTotal(): number {
    return this.assetDistributions.reduce((total, row) => total + row.quantity, 0);
  }

  createAssetArea(): void {
    const siteId = this.assetForm.controls.siteId.value;
    if (!siteId) {
      this.moduleError = 'Selecciona o crea una sede antes de crear un área.';
      return;
    }
    if (this.assetAreaForm.invalid) {
      this.assetAreaForm.markAllAsTouched();
      return;
    }
    const name = this.assetAreaForm.controls.name.value.trim();
    this.savingArea = true;
    this.business.createAssetArea(siteId, name).pipe(finalize(() => this.savingArea = false)).subscribe({
      next: area => {
        if (this.assetOptions) {
          this.assetOptions = {
            ...this.assetOptions,
            sites: this.assetOptions.sites.map(site => site.id === siteId
              ? { ...site, areas: [...site.areas, area].sort((a, b) => a.name.localeCompare(b.name, 'es')) }
              : site)
          };
        }
        this.assetForm.controls.areaId.setValue(area.id);
        this.assetForm.controls.area.setValue(area.name);
        this.assetAreaForm.reset({ name: '' });
        this.showAreaForm = false;
        this.moduleError = '';
        this.moduleMessage = `Área "${area.name}" creada para ${this.selectedAssetSite?.name ?? 'la sede'}.`;
      },
      error: error => this.moduleError = this.apiMessage(error, 'No se pudo crear el área.')
    });
  }

  loadDepreciationValuation(): void {
    if (!this.valuationDate) return;
    this.valuationLoading = true;
    this.business.getAssetDepreciationValuation(this.valuationDate)
      .pipe(finalize(() => this.valuationLoading = false))
      .subscribe({
        next: valuations => {
          this.assetValuations = valuations;
          this.valuationPage = 1;
        },
        error: error => this.moduleError = this.apiMessage(error, 'No se pudo consultar la depreciación.')
      });
  }

  selectAssetSite(siteId = this.assetForm.controls.siteId.value): void {
    const site = this.siteOptions.find(option => option.id === Number(siteId));
    this.assetForm.controls.siteId.setValue(site?.id ?? 0);
    this.assetForm.controls.location.setValue(site?.name ?? '');
    this.assetForm.controls.areaId.setValue(0);
    this.assetForm.controls.area.setValue('');
  }

  selectAssetArea(): void {
    const area = this.areaOptions.find(option => option.id === this.assetForm.controls.areaId.value);
    this.assetForm.controls.area.setValue(area?.name ?? '');
  }

  selectAssetResponsible(): void {
    const responsible = this.responsibleOptions.find(option => option.id === this.assetForm.controls.responsibleId.value);
    this.assetForm.controls.responsible.setValue(responsible?.fullName ?? '');
  }

  selectAssetKind(kind: 'computer' | 'enser'): void {
    this.assetKind = kind;
    this.assetForm.controls.categoryId.reset(0);
    this.assetForm.patchValue({ processor: '', ram: '', storage: '', operatingSystem: '', material: '', color: '', dimensions: '', itemDetails: '' });
    this.goodsPreview = false;
  }

  resetGoodsEntry(): void {
    this.assetEditingId = null;
    this.assetDistributions = [];
    this.entryQuantity = 1;
    this.assetForm.reset({
      categoryId: 0,
      name: '',
      serialNumber: '',
      brand: '',
      model: '',
      processor: '',
      ram: '',
      storage: '',
      operatingSystem: '',
      material: '',
      color: '',
      dimensions: '',
      itemDetails: '',
      companyId: this.assetOptions?.companyId ?? 0,
      siteId: 0,
      areaId: 0,
      responsibleId: 0,
      location: '',
      area: '',
      condition: 'Activo',
      responsible: '',
      purchaseDate: '',
      purchaseDateUnknown: false,
      purchaseValue: '',
      currency: 'COP',
      warrantyUntil: '',
      vendor: '',
      purchaseReference: '',
      observations: '',
      damageReason: '',
      initialInventory: false,
      approximateAge: '',
      startUseDate: '',
      residualValue: 0,
      usefulLifeYears: 0
    });
    this.goodsPreview = false;
  }

  addAssetDistribution(): void {
    const siteId = Number(this.assetForm.controls.siteId.value);
    const areaId = Number(this.assetForm.controls.areaId.value);
    const quantity = Number(this.entryQuantity);
    if (!siteId || !areaId || !Number.isInteger(quantity) || quantity < 1) {
      this.moduleError = 'Selecciona sede, área y una cantidad entera mayor que cero.';
      return;
    }
    const total = this.assetDistributions.reduce((sum, row) => sum + row.quantity, 0) + quantity;
    if (total > 500) {
      this.moduleError = 'Puedes registrar hasta 500 unidades por ingreso.';
      return;
    }
    const existing = this.assetDistributions.find(row => row.siteId === siteId && row.areaId === areaId);
    if (existing) existing.quantity += quantity;
    else this.assetDistributions = [...this.assetDistributions, { siteId, areaId, quantity }];
    this.entryQuantity = 1;
    this.moduleError = '';
  }

  removeAssetDistribution(index: number): void {
    this.assetDistributions = this.assetDistributions.filter((_, rowIndex) => rowIndex !== index);
  }

  assetDistributionName(distribution: { siteId: number; areaId: number }): string {
    const site = this.siteOptions.find(option => option.id === distribution.siteId);
    const area = site?.areas.find(option => option.id === distribution.areaId);
    return `${site?.name ?? 'Sede'} · ${site?.department ?? 'Departamento'} · ${site?.city ?? 'Ciudad'} · ${area?.name ?? 'Área'}`;
  }

  editAsset(asset: AssetRecord): void {
    this.activeMenu = 'assets';
    this.assetKind = asset.assetType === 'Enser' ? 'enser' : 'computer';
    this.assetEditingId = asset.id;
    this.moduleError = '';
    this.moduleMessage = '';
    if (!this.assetOptions) {
      this.loadAssetOptions();
      return;
    }
    this.populateAssetForm(asset);
  }

  private populateAssetForm(asset: AssetRecord): void {
    const site = this.siteOptions.find(option => option.id === asset.siteId)
      ?? this.siteOptions.find(option => option.name.toLocaleLowerCase() === asset.location.toLocaleLowerCase());
    const areas = site?.areas ?? [];
    const area = areas.find(option => option.id === asset.areaId)
      ?? areas.find(option => option.name.toLocaleLowerCase() === asset.area.toLocaleLowerCase());
    const responsible = this.responsibleOptions.find(option => option.id === asset.responsibleId)
      ?? this.responsibleOptions.find(option => option.fullName.toLocaleLowerCase() === asset.responsible?.toLocaleLowerCase());
    this.assetForm.reset({
      categoryId: asset.categoryId ?? 0,
      name: asset.name,
      serialNumber: asset.serialNumber ?? '',
      brand: asset.brand ?? '',
      model: asset.model ?? '',
      processor: asset.processor ?? '',
      ram: asset.ram ?? '',
      storage: asset.storage ?? '',
      operatingSystem: asset.operatingSystem ?? '',
      material: asset.material ?? '',
      color: asset.color ?? '',
      dimensions: asset.dimensions ?? '',
      itemDetails: asset.itemDetails ?? '',
      companyId: asset.companyId ?? this.assetOptions?.companyId ?? 0,
      siteId: site?.id ?? 0,
      areaId: area?.id ?? 0,
      responsibleId: responsible?.id ?? 0,
      location: site?.name ?? asset.location,
      area: area?.name ?? asset.area,
      condition: asset.condition,
      responsible: responsible?.fullName ?? asset.responsible ?? '',
      purchaseDate: asset.purchaseDate ?? '',
      purchaseDateUnknown: !asset.purchaseDate,
      purchaseValue: asset.purchaseValue == null ? '' : new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 }).format(asset.purchaseValue),
      currency: asset.currency ?? 'COP',
      vendor: asset.vendor ?? '',
      purchaseReference: asset.purchaseReference ?? '',
      warrantyUntil: asset.warrantyUntil ?? '',
      observations: asset.observations ?? '',
      damageReason: '',
      initialInventory: false,
      approximateAge: '',
      startUseDate: asset.startUseDate ?? '',
      residualValue: asset.residualValue ?? 0,
      usefulLifeYears: asset.usefulLifeYears ?? 0
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  loadAssets(): void {
    this.assetLoading = true;
    this.business.getAssets().pipe(finalize(() => this.assetLoading = false)).subscribe({
      next: assets => {
        this.assets = assets;
        this.assetPage = 1;
        this.retiredAssetPage = 1;
      },
      error: error => {
        if (!this.handleExpiredAssetSession(error)) {
          this.moduleError = this.apiMessage(error, 'No se pudieron cargar los productos registrados.');
        }
      }
    });
  }

  markPurchaseDateUnknown(): void {
    if (this.assetForm.controls.purchaseDateUnknown.value) {
      this.assetForm.controls.purchaseDate.setValue('');
    }
  }

  openPurchaseDatePicker(input: HTMLInputElement): void {
    if (input.disabled) return;
    const picker = input as HTMLInputElement & { showPicker?: () => void };
    if (typeof picker.showPicker === 'function') picker.showPicker();
    else input.focus();
  }

  formatPurchaseValue(): void {
    const amount = this.parsePurchaseValue(this.assetForm.controls.purchaseValue.value);
    if (amount !== null) {
      this.assetForm.controls.purchaseValue.setValue(
        new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 }).format(amount)
      );
    }
  }

  private parsePurchaseValue(value: string): number | null {
    const trimmed = value.trim();
    if (!trimmed) return null;
    let normalized = trimmed;
    if (normalized.includes(',')) {
      normalized = normalized.replace(/\./g, '').replace(',', '.');
    } else if (/^\d{1,3}(?:\.\d{3})+$/.test(normalized)) {
      normalized = normalized.replace(/\./g, '');
    }
    const amount = Number(normalized);
    return Number.isFinite(amount) ? amount : null;
  }

  private handleExpiredAssetSession(error: unknown): boolean {
    if (!(error instanceof HttpErrorResponse) || error.status !== 401) return false;
    this.currentUser = null;
    this.authError = 'La sesión expiró. Inicia sesión nuevamente para consultar el inventario.';
    return true;
  }

  saveGoodsEntry(): void {
    const values = this.assetForm.getRawValue();
    const purchaseValue = this.parsePurchaseValue(values.purchaseValue);
    const residualValue = Number(values.residualValue);
    if (purchaseValue != null && purchaseValue <= 0) {
      this.moduleError = 'El valor de adquisición debe ser mayor que cero.';
      this.assetForm.controls.purchaseValue.markAsTouched();
      return;
    }
    if (residualValue < 0 || (purchaseValue != null && residualValue > purchaseValue)) {
      this.moduleError = 'El valor residual debe estar entre cero y el valor de adquisición.';
      this.assetForm.controls.residualValue.markAsTouched();
      return;
    }
    if (values.startUseDate && values.startUseDate > this.todayDate) {
      this.moduleError = 'La fecha de puesta en servicio no puede ser posterior a hoy.';
      this.assetForm.controls.startUseDate.markAsTouched();
      return;
    }
    if (this.assetForm.invalid) {
      this.assetForm.markAllAsTouched();
      return;
    }
    const {
      damageReason, initialInventory, approximateAge, purchaseDateUnknown, observations: enteredObservations,
      companyId, siteId, areaId, ...assetValues
    } = values;
    const observations = [
      enteredObservations.trim(),
      damageReason.trim() ? `Motivo del cambio o daño: ${damageReason.trim()}` : '',
      initialInventory ? 'Tipo de registro: inventario inicial; este bien ya existía antes de usar el sistema.' : '',
      initialInventory && approximateAge.trim() ? `Antigüedad aproximada: ${approximateAge.trim()}.` : ''
    ]
      .filter(Boolean)
      .join('\n');
    const request: AssetRequest = {
      ...assetValues,
      companyId,
      siteId,
      areaId,
      responsibleId: null,
      location: values.location,
      condition: values.condition || 'Activo',
      observations,
      assetType: this.assetKind === 'computer' ? 'Equipo de cómputo' : 'Enser',
      purchaseDate: purchaseDateUnknown ? null : values.purchaseDate || null,
      purchaseValue,
      warrantyUntil: values.warrantyUntil || null
    };
    if (this.assetEditingId === null) {
      this.assetDistributions = [{ siteId, areaId, quantity: 1 }];
    }
    if (this.assetEditingId === null && this.assetDistributions.length === 0) {
      this.moduleError = 'Agrega al menos una sede y área con la cantidad que vas a ingresar.';
      return;
    }
    const totalUnits = this.assetDistributionTotal;
    if (this.assetEditingId === null && totalUnits > 1 && values.serialNumber.trim()) {
      this.moduleError = 'Para registrar varias unidades, deja el serial vacío. Registra por separado los equipos con serial individual.';
      return;
    }
    const batchRequests = this.assetDistributions.flatMap(distribution => {
      const site = this.siteOptions.find(option => option.id === distribution.siteId);
      const area = site?.areas.find(option => option.id === distribution.areaId);
      if (!site || !area) return [];
      return Array.from({ length: distribution.quantity }, () => ({
        ...request,
        siteId: distribution.siteId,
        areaId: distribution.areaId,
        location: site.name,
        area: area.name,
        serialNumber: totalUnits > 1 ? '' : request.serialNumber
      }));
    });
    this.savingAsset = true;
    this.moduleError = '';
    this.moduleMessage = '';
    if (this.assetEditingId !== null) {
      this.business.updateAsset(this.assetEditingId, request).pipe(finalize(() => this.savingAsset = false)).subscribe({
        next: asset => {
          this.assets = this.assets.map(existing => existing.id === asset.id ? asset : existing);
          this.moduleMessage = `Datos corregidos. Se conservó el código ${asset.inventoryCode}.`;
          this.resetGoodsEntry();
        },
        error: error => this.moduleError = this.apiMessage(error, 'No se pudo guardar la corrección. Revisa los datos e inténtalo de nuevo.')
      });
      return;
    }
    this.business.createAssetsBatch(batchRequests).pipe(finalize(() => this.savingAsset = false)).subscribe({
      next: created => {
        this.assets = [...created, ...this.assets.filter(existing => !created.some(asset => asset.id === existing.id))];
        this.moduleMessage = `${created.length} unidades del producto registradas correctamente y distribuidas por sede.`;
        this.resetGoodsEntry();
      },
      error: error => this.moduleError = this.apiMessage(error, 'No se pudieron guardar las unidades. Revisa la distribución por sede e inténtalo de nuevo.')
    });
  }

  selectReports(): void {
    this.openModule('report');
  }

  openModule(module: Exclude<BusinessModuleKey, 'dashboard' | 'assets'>): void {
    if (module === 'users' && this.currentUser?.role !== 'ADMIN') return;
    if (module === 'admin-config' && !this.canAdminConfiguration) return;
    if (module === 'depreciation' && !this.can('DEPRECIACION_WRITE')) return;
    this.activeMenu = module;
    if (module === 'users' || module === 'admin-config' || module === 'depreciation') this.adminMenuExpanded = true;
    if (module === 'asset-management' || module === 'depreciation') this.filesMenuExpanded = true;
    this.moduleError = '';
    this.moduleMessage = '';
    if (module === 'admin-config') {
      this.loadAssetOptions();
      this.loadAssetCategories();
    } else if (module === 'asset-management') {
      this.assetStatusForm.reset({ assetId: 0, condition: 'Activo', reason: '' });
      this.loadAssets();
      this.loadAssetOptions();
    } else if (module === 'depreciation') {
      this.valuationDate = this.localDateString();
      this.loadAssetOptions();
      this.loadDepreciationValuation();
    } else if (module === 'quotes') {
      this.loadCollection(this.business.getQuotes(), records => this.quotes = records);
    } else if (module === 'purchases') {
      this.loadCollection(this.business.getPurchases(), records => this.purchases = records);
    } else if (module === 'cash') {
      this.moduleLoading = true;
      this.business.getActiveRegister().pipe(finalize(() => this.moduleLoading = false)).subscribe({
        next: register => {
          this.activeRegister = register;
          if (register) {
            this.business.getCashMovements().subscribe({
              next: movements => this.cashMovements = movements,
              error: error => this.moduleError = this.apiMessage(error, 'No se pudieron cargar los movimientos de caja.')
            });
          } else {
            this.cashMovements = [];
          }
        },
        error: error => this.moduleError = this.apiMessage(error, 'No se pudo consultar la caja.')
      });
    } else if (module === 'sales') {
      this.loadCollection(this.business.getSales(), records => this.sales = records);
    } else if (module === 'reservations') {
      this.loadCollection(this.business.getReservations(), records => this.reservations = records);
    } else if (module === 'inventory') {
      this.loadCollection(this.business.getInventoryMovements(), records => {
        this.inventoryMovements = records;
        this.inventoryPage = 1;
      });
    } else if (module === 'receipts') {
      this.loadCollection(this.business.getReceipts(), records => this.receipts = records);
    } else if (module === 'users') {
      this.userError = '';
      this.userMessage = '';
      this.usersSearch = '';
      this.usersLoading = true;
      this.auth.getUsers().pipe(finalize(() => this.usersLoading = false)).subscribe({
        next: users => this.users = users,
        error: error => this.userError = error?.error?.message ?? 'No se pudieron cargar las cuentas.'
      });
    }
  }

  toggleAdminMenu(): void {
    this.adminMenuExpanded = !this.adminMenuExpanded;
  }

  toggleFilesMenu(): void {
    this.filesMenuExpanded = !this.filesMenuExpanded;
  }

  toggleGoodsMenu(): void {
    this.goodsMenuExpanded = !this.goodsMenuExpanded;
  }

  selectManagedAsset(): void {
    const asset = this.assets.find(item => item.id === this.assetStatusForm.controls.assetId.value);
    this.assetStatusForm.patchValue({ condition: asset?.condition ?? 'Activo' });
  }

  openAssetForStatus(asset: AssetRecord, condition = asset.condition): void {
    this.assetStatusForm.reset({ assetId: asset.id, condition, reason: '' });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  saveAssetStatus(): void {
    if (this.assetStatusForm.invalid) {
      this.assetStatusForm.markAllAsTouched();
      return;
    }
    const request = this.assetStatusForm.getRawValue();
    this.savingAsset = true;
    this.moduleError = '';
    this.moduleMessage = '';
    this.business.updateAssetStatus(request.assetId, { condition: request.condition, reason: request.reason.trim() })
      .pipe(finalize(() => this.savingAsset = false))
      .subscribe({
        next: () => {
          this.moduleMessage = 'Estado del activo actualizado.';
          this.loadAssets();
          this.loadDepreciationValuation();
        },
        error: error => this.moduleError = this.apiMessage(error, 'No se pudo actualizar el estado del activo.')
      });
  }
  openModuleForm(): void {
    if (this.activeMenu === 'cash') {
      this.moduleForm.reset({ ...this.moduleForm.getRawValue(), openingAmount: 0, amount: 0, description: '', movementType: 'IN', reason: '' });
    } else {
      this.basket = [];
      this.moduleForm.reset({
        customerName: '', supplierName: '', productId: 0, quantity: 1, deposit: 0,
        openingAmount: 0, amount: 0, movementType: 'IN', paymentMethod: 'CASH', description: '', reason: ''
      });
    }
    this.showModuleForm = true;
    this.moduleError = '';
  }

  get moduleFormTitle(): string {
    if (this.activeMenu === 'quotes') return 'Crear cotización';
    if (this.activeMenu === 'purchases') return 'Registrar compra al proveedor';
    if (this.activeMenu === 'sales') return 'Registrar venta';
    if (this.activeMenu === 'reservations') return 'Apartar productos';
    if (this.activeMenu === 'inventory') return 'Ajuste manual de existencias';
    return this.activeRegister ? 'Registrar movimiento de caja' : 'Abrir caja';
  }

  get basketTotal(): number {
    return this.basket.reduce((total, line) => total + this.basketUnitPrice(line) * line.quantity, 0);
  }

  get selectedProduct(): Product | undefined {
    return this.products.find(product => product.id === Number(this.moduleForm.controls.productId.value));
  }

  get inventoryMovementQuantity(): number {
    const amount = Number(this.moduleForm.controls.amount.value);
    return this.moduleForm.controls.movementType.value === 'ADJUSTMENT'
      ? amount - (this.selectedProduct?.stock ?? 0)
      : amount;
  }

  addBasketLine(): void {
    const productId = Number(this.moduleForm.controls.productId.value);
    const quantity = Number(this.moduleForm.controls.quantity.value);
    const product = this.products.find(candidate => candidate.id === productId);
    if (!productId || !product || !Number.isInteger(quantity) || quantity < 1) {
      this.moduleError = 'Selecciona un producto y una cantidad entera mayor que cero.';
      return;
    }
    const existingQuantity = this.basket.find(line => line.productId === productId)?.quantity ?? 0;
    const tracksAvailableStock = this.activeMenu === 'sales' || this.activeMenu === 'reservations';
    if (tracksAvailableStock && existingQuantity + quantity > product.stock) {
      this.moduleError = `Existencia insuficiente para ${product.name}. Disponible: ${Math.max(0, product.stock - existingQuantity)} unidades.`;
      return;
    }
    const existing = this.basket.find(line => line.productId === productId);
    if (existing) existing.quantity += quantity;
    else this.basket.push({
      productId,
      quantity,
      ...(this.activeMenu === 'purchases' ? { unitCost: product.price } : {})
    });
    this.moduleError = '';
    this.moduleForm.controls.productId.setValue(0);
    this.moduleForm.controls.quantity.setValue(1);
  }

  removeBasketLine(productId: number): void {
    this.basket = this.basket.filter(line => line.productId !== productId);
  }

  submitModuleForm(): void {
    this.moduleError = '';
    if (this.activeMenu === 'cash') {
      if (!this.activeRegister) {
        const openingAmount = Number(this.moduleForm.controls.openingAmount.value);
        if (!Number.isFinite(openingAmount) || openingAmount < 0) {
          this.moduleError = 'El fondo inicial no puede ser negativo.';
          return;
        }
        this.submitModuleRequest(this.business.openRegister({ openingAmount }), 'La caja quedó abierta.');
        return;
      }
      const amount = Number(this.moduleForm.controls.amount.value);
      const description = this.moduleForm.controls.description.value.trim();
      if (!Number.isFinite(amount) || amount <= 0 || !description) {
        this.moduleError = 'Indica un importe mayor que cero y una descripción para el movimiento.';
        return;
      }
      this.submitModuleRequest(this.business.createCashMovement(
        this.moduleForm.controls.movementType.value === 'OUT' ? 'OUT' : 'IN',
        {
        amount,
        description
        }
      ), 'El movimiento quedó registrado.');
      return;
    }

    if (this.activeMenu === 'inventory') {
      const product = this.selectedProduct;
      const amount = Number(this.moduleForm.controls.amount.value);
      const reason = this.moduleForm.controls.reason.value.trim();
      if (!product || !Number.isInteger(amount) || amount < 0 || !reason) {
        this.moduleError = 'Selecciona un producto, una existencia final válida y explica el motivo.';
        return;
      }
      if (this.moduleForm.controls.movementType.value !== 'ADJUSTMENT' && amount === 0) {
        this.moduleError = 'El ajuste de existencias debe ser mayor que cero.';
        return;
      }
      this.submitModuleRequest(this.business.createInventoryMovement({
        productId: product.id,
        type: this.moduleForm.controls.movementType.value,
        quantity: amount,
        reason
      }), 'El movimiento de inventario quedó registrado.');
      return;
    }

    if (!this.basket.length) {
      this.moduleError = 'Agrega al menos un producto antes de guardar.';
      return;
    }
    if (this.activeMenu === 'quotes') {
      const customerName = this.moduleForm.controls.customerName.value.trim();
      if (!customerName) {
        this.moduleError = 'Escribe el nombre del cliente para la cotización.';
        return;
      }
      this.submitModuleRequest(this.business.createQuote({
        customerName,
        items: this.basket
      }), 'La cotización quedó guardada.');
    } else if (this.activeMenu === 'purchases') {
      const supplierName = this.moduleForm.controls.supplierName.value.trim();
      if (!supplierName) {
        this.moduleError = 'Escribe el nombre del proveedor.';
        return;
      }
      this.submitModuleRequest(this.business.createPurchase({
        supplierName,
        items: this.basket.map(item => ({ ...item, unitCost: item.unitCost ?? this.productPrice(item.productId) }))
      }), 'La orden de compra quedó registrada.');
    } else if (this.activeMenu === 'sales') {
      this.submitModuleRequest(this.business.createSale({
        paymentMethod: this.moduleForm.controls.paymentMethod.value,
        items: this.basket
      }), 'La venta quedó registrada y el inventario fue actualizado.');
    } else if (this.activeMenu === 'reservations') {
      const customerName = this.moduleForm.controls.customerName.value.trim();
      if (!customerName) {
        this.moduleError = 'Escribe el nombre del cliente para el apartado.';
        return;
      }
      this.submitModuleRequest(this.business.createReservation({
        customerName,
        items: this.basket
      }), 'El apartado quedó registrado y las unidades quedaron reservadas.');
    }
  }

  receivePurchase(purchase: TradeDocument): void {
    if (purchase.status !== 'PENDING') return;
    this.moduleError = '';
    this.business.receivePurchase(purchase.id).subscribe({
      next: () => {
        this.moduleMessage = 'Mercancía recibida. Las existencias ya se actualizaron.';
        this.load();
        this.openModule('purchases');
      },
      error: error => this.moduleError = this.apiMessage(error, 'No se pudo recibir la orden.')
    });
  }

  closeRegister(): void {
    if (!this.activeRegister || !confirm('¿Confirmas el cierre de esta caja? No se podrán registrar más movimientos en esta jornada.')) return;
    this.business.closeRegister().subscribe({
      next: () => {
        this.moduleMessage = 'La caja se cerró correctamente.';
        this.openModule('cash');
      },
      error: error => this.moduleError = this.apiMessage(error, 'No se pudo cerrar la caja.')
    });
  }

  cancelReservation(reservation: TradeDocument): void {
    if (reservation.status !== 'ACTIVE' || !confirm(`¿Cancelar el apartado ${reservation.number} y liberar los productos?`)) return;
    this.business.cancelReservation(reservation.id).subscribe({
      next: () => {
        this.moduleMessage = 'El apartado se canceló y sus unidades quedaron disponibles.';
        this.load();
        this.openModule('reservations');
      },
      error: error => this.moduleError = this.apiMessage(error, 'No se pudo cancelar el apartado.')
    });
  }

  fulfillReservation(reservation: TradeDocument): void {
    if (reservation.status !== 'ACTIVE' || !confirm(`¿Entregar los productos del apartado ${reservation.number} y cobrar el saldo?`)) return;
    this.business.fulfillReservation(reservation.id).subscribe({
      next: () => {
        this.moduleMessage = 'El apartado se entregó y se registró la venta.';
        this.load();
        this.openModule('reservations');
      },
      error: error => this.moduleError = this.apiMessage(error, 'No se pudo entregar el apartado.')
    });
  }

  downloadReceipt(receiptNumber: string): void {
    this.business.getReceiptPdf(receiptNumber).subscribe({
      next: file => this.saveBlob(file, `${receiptNumber}.pdf`),
      error: error => this.moduleError = this.apiMessage(error, 'No se pudo descargar el comprobante.')
    });
  }

  countByStatus(records: readonly TradeDocument[], status: string): number {
    return records.filter(record => record.status === status).length;
  }

  statusLabel(status: string | undefined): string {
    if (!status) return '—';
    const labels: Record<string, string> = {
      OPEN: 'Pendiente',
      APPROVED: 'Aprobada',
      PENDING: 'Por recibir',
      RECEIVED: 'Recibida',
      ACTIVE: 'Activo',
      FULFILLED: 'Entregado',
      CANCELLED: 'Cancelado',
      CASH: 'Efectivo',
      CARD: 'Tarjeta',
      TRANSFER: 'Transferencia',
      IN: 'Entrada',
      OUT: 'Salida',
      ADJUSTMENT: 'Ajuste',
      PAID: 'Cobrado'
    };
    return labels[status] ?? status;
  }

  productName(productId: number): string {
    return this.products.find(product => product.id === productId)?.name ?? 'Producto eliminado';
  }

  productPrice(productId: number): number {
    return this.products.find(product => product.id === productId)?.price ?? 0;
  }

  basketUnitPrice(line: BusinessLineRequest): number {
    return this.activeMenu === 'purchases' ? line.unitCost ?? this.productPrice(line.productId) : this.productPrice(line.productId);
  }

  private loadCollection<T>(request: Observable<T[]>, update: (records: T[]) => void): void {
    this.moduleLoading = true;
    request.pipe(finalize(() => this.moduleLoading = false)).subscribe({
      next: update,
      error: error => this.moduleError = this.apiMessage(error, `No se pudieron cargar ${this.currentModuleTitle.toLowerCase()}.`)
    });
  }

  private submitModuleRequest<T>(request: Observable<T>, successMessage: string): void {
    this.savingModule = true;
    this.moduleError = '';
    request.pipe(finalize(() => this.savingModule = false)).subscribe({
      next: () => {
        this.showModuleForm = false;
        this.basket = [];
        this.moduleMessage = successMessage;
        if (['purchases', 'sales', 'reservations', 'inventory'].includes(this.activeMenu)) this.load();
        this.openModule(this.activeMenu as Extract<BusinessModuleKey, 'quotes' | 'purchases' | 'cash' | 'sales' | 'reservations' | 'inventory' | 'receipts'>);
      },
      error: error => this.moduleError = this.apiMessage(error, 'No se pudo guardar la operación.')
    });
  }

  private apiMessage(error: { error?: { message?: string } }, fallback: string): string {
    return error.error?.message ?? fallback;
  }

  private saveBlob(file: Blob, filename: string): void {
    const url = URL.createObjectURL(file);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  closeModuleForm(): void {
    this.showModuleForm = false;
    this.basket = [];
  }

  suggestUsername(): void {
    const username = this.userForm.controls.username;
    if (username.dirty) return;
    const suggestion = this.userForm.controls.fullName.value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9._-]+/g, '.')
      .replace(/^[.-]+|[.-]+$/g, '')
      .slice(0, 50);
    username.setValue(suggestion);
  }

  generateTemporaryPassword(): void {
    const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%';
    const randomValues = crypto.getRandomValues(new Uint32Array(18));
    const password = Array.from(randomValues, value => alphabet[value % alphabet.length]).join('');
    this.userForm.controls.password.setValue(password);
    this.passwordVisible = true;
  }

  createUser(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }
    const formValue = this.userForm.getRawValue();
    const request: AccountRequest = {
      ...formValue,
      permissions: formValue.role === 'ADMIN' ? [] : formValue.permissions
    };
    this.savingUser = true;
    this.userError = '';
    this.auth.createUser(request).pipe(finalize(() => this.savingUser = false)).subscribe({
      next: user => {
        this.users = [...this.users, user].sort((left, right) => left.fullName.localeCompare(right.fullName));
        this.userMessage = `Se creó el perfil de ${user.role === 'ADMIN' ? 'administrador' : 'empleado'} ${user.fullName}.`;
        this.userForm.reset({ fullName: '', username: '', password: '', role: 'EMPLOYEE', permissions: [] });
        this.passwordVisible = false;
      },
      error: error => this.userError = error?.error?.message ?? 'No se pudo crear el perfil.'
    });
  }

  onUserRoleChange(): void {
    if (this.userForm.controls.role.value === 'ADMIN') {
      this.userForm.controls.permissions.setValue([]);
      this.permissionsOpen = false;
    }
  }

  togglePermission(permission: PermissionCode): void {
    const current = this.userForm.controls.permissions.value;
    const next = current.includes(permission)
      ? current.filter(value => value !== permission)
      : [...current, permission];
    this.userForm.controls.permissions.setValue(next);
  }

  toggleUserStatus(user: Account): void {
    const action = user.enabled ? 'quitar el acceso a' : 'restaurar el acceso de';
    if (!confirm(`¿Deseas ${action} ${user.fullName}? Su historial se conservará.`)) return;
    this.userError = '';
    this.auth.setUserEnabled(user.id, !user.enabled).subscribe({
      next: updated => {
        this.users = this.users.map(existing => existing.id === updated.id ? updated : existing);
        this.userMessage = `El perfil de ${updated.fullName} ahora está ${updated.enabled ? 'activo' : 'inactivo'}.`;
      },
      error: error => this.userError = error?.error?.message ?? 'No se pudo actualizar el perfil.'
    });
  }

  setCategory(event: Event): void {
    this.selectedCategory = (event.target as HTMLSelectElement).value;
    this.productPage = 1;
  }

  setAvailability(filter: 'all' | 'available' | 'out-of-stock'): void {
    this.availabilityFilter = filter;
    this.productPage = 1;
  }

  updateSearch(event: Event): void {
    this.searchTerm = (event.target as HTMLInputElement).value;
    this.productPage = 1;
  }

  openCreateForm(): void {
    this.editingId = null;
    this.form.reset({ sku: '', name: '', category: '', description: '', price: 0, stock: 0, minStock: 5, supplier: '' });
    this.showForm = true;
    this.error = '';
  }

  editProduct(product: Product): void {
    this.editingId = product.id;
    this.form.reset({
      sku: product.sku,
      name: product.name,
      category: product.category,
      description: product.description ?? '',
      price: product.price,
      stock: product.stock,
      minStock: product.minStock,
      supplier: product.supplier ?? ''
    });
    this.showForm = true;
    this.error = '';
  }

  closeForm(): void {
    this.showForm = false;
    this.editingId = null;
    this.form.reset({ sku: '', name: '', category: '', description: '', price: 0, stock: 0, minStock: 5, supplier: '' });
  }

  saveProduct(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const request: ProductRequest = this.form.getRawValue();
    this.saving = true;
    this.error = '';
    const saveRequest = this.editingId === null
      ? this.service.create(request)
      : this.service.update(this.editingId, request);
    saveRequest.pipe(finalize(() => this.saving = false)).subscribe({
      next: () => {
        this.message = this.editingId === null ? 'Producto agregado correctamente.' : 'Producto actualizado correctamente.';
        this.closeForm();
        this.load();
        this.clearMessage();
      },
      error: error => this.error = error?.error?.message ?? 'No se pudo guardar el producto.'
    });
  }

  removeProduct(product: Product): void {
    if (!confirm(`¿Eliminar "${product.name}" del inventario? Si ya tiene operaciones asociadas, se conservará para proteger el historial.`)) return;
    this.error = '';
    this.service.delete(product.id).subscribe({
      next: () => {
        this.message = 'Producto eliminado correctamente.';
        this.load();
        this.clearMessage();
      },
      error: error => {
        this.error = error?.status === 409
          ? 'No se puede eliminar este producto porque está asociado a operaciones comerciales existentes. Se conservó para proteger el historial.'
          : error?.error?.message ?? 'No se pudo eliminar el producto.';
      }
    });
  }

  downloadReport(): void {
    this.downloadingReport = true;
    this.error = '';
    this.moduleError = '';
    this.moduleMessage = '';
    this.service.downloadReport().pipe(finalize(() => this.downloadingReport = false)).subscribe({
      next: file => {
        const url = URL.createObjectURL(file);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'reporte-inventario.pdf';
        link.click();
        URL.revokeObjectURL(url);
        if (this.activeMenu === 'report') {
          this.moduleMessage = 'Reporte PDF generado y descargado.';
        } else {
          this.message = 'Reporte de inventario descargado.';
          this.clearMessage();
        }
      },
      error: () => {
        const reportError = 'No se pudo generar el reporte. Intenta nuevamente.';
        if (this.activeMenu === 'report') this.moduleError = reportError;
        else this.error = reportError;
      }
    });
  }

  downloadExcel(): void {
    this.downloadingExcel = true;
    this.error = '';
    this.moduleError = '';
    this.moduleMessage = '';
    this.service.downloadExcel().pipe(finalize(() => this.downloadingExcel = false)).subscribe({
      next: file => {
        const url = URL.createObjectURL(file);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'reporte-inventario.xlsx';
        link.click();
        URL.revokeObjectURL(url);
        if (this.activeMenu === 'report') {
          this.moduleMessage = 'Reporte Excel generado y descargado.';
        } else {
          this.message = 'Reporte Excel descargado.';
          this.clearMessage();
        }
      },
      error: () => {
        const reportError = 'No se pudo generar el reporte Excel. Intenta nuevamente.';
        if (this.activeMenu === 'report') this.moduleError = reportError;
        else this.error = reportError;
      }
    });
  }

  productColor(category: string): string {
    const colors = ['#ede9fe', '#e0f2fe', '#dcfce7', '#ffedd5', '#fce7f3'];
    const index = Array.from(category).reduce((sum, character) => sum + character.charCodeAt(0), 0) % colors.length;
    return colors[index];
  }

  private clearMessage(): void {
    window.setTimeout(() => this.message = '', 3500);
  }

  // Depreciation methods
  openDepreciation(asset: AssetRecord): void {
    this.selectedAssetForDepreciation = asset;
    this.showDepreciationForm = true;
    this.depreciationError = '';
    this.depreciationMessage = '';
    this.assetDepreciationHistory = [];
    this.depreciationReport = null;
    this.singleAssetDepreciationDate = this.localDateString();
    this.singleAssetProcessedBy = this.currentUser?.fullName ?? '';
    this.loadAssetDepreciationHistory(asset.id);
  }

  closeDepreciationForm(): void {
    this.showDepreciationForm = false;
    this.selectedAssetForDepreciation = null;
    this.depreciationError = '';
    this.depreciationMessage = '';
  }

  loadAssetDepreciationHistory(assetId: number): void {
    this.depreciationLoading = true;
    this.business.getAssetDepreciationHistoryByAsset(assetId).pipe(
      finalize(() => this.depreciationLoading = false)
    ).subscribe({
      next: history => this.assetDepreciationHistory = history,
      error: error => this.depreciationError = this.apiMessage(error, 'No se pudo cargar el historial de depreciación.')
    });
  }

  calculateSingleAssetDepreciation(): void {
    if (!this.selectedAssetForDepreciation || !this.singleAssetDepreciationDate) {
      this.depreciationError = 'Selecciona una fecha de depreciación.';
      return;
    }
    this.depreciationLoading = true;
    this.depreciationError = '';
    const request: DepreciationCalculateRequest = {
      assetId: this.selectedAssetForDepreciation.id,
      depreciationDate: this.singleAssetDepreciationDate,
      processedBy: this.singleAssetProcessedBy || (this.currentUser?.fullName ?? 'Sistema')
    };
    this.business.calculateAssetDepreciation(this.selectedAssetForDepreciation.id, request).pipe(
      finalize(() => this.depreciationLoading = false)
    ).subscribe({
      next: record => {
        this.depreciationMessage = `Depreciación calculada: ${new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP' }).format(record.monthlyDepreciation)} mensual.`;
        this.loadAssetDepreciationHistory(this.selectedAssetForDepreciation!.id);
        this.loadAssets();
        this.loadDepreciationValuation();
      },
      error: error => this.depreciationError = this.apiMessage(error, 'No se pudo calcular la depreciación.')
    });
  }

  openBatchDepreciation(): void {
    this.showDepreciationForm = true;
    this.selectedAssetForDepreciation = null;
    this.depreciationError = '';
    this.depreciationMessage = '';
    this.assetDepreciationHistory = [];
    this.depreciationReport = null;
    this.batchDepreciationDate = this.localDateString();
    this.batchProcessedBy = this.currentUser?.fullName ?? '';
  }

  runBatchDepreciation(): void {
    if (!this.batchDepreciationDate) {
      this.depreciationError = 'Selecciona una fecha de depreciación.';
      return;
    }
    this.depreciationLoading = true;
    this.depreciationError = '';
    const request: DepreciationBatchRequest = {
      depreciationDate: this.batchDepreciationDate,
      processedBy: this.batchProcessedBy || (this.currentUser?.fullName ?? 'Sistema')
    };
    this.business.depreciateAll(request).pipe(
      finalize(() => this.depreciationLoading = false)
    ).subscribe({
      next: report => {
        this.depreciationReport = report;
        this.depreciationMessage = `Proceso batch completado: ${report.totalAssets} activos, depreciación total: ${new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP' }).format(report.totalMonthlyDepreciation)}.`;
        this.loadAssets();
        this.loadDepreciationValuation();
      },
      error: error => this.depreciationError = this.apiMessage(error, 'No se pudo ejecutar la depreciación por lote.')
    });
  }

  loadPeriodReport(): void {
    this.depreciationLoading = true;
    this.depreciationError = '';
    this.business.getDepreciationReport(this.depreciationYear, this.depreciationMonth).pipe(
      finalize(() => this.depreciationLoading = false)
    ).subscribe({
      next: report => {
        this.depreciationReport = report;
        this.depreciationMessage = `Reporte cargado: ${report.totalAssets} registros.`;
      },
      error: error => this.depreciationError = this.apiMessage(error, 'No se pudo cargar el reporte de depreciación.')
    });
  }

  downloadDepreciationExcel(): void {
    this.downloadingDepreciationExcel = true;
    this.depreciationError = '';
    this.depreciationMessage = '';
    this.business.downloadDepreciationExcel()
      .pipe(finalize(() => this.downloadingDepreciationExcel = false))
      .subscribe({
        next: file => {
          const url = URL.createObjectURL(file);
          const link = document.createElement('a');
          link.href = url;
          link.download = 'reporte-depreciacion-activos.xlsx';
          link.click();
          URL.revokeObjectURL(url);
          this.depreciationMessage = 'Reporte de depreciación Excel generado y descargado.';
        },
        error: error => this.depreciationError = this.apiMessage(error, 'No se pudo generar el reporte Excel de depreciación.')
      });
  }

  formatCurrency(value: number | null | undefined): string {
    if (value == null) return '—';
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP' }).format(value);
  }

  formatPercent(value: number | null | undefined): string {
    return value == null ? '—' : `${new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 }).format(value * 100)} %`;
  }

  formatDate(dateStr: string): string {
    return new Intl.DateTimeFormat('es-CO', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(dateStr));
  }

  get apiBaseUrl(): string {
    return `${window.location.protocol}//${window.location.hostname}:8080`;
  }

  submitApiRequest(): void {
    if (this.currentUser?.role !== 'ADMIN') {
      this.apiError = 'Solo un administrador puede usar el explorador de API.';
      return;
    }
    if (this.apiForm.invalid) {
      this.apiForm.markAllAsTouched();
      this.apiError = 'Indica una ruta relativa válida de la API local.';
      return;
    }
    const { method, path, body } = this.apiForm.getRawValue();
    const trimmedPath = path.trim();
    if (!trimmedPath.startsWith('/') || trimmedPath.startsWith('//') || trimmedPath.includes('..') || trimmedPath.includes('\\')) {
      this.apiError = 'La ruta debe pertenecer a la API local.';
      return;
    }
    let parsedBody: unknown;
    if (body.trim()) {
      if (method === 'GET') {
        this.apiError = 'Las solicitudes GET no admiten un cuerpo JSON.';
        return;
      }
      try { parsedBody = JSON.parse(body); }
      catch { this.apiError = 'El cuerpo debe contener JSON válido.'; return; }
    }
    this.apiError = '';
    this.apiResponse = null;
    this.apiBusy = true;
    this.business.executeApiRequest(method, trimmedPath, parsedBody)
      .pipe(finalize(() => this.apiBusy = false))
      .subscribe({
        next: response => this.apiResponse = { method, path: trimmedPath, status: response.status, statusText: response.statusText, body: this.formatApiBody(response.body), failed: false },
        error: error => {
          const bodyText = error instanceof HttpErrorResponse && typeof error.error === 'string' ? error.error : error?.message ?? 'Error';
          this.apiResponse = { method, path: trimmedPath, status: error?.status ?? 0, statusText: error?.statusText ?? 'Error', body: this.formatApiBody(bodyText), failed: true };
        }
      });
  }

  private formatApiBody(body: string | null): string {
    if (!body) return 'Sin contenido en la respuesta.';
    try { return JSON.stringify(JSON.parse(body), null, 2); }
    catch { return body; }
  }
getDepreciationStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    ACTIVE: 'Activo',
    FULLY_DEPRECIATED: 'Totalmente Depreciado',
    DISPOSED: 'Baja'
  };
  return labels[status] ?? status;
}

getInventoryDepreciationStatus(asset: AssetRecord): string {
  const valuation = this.assetValuations.find(item => item.id === asset.id);
  return this.getDepreciationStatusLabel(valuation?.assetStatus ?? asset.assetStatus ?? 'ACTIVE');
}

private localDateString(): string {
  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
}
