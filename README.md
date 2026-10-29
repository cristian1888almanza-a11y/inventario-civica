# La Cívica Inventario

Sistema de inventario **La Cívica Inventario**, con dashboard responsive en Angular y API REST en Java/Spring Boot. Incluye inicio de sesión, perfiles individuales de empleados y un administrador principal que puede crear o desactivar cuentas. También permite registrar y editar productos, buscar y filtrar existencias, revisar alertas de stock mínimo, consultar indicadores y descargar reportes PDF generados con JasperReports. La plantilla está en formato JRXML y puede editarse con Jaspersoft Studio o iReport.

## Tecnologías

- Java 25, Spring Boot 3.5, Spring Data JPA y Bean Validation
- MySQL para instalaciones persistentes; H2 en memoria para desarrollo rápido y pruebas
- JasperReports 6.21
- Angular 20, TypeScript y Reactive Forms

## Iniciar en desarrollo (H2)

Requisitos: Java 25, Maven 3.9+ y Node.js 22+.

```bash
cd backend
mvn spring-boot:run
```

En otra terminal:

```bash
cd frontend
npm install
npm start
```

Abre `http://localhost:4200`. La configuración predeterminada usa H2 en un archivo local (`backend/data/taskdb`) para conservar perfiles, productos y actividad cuando se reinicia el backend. La consola de desarrollo está en `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/taskdb`, usuario `sa`, contraseña vacía). El archivo local de datos no se incluye en Git.

En la primera visita, crea el perfil del administrador principal. Es la única ocasión en la que se permite el alta pública de una cuenta; después, solo el administrador autenticado puede dar de alta empleados desde **Usuarios**. Las contraseñas requieren al menos 12 caracteres y se almacenan con BCrypt. El administrador puede desactivar empleados; solo las cuentas activas pueden iniciar sesión.

## Usar MySQL

Crea o usa una instancia local de MySQL y activa el perfil `mysql`:

```bash
cd backend
$env:DB_URL="jdbc:mysql://localhost:3306/inventorydb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="tu-contrasena"
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

También puedes definir `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` en el entorno antes de iniciar la aplicación. Hibernate crea o actualiza las tablas al iniciar (`ddl-auto=update`); para producción, reemplaza esta estrategia por migraciones controladas.

## Activos fijos y depreciación

En **Gestión de activos** puedes consultar valores históricos y en libros a una fecha de corte, registrar depreciación mensual y filtrar por categoría, fechas, estado, responsable y ubicación. La vida útil y la tasa anual se guardan en `asset_categories`; los saldos y asientos se conservan en `assets` y `depreciation_records`. El cálculo es de línea recta con redondeo a dos decimales y no modifica el valor histórico del activo.

Las categorías iniciales se crean automáticamente al iniciar el backend. Para aplicar manualmente la precisión de tasa y sembrar esas categorías en una base existente, ejecuta el script de esquema correspondiente (`backend/db/asset-depreciation-h2.sql` o `backend/db/asset-depreciation-mysql.sql`) y después `backend/db/asset-depreciation-categories.sql`. Los inserts de categorías son idempotentes.

## Inventario

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/products` | Listar productos |
| `GET` | `/products/{id}` | Consultar producto |
| `GET` | `/products/summary` | Indicadores y alertas de inventario |
| `GET` | `/products/report` | Descargar PDF JasperReports |
| `POST` | `/products` | Crear producto |
| `PUT` | `/products/{id}` | Actualizar producto |
| `DELETE` | `/products/{id}` | Eliminar producto |

## Perfiles y acceso

La autenticación usa sesiones y protección CSRF con cookie. La aplicación crea el primer perfil como `ADMIN`; el endpoint de alta inicial se cierra automáticamente después de esa cuenta. El administrador gestiona empleados mediante:

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/auth/setup-status` | Indicar si falta configurar el administrador inicial |
| `GET` | `/auth/csrf` | Obtener el token CSRF |
| `POST` | `/auth/setup` | Crear el administrador inicial (disponible una sola vez) |
| `POST` | `/auth/login` | Iniciar sesión |
| `GET` | `/auth/me` | Consultar el perfil de la sesión actual |
| `POST` | `/auth/logout` | Cerrar sesión |
| `GET` | `/users` | Listar cuentas (solo administrador) |
| `POST` | `/users` | Crear un perfil de empleado (solo administrador) |
| `PATCH` | `/users/{id}/status?enabled=true|false` | Activar o desactivar un empleado (solo administrador) |
| `GET` | `/admin/activity` | Consultar las 300 acciones recientes del equipo (solo administrador) |

Los perfiles `EMPLOYEE` tienen acceso al inventario; la administración de cuentas, la consulta de actividad y la consola de API quedan reservadas al perfil `ADMIN`. Quitar el acceso desactiva al empleado sin eliminar su perfil ni su historial; el administrador puede restaurar el acceso después. El registro de actividad guarda el usuario, el nombre del perfil, la acción, el detalle y la fecha para operaciones de inventario, caja, cotizaciones, compras, ventas, apartados y cuentas. El alta de cuentas posteriores siempre crea perfiles de empleado, por lo que la aplicación conserva un único administrador principal.

La opción **API**, visible solo para el administrador, permite enviar solicitudes autenticadas a rutas relativas del backend local con los métodos `GET`, `POST`, `PUT`, `PATCH` y `DELETE`. Las solicitudes que modifican datos piden confirmación; el backend aplica sus permisos y validaciones habituales. No admite enviar solicitudes a servidores externos ni editar encabezados de autenticación.

Ejemplo para crear o actualizar un producto:

```json
{
  "sku": "AUD-001",
  "name": "Audífonos inalámbricos",
  "category": "Electrónica",
  "description": "Audífonos Bluetooth",
  "price": 49.99,
  "stock": 18,
  "minStock": 5,
  "supplier": "Audio Centro"
}
```

El SKU es único y se normaliza a mayúsculas. El indicador de stock bajo cuenta los productos cuya existencia es igual o inferior al mínimo configurado. El valor del inventario se calcula como precio unitario por unidades en existencia.

El diseño del PDF se define en `backend/src/main/resources/reports/inventory-report.jrxml`. Puede abrirse y ajustarse en Jaspersoft Studio/iReport; después de modificarlo, reinicia el backend para utilizar la plantilla actualizada.

## Pruebas

```bash
cd backend
mvn test
```

La suite comprueba las operaciones CRUD, validaciones, resumen de existencias, SKU duplicado y generación del PDF Jasper.
