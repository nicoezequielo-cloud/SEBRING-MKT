# Sebring — Automotive Market

Marketplace de vehículos con temática de circuito (SEBRING). Backend **Spring Boot 3 + PostgreSQL** y frontend **React + Vite**.

## Stack

- **Backend**: Java 21, Spring Boot, Spring Data JPA, Hibernate, Maven, PostgreSQL.
- **Frontend**: React, Vite, CSS propio (tema dark + rojo F1).
- **Auth**: sesiones en memoria con token Bearer y filtros por rol (`admin` / comprador).

## Funcionalidades

- Catálogo de vehículos con búsqueda, **filtros por marca, rango de precio y orden por precio**.
- Detalle de vehículo con galería de fotos y acciones de compra / reserva / test drive.
- **Login por email o nombre de usuario + contraseña**, registro público (crea compradores).
- **Perfil editable** (nombre, apellido, teléfono, DNI, contraseña) para ambos roles.
- **Panel admin**: alta/edición de vehículos (prefill con botón "Guardar cambios"), alta de compradores, y gestión de imágenes (subir, reordenar, fijar portada, quitar).
- **Permisos por rol** en el servidor: matriceds de endpoints verificada (público, comprador, admin).

## Permisos (resumen)

| Acción | Anónimo | Comprador | Admin |
|---|---|---|---|
| Ver vehículos / transacciones (propias) | L/E | Sí | Sí |
| Comprar / reservar / test drive | — | Sí | Sí |
| Crear / borrar vehículos, imágenes, usuarios | — | — | Sí |
| Editar perfil propio | — | Sí | Sí |
| Editar perfil de otro / roles | — | — | Sí |

El rol no se puede autoconfirmar desde el perfil (aunque un comprador intente enviarlo).

## Cómo correrlo

### 1. Base de datos (PostgreSQL)

Crea la base `sebring`. El acceso por defecto está en `src/main/resources/application.properties`:

```
jdbc:postgresql://localhost:5432/sebring
usuario: postgres / password: admin
```

### 2. Backend

```bash
mvn -q -DskipTests package
java -jar target/sebring-1.0-SNAPSHOT.jar
```

El seed de ejemplo (`Main.java`) crea las tablas y carga autos, transacciones y usuarios de prueba.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Abrí `http://localhost:5173` (el proxy reenvía `/api` y `/uploads` al backend en `:8080`).

## Cuentas de prueba

| Rol | Email | Usuario | Password |
|---|---|---|---|
| Comprador | alice@example.com | `alice` | secret123 |
| Admin | bob@example.com | `bob` | secret456 |
| Comprador | carla.gomez@example.com | `carla` | carla2024 |

## Estructura

```
src/main/java/org/example/
  controller/   # REST controllers (vehicles, users, transactions, auth, images)
  security/     # AuthFilter, SessionService, AuthSupport
  service/      # Lógica de negocio
  model/        # Entidades JPA (User, Vehicle, VehicleImage, Transaction)
  dto/          # Request/response (incluye LoginRequest con email o username)
  exception/    # Manejadores de error (401/403/404/409/422)
frontend/
  src/          # App, componentes, api.js, index.css
```

## Nota

Las credenciales de base de datos y el archivo `Main.java` contienen credenciales de desarrollo. Cambialas antes de exponer el proyecto en producción.