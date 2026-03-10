# ARQUITECTURA - RESUMEN

## Diagrama General

```
                    ┌─────────────────┐
                    │  Cliente/Postman│
                    └────────┬────────┘
                             │ HTTP:8080
                    ┌────────▼────────┐
                    │  API GATEWAY    │
                    │    (8080)       │
                    └────────┬────────┘
                      /      │      \
         ┌────────────┴───┬──┴──┬─────────┐
         │                │     │         │
    ┌────▼──┐        ┌───▼──┐  │   ┌────▼──┐
    │PRODUCT│        │ORDER │  │   │PAYMENT│
    │(8081) │        │(8082)│  │   │(8083) │
    └────┬──┘        └───┬──┘  │   └────┬──┘
         │               │     │        │
    ┌────▼──────┐   ┌───▼──────┐ ┌────▼──┐
    │MongoDB    │   │MongoDB   │ │MongoDB│
    │:27017     │   │:27018    │ │:27019 │
    └───────────┘   └──────────┘ └───────┘

    ┌────────────────────────────────────┐
    │  EUREKA SERVER (8761)              │
    │ Service Discovery (registro central) │
    └────────────────────────────────────┘

    ┌─────────────────────────────────┐
    │ CloudWatch (LocalStack :4566)   │
    │ 5 Log Groups (logs de servicios)│
    └─────────────────────────────────┘
```

## Componentes

### 1. API Gateway (Puerto 8080)
- Punto de entrada único para todas las solicitudes
- Ruta automática a servicios registrados en Eureka
- Rutas:
  - `/productos/**` → productservice:8081
  - `/ordenes/**` → orderservice:8082
  - `/pagos/**` → paymentservice:8083

### 2. Eureka Server (Puerto 8761)
- Service Discovery central
- Todos los servicios se auto-registran al iniciar
- El Gateway consulta Eureka para encontrar servicios

### 3. Microservicios (3)

**Product Service (8081)**
- MongoDB: puerto 27017, BD: productos_db
- Endpoints: GET, POST, PUT, DELETE /productos-api

**Order Service (8082)**
- MongoDB: puerto 27018, BD: ordenes_db  
- Endpoints: POST, GET, PUT /ordenes-api

**Payment Service (8083)**
- MongoDB: puerto 27019, BD: pagos_db
- Endpoints: POST, GET, PUT /pagos-api

### 4. MongoDB (3 instancias independientes)
- Cada servicio con su base de datos
- Credenciales: root / rootpassword
- Persistencia en volúmenes

### 5. CloudWatch via LocalStack (4566)
- Simula AWS para desarrollo
- 5 log groups (uno por servicio)
- Los logs se envían automáticamente

## Flujo de una solicitud

```
1. Cliente: GET /productos
   ↓
2. API Gateway (8080) recibe
   ↓
3. Eureka: "¿Dónde está productservice?"
   ↓
4. Eureka: "En productservice:8081"
   ↓
5. Gateway → GET productservice:8081/productos-api
   ↓
6. ProductService → MongoDB:27017
   ↓
7. MongoDB → Devuelve documentos
   ↓
8. ProductService → Log a CloudWatch
   ↓
9. ProductService → Response al Gateway
   ↓
10. Gateway → Response al cliente
```

## Tecnologías

| Componente | Versión |
|-----------|---------|
| Spring Boot | 3.3.4 |
| Spring Cloud | 2023.0.0 |
| MongoDB | 7.0 |
| Docker | Actual |
| LocalStack | Actual |

## Puertos

| Servicio | Puerto |
|----------|--------|
| LocalStack | 4566 |
| MongoDB Productos | 27017 |
| MongoDB Órdenes | 27018 |
| MongoDB Pagos | 27019 |
| Eureka Server | 8761 |
| API Gateway | 8080 |
| Product Service | 8081 |
| Order Service | 8082 |
| Payment Service | 8083 |
