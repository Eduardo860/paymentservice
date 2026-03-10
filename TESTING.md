# TESTING Y EJECUCIÓN

## Requisitos previos

- Docker Desktop corriendo
- Docker Compose 3.9+
- Terminal/CLI

## Paso 1: Levantar el sistema completo

Desde la raíz del proyecto:

```bash
docker-compose up -d
```

Espera 2-3 minutos a que todo esté saludable.

## Paso 2: Verificar que el sistema está arriba

### Opción A: Ver contenedores corriendo

```bash
docker ps
```

Deberías ver 10 contenedores:
- infra-localstack
- mongo-productos, mongo-ordenes, mongo-pagos
- infra-eureka-server
- infra-apigateway
- infra-productservice, infra-orderservice, infra-paymentservice

### Opción B: Verificar Eureka Dashboard

Abre en navegador: **http://localhost:8761**

Deberías ver 4 servicios registrados (el Gateway se registra después):
- PRODUCTSERVICE
- ORDERSERVICE
- PAYMENTSERVICE
- APIGATEWAY (después de 30-60 segundos)

### Opción C: Test con curl

```bash
curl http://localhost:8080/actuator/health
```

Respuesta esperada:
```json
{"status":"UP"}
```

---

## Pruebas manuales de endpoints

### 1. PRODUCTS - Crear un producto

```bash
curl -X POST http://localhost:8080/productos \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15",
    "description": "Latest iPhone",
    "price": 1299.99,
    "stock": 50
  }'
```

Respuesta: ID del producto creado

**Guardar el ID para próximos tests.**

### 2. PRODUCTS - Listar todos

```bash
curl http://localhost:8080/productos
```

Deberías ver un array con todos los productos (incluyendo el que creaste).

### 3. PRODUCTS - Obtener uno por ID

```bash
curl http://localhost:8080/productos/{ID_DEL_PRODUCTO}
```

Reemplaza `{ID_DEL_PRODUCTO}` con el ID que guardaste.

### 4. ORDERS - Crear una orden

```bash
curl -X POST http://localhost:8080/ordenes \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "totalAmount": 1299.99,
    "status": "PENDING"
  }'
```

Respuesta: ID de la orden

**Guardar el ID para el siguiente test.**

### 5. ORDERS - Listar orden

```bash
curl http://localhost:8080/ordenes/{ID_DE_LA_ORDEN}
```

### 6. ORDERS - Listar por usuario

```bash
curl http://localhost:8080/ordenes/usuario/1
```

### 7. PAYMENTS - Procesar pago

```bash
curl -X POST http://localhost:8080/pagos/procesar \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "{ID_DE_LA_ORDEN}",
    "amount": 1299.99,
    "paymentMethod": "CREDIT_CARD"
  }'
```

Reemplaza `{ID_DE_LA_ORDEN}` con el ID de la orden.

Respuesta: ID del pago

**Guardar el ID para el siguiente test.**

### 8. PAYMENTS - Obtener pago

```bash
curl http://localhost:8080/pagos/{ID_DEL_PAGO}
```

### 9. PAYMENTS - Obtener pagos por orden

```bash
curl http://localhost:8080/pagos/orden/{ID_DE_LA_ORDEN}
```

### 10. PAYMENTS - Procesar reembolso

```bash
curl -X PUT http://localhost:8080/pagos/{ID_DEL_PAGO}/reembolso
```

---

## Verificar logs

### Logs de consola en tiempo real

```bash
# Product Service
docker logs infra-productservice -f

# Order Service
docker logs infra-orderservice -f

# Payment Service
docker logs infra-paymentservice -f

# API Gateway
docker logs infra-apigateway -f

# Eureka
docker logs infra-eureka-server -f
```

### Logs en CloudWatch (LocalStack)

```bash
# Listar log groups
aws logs describe-log-groups --endpoint-url=http://localhost:4566

# Ver logs del servicio de productos
aws logs get-log-events \
  --log-group-name=producto-log-group \
  --log-stream-name=producto-service-stream \
  --endpoint-url=http://localhost:4566
```

---

## Acceder a MongoDB

### Con MongoDB Compass (GUI)

1. Descarga MongoDB Compass desde: https://www.mongodb.com/products/tools/compass
2. Crea 3 conexiones:

**Conexión 1 - Productos**
- URI: `mongodb://root:rootpassword@localhost:27017/productos_db`

**Conexión 2 - Órdenes**
- URI: `mongodb://root:rootpassword@localhost:27018/ordenes_db`

**Conexión 3 - Pagos**
- URI: `mongodb://root:rootpassword@localhost:27019/pagos_db`

### Con mongosh (CLI)

```bash
# Conectar a Productos
docker exec -it mongo-productos mongosh \
  -u root -p rootpassword \
  --authenticationDatabase admin \
  --eval "db.products.find()"

# Conectar a Órdenes
docker exec -it mongo-ordenes mongosh \
  -u root -p rootpassword \
  --authenticationDatabase admin \
  --eval "db.orders.find()"

# Conectar a Pagos
docker exec -it mongo-pagos mongosh \
  -u root -p rootpassword \
  --authenticationDatabase admin \
  --eval "db.payments.find()"
```

---

## Detener el sistema

### Opción 1: Detener sin borrar datos

```bash
docker-compose stop
```

Luego para reanudar:
```bash
docker-compose start
```

### Opción 2: Detener y eliminar TODO

```bash
docker-compose down -v
```

⚠️ Esto borra todas las bases de datos.

---

## Troubleshooting

### Error: "Port already in use"

```bash
# Encuentra qué usa el puerto 8080
lsof -i :8080

# Mata el proceso
kill -9 <PID>
```

### Eureka dice "Out of Service" o servicios tardan

Espera 1-2 minutos. Es normal que tarden en registrarse y pasar health checks.

### Gateway devuelve "404 Not Found"

1. Verifica que Eureka esté up: `curl http://localhost:8761`
2. Verifica que el servicio esté registrado en Eureka
3. Espera más tiempo antes de hacer la prueba

### MongoDB "Connection refused"

```bash
# Verifica contenedores
docker ps | grep mongo

# Verifica logs
docker logs mongo-productos

# Intenta crear una conexión manual
docker exec mongo-productos mongosh --eval "db.runCommand("'"'ping'"'"')"
```

### Los logs no aparecen en CloudWatch

1. Verifica que LocalStack esté corriendo: `curl http://localhost:4566`
2. Reinicia los contenedores: `docker-compose restart`
3. Los logs pueden tardar 30 segundos en aparecer

---

## Resumen de prueba completa

1. ✅ Crear producto
2. ✅ Listar productos
3. ✅ Obtener producto por ID
4. ✅ Crear orden
5. ✅ Listar orden
6. ✅ Procesar pago
7. ✅ Obtener pago
8. ✅ Ver logs en CloudWatch
9. ✅ Acceder a MongoDB

Si todos estos pasos funcionan, el sistema está **100% operacional**. 🎉
