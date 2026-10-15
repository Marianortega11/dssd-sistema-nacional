# Sistema Nacional de Gestión de Recursos y Riesgos

API REST (Spring Boot 3.4 / Java 17 / PostgreSQL) con autenticación JWT. Es el "Sistema Nacional" externo de RescueSync:
Bonita la invoca para **validar** ofertas por niveles de competencia, **bloquear** recursos al adjudicar y **liberarlos** al cierre.

- Swagger UI: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`
- Health (para Render): `/actuator/health`

## Endpoints

| Área | Método y ruta | Rol |
|---|---|---|
| 1. Auth | `POST /api/auth/login` | público |
| 1. Auth | `POST /api/auth/register` (onboarding de ONG, queda pendiente de habilitación) | público |
| 2. Catálogo | `GET /api/certificaciones`, `GET /api/ongs[?habilitada=]`, `GET /api/ongs/{id}`, `GET /api/ongs/{id}/recursos` | autenticado |
| 2. Catálogo | `GET /api/recursos/disponibilidad[?tipoRecurso=]` (global, ONGs habilitadas) | autenticado |
| 2. Gestión | `PATCH /api/ongs/{id}/habilitacion`, `POST /api/ongs/{id}/certificaciones` | ADMIN |
| 2. Gestión | `PUT /api/ongs/{id}/recursos` (cargar inventario) | ADMIN o la propia ONG |
| 3. Competencia | `POST /api/validacion/competencias` | SISTEMA / ADMIN |
| 4. Bloqueo | `POST /api/recursos/validar-restricciones` (sin bloquear) | SISTEMA / ADMIN |
| 4. Bloqueo | `POST /api/recursos/bloqueos` (201 crea, 200 ya existía, 409 insuficiente) | SISTEMA / ADMIN |
| 5. Cierre | `POST /api/proyectos/{ref}/finalizacion` (una ONG terminó) | SISTEMA / ADMIN |
| 5. Cierre | `POST /api/proyectos/{ref}/cierre` (libera todo el proyecto) · `GET /api/proyectos/{ref}/compromisos` | SISTEMA / ADMIN · autenticado |

`proyectoRef` es el id de la emergencia en la app local. La ONG se identifica con `ong`: su **alias local**
(el username de Bonita, ej. `april.sanchez`) o su **CUIT**.

### Validación por niveles (sin rechazo binario)

`POST /api/validacion/competencias` con `{ "ong": "april.sanchez", "gravedad": "ALTO", "items": [{ "tipoRecurso": "Paramédicos", "cantidad": 5 }] }`
devuelve por recurso el `nivel` (1–3), el `riesgoMaximoPermitido` y el `rolPermitido` (`PRINCIPAL`, `APOYO`, `NO_HABILITADA`),
más un `resultado` global (`HABILITADA_PRINCIPAL`, `HABILITACION_PARCIAL`, `HABILITADA_APOYO`, `NO_HABILITADA`) y `recomendaciones`.
`nivelHabilitacion` global puede guardarse en `OfertaAyuda.nivelHabilitacion`. La gravedad acepta texto libre (Baja/Media/Alta/Crítica).

### Concurrencia

`/api/recursos/bloqueos` corre en una transacción que toma `SELECT ... FOR UPDATE` sobre la fila de la ONG y luego sobre sus
filas de inventario (orden estable → sin deadlocks). Dos adjudicaciones simultáneas sobre el mismo recurso se serializan y la
segunda recibe `409` con el faltante. Es idempotente por `(proyectoRef, ONG)`: los reintentos de Bonita no descuentan dos veces.
Liberación y cierre también son idempotentes. Hay un test con 10 hilos (`ConcurrenciaBloqueoTest`).

## Usuarios de demo (seed)

| Usuario | Rol | Contraseña |
|---|---|---|
| `admin` | ADMIN | la definida en `ADMIN_PASSWORD` |
| `bonita` | SISTEMA (usar desde Bonita) | la definida en `SISTEMA_PASSWORD` |
| `cruzroja` (alias `april.sanchez`) | ONG | `demo12345` (`DEMO_PASSWORD`) |
| `bomberos` (alias `helen.kelly`) | ONG | `demo12345` |

La app no tiene valores por defecto para `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD` ni `SISTEMA_PASSWORD`: si falta
alguno no arranca. En Render el blueprint los genera (se ven en **Environment** del servicio). Si no se quieren ONGs de
ejemplo, `SEED_DEMO=false`.

## Ejecutar

```bash
# 1. Crear el .env a partir del ejemplo y completar los valores (no se sube al repo)
cp .env.example .env

# 2. Local con Docker (API en http://localhost:8090/swagger-ui.html)
docker compose up --build

# Tests (usan H2 en modo PostgreSQL)
./mvnw test
```

Variables obligatorias: `DB_PASSWORD`, `JWT_SECRET` (≥32 caracteres), `ADMIN_PASSWORD`, `SISTEMA_PASSWORD`.
Opcionales: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `JWT_EXPIRATION_MINUTES`, `DEMO_PASSWORD`, `SEED_DEMO`, `PORT`.
Para correr sin Docker (`./mvnw spring-boot:run`) hay que definirlas en la terminal o en el IDE: Spring no lee el `.env`.

## Despliegue en Render

1. Subir este repo a GitHub.
2. En Render: **New → Blueprint** y elegir el repo (usa `render.yaml`: servicio Docker + PostgreSQL gratuito).
3. Al terminar, la API queda en `https://<servicio>.onrender.com/swagger-ui.html`. (El plan gratuito duerme tras inactividad:
   la primera llamada de Bonita puede tardar ~1 min.)

## Cómo lo usa Bonita

1. `POST /api/auth/login` con la cuenta `bonita` → guardar el `accessToken`.
2. Tras vencer el timer y leer las ofertas del backend local: `POST /api/validacion/competencias` por oferta/ONG.
3. Al adjudicar: `POST /api/recursos/bloqueos` por cada ONG adjudicada.
4. Al cerrar la ejecución: `POST /api/proyectos/{emergenciaId}/cierre` (o `/finalizacion` por ONG).
