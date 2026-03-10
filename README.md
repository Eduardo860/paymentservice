# Examen Primer Parcial - Microservicios

Sistema completo de microservicios con Spring Boot, MongoDB y CloudWatch en Docker.

## 🚀 Ejecución

### Opción 1: Sequencial (3 pasos)

```bash
# Terminal 1: LocalStack
docker-compose -f docker/localstack/docker-compose.yml up -d

# Terminal 2: MongoDB
docker-compose -f docker/mongodb/docker-compose.yml up -d

# Terminal 3: Microservicios
docker-compose up --build
```

Espera 2-3 minutos. Luego:

- **Eureka**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **LocalStack**: http://localhost:4566

### Opción 2: Todo desde raíz (si LocalStack y MongoDB ya están corriendo)

```bash
docker-compose up --build
```

## 📋 Estructura

```
├── eureka-server/                    (Service Discovery)
├── apigateway/                       (Point of entry, port 8080)
├── productservice/                   (CRUD productos, port 8081)
├── orderservice/                     (CRUD órdenes, port 8082)
├── paymentservice/                   (CRUD pagos, port 8083)
├── docker-compose.yml                (Microservicios Spring Boot)
├── docker/
│   ├── mongodb/
│   │   └── docker-compose.yml        (3 instancias MongoDB)
│   └── localstack/
│       └── docker-compose.yml        (CloudWatch en AWS simulado)
├── ARQUITECTURA.md                   (Diagrama y componentes)
├── TESTING.md                        (Cómo ejecutar y probar)
└── VERIFICACION.md                   (Checklist del examen)
```

## 🔧 Microservicios

### Product Service (8081)
- **GET** `/productos` - Listar todos
- **GET** `/productos/{id}` - Por ID
- **POST** `/productos` - Crear
- **PUT** `/productos/{id}` - Actualizar
- **DELETE** `/productos/{id}` - Eliminar

### Order Service (8082)
- **POST** `/ordenes` - Crear
- **GET** `/ordenes/{id}` - Por ID
- **GET** `/ordenes/usuario/{userId}` - Por usuario
- **PUT** `/ordenes/{id}/status` - Actualizar estado

### Payment Service (8083)
- **POST** `/pagos/procesar` - Procesar pago
- **GET** `/pagos/{id}` - Por ID
- **GET** `/pagos/orden/{orderId}` - Por orden
- **PUT** `/pagos/{id}/reembolso` - Reembolso

## 📊 Bases de datos

- **MongoDB Productos**: puerto 27017
- **MongoDB Órdenes**: puerto 27018
- **MongoDB Pagos**: puerto 27019

Credenciales: `root` / `rootpassword`

## 📝 Documentación

1. **ARQUITECTURA.md** - Diagrama y componentes del sistema
2. **TESTING.md** - Cómo ejecutar y probar manualmente
3. **VERIFICACION.md** - Checklist de requisitos del examen

## ⛔ Detener

```bash
docker-compose down -v
```

---

**Estado**: ✅ Listo para evaluación

## 🗂️ Estructura del Proyecto

```
.
├── docker-compose.yml                 # Composefile de referencia (RECOMENDADO)
├── docker/
│   ├── microservices/docker-compose.yml (solo microservicios)
│   ├── mongodb/docker-compose.yml      (solo MongoDB)
│   └── localstack/docker-compose.yml   (solo LocalStack + CloudWatch)
├── eureka-server/
├── apigateway/
├── productservice/
├── orderservice/
├── paymentservice/
└── README.md (este archivo)
```

## 🚀 EJECUCIÓN RÁPIDA (RECOMENDADO)

Para levantar todo el sistema con un único comando:

```bash
docker-compose up -d
```

Esto levanta:
- LocalStack (con CloudWatch y Logs)
- 3 instancias de MongoDB (productos, órdenes, pagos)
- Eureka Server
- API Gateway
- 3 Microservicios

## 🔧 Ejecución Modular (RECOMENDADO para desarrollo)

### Estructura de Docker Compose Separados

El proyecto utiliza **3 docker-compose independientes** para mayor control y flexibilidad:

```
docker/
├── localstack/
│   └── docker-compose.yml    (AWS simulado + CloudWatch)
├── mongodb/
│   └── docker-compose.yml    (3 instancias de MongoDB)
└── microservices/
    └── docker-compose.yml    (5 servicios Spring Boot)
```

### Ejecución paso a paso (en 3 terminales diferentes)

**Terminal 1 - LocalStack:**
```bash
docker-compose -f docker/localstack/docker-compose.yml up
```

**Terminal 2 - MongoDB (espera 5 segundos):**
```bash
docker-compose -f docker/mongodb/docker-compose.yml up
```

**Terminal 3 - Microservicios (espera 10 segundos):**
```bash
docker-compose -f docker/microservices/docker-compose.yml up --build
```

**Alternativa desde raíz:**
```bash
# Terminal 1
cd docker/localstack && docker-compose up

# Terminal 2
cd docker/mongodb && docker-compose up

# Terminal 3
cd docker/microservices && docker-compose up --build
```

### Ventajas de los docker-compose separados

✅ Iniciar/parar componentes independientemente  
✅ Ver logs de cada parte por separado  
✅ Recompilar solo los servicios que cambian  
✅ Mejor para desarrollo y debugging  
✅ Mejor para entender la arquitectura  

Para información detallada, consulta [EJECUCION_COMPLETA.md](EJECUCION_COMPLETA.md) y [DOCKER_REFERENCE.md](DOCKER_REFERENCE.md)

## 📊 Puertos y Acceso

### Servicios Principales

| Servicio | Puerto | URL |
|----------|--------|-----|
| Eureka Server | 8761 | http://localhost:8761/eureka/apps |
| API Gateway | 8080 | http://localhost:8080 |
| Product Service | 8081 | http://localhost:8081/productos-api |
| Order Service | 8082 | http://localhost:8082/ordenes-api |
| Payment Service | 8083 | http://localhost:8083/pagos-api |

### MongoDB

| DB | Puerto | URI |
|----|--------|-----|
| Productos | 27017 | mongodb://localhost:27017 |
| Órdenes | 27018 | mongodb://localhost:27018 |
| Pagos | 27019 | mongodb://localhost:27019 |

### LocalStack

| Servicio | Endpoint |
|----------|----------|
| CloudWatch Logs | http://localhost:4566 |
| LocalStack UI | http://localhost:4566/_localstack/dashboard |

## 🔌 Endpoints del API Gateway

### Productos

```bash
GET    /productos              # Obtener todos
GET    /productos/{id}         # Obtener por ID
POST   /productos              # Crear
PUT    /productos/{id}         # Actualizar
DELETE /productos/{id}         # Eliminar
```

### Órdenes

```bash
POST   /ordenes                # Crear orden
GET    /ordenes/{id}           # Obtener por ID
GET    /ordenes/usuario/{userId}  # Obtener por usuario
PUT    /ordenes/{id}/status    # Actualizar estado
```

### Pagos

```bash
POST   /pagos/procesar         # Procesar pago
GET    /pagos/{id}             # Obtener por ID
GET    /pagos/orden/{orderId}  # Obtener por orden
PUT    /pagos/{id}/reembolso   # Procesar reembolso
```

## 📝 Ejemplos de Uso

### Crear un Producto

```bash
curl -X POST http://localhost:8080/productos \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "description": "Laptop de alto rendimiento",
    "price": 1299.99,
    "stock": 5
  }'
```

### Crear una Orden

```bash
curl -X POST http://localhost:8080/ordenes \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "totalAmount": 2500.00,
    "status": "PENDING"
  }'
```

### Procesar un Pago

```bash
curl -X POST http://localhost:8080/pagos/procesar \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "XXXX",
    "amount": 2500.00,
    "paymentMethod": "CREDIT_CARD"
  }'
```

## 📊 CloudWatch Logs

Los logs se envían automáticamente a los siguientes log groups:

| Servicio | Log Group |
|----------|-----------|
| Eureka | eureka-log-group |
| API Gateway | apigateway-log-group |
| Productos | producto-log-group |
| Órdenes | ordenes-log-group |
| Pagos | pagos-log-group |

### Ver logs en LocalStack

```bash
# Usando AWS CLI configurado para LocalStack
aws logs describe-log-groups --endpoint-url=http://localhost:4566
aws logs describe-log-streams --log-group-name=producto-log-group --endpoint-url=http://localhost:4566
aws logs get-log-events --log-group-name=producto-log-group --log-stream-name=producto-service-stream --endpoint-url=http://localhost:4566
```

## 🔐 Configuración Eureka

Todos los microservicios se registran automáticamente en Eureka Server.

**Verificar servicios registrados:**

```bash
curl http://localhost:8761/eureka/apps
```

## 🛑 Detener Servicios

### Detener todo

```bash
docker-compose down -v
```

### Detener modularmente

```bash
cd docker/localstack && docker-compose down -v
cd docker/mongodb && docker-compose down -v
cd docker/microservices && docker-compose down -v
```

## 🗄️ Base de Datos MongoDB

### Credenciales por defecto

- Usuario: `root`
- Contraseña: `rootpassword`

### Conectar con MongoDB Client

```bash
# Productos
mongosh --host localhost:27017 -u root -p rootpassword --authenticationDatabase admin

# Órdenes
mongosh --host localhost:27018 -u root -p rootpassword --authenticationDatabase admin

# Pagos
mongosh --host localhost:27019 -u root -p rootpassword --authenticationDatabase admin
```

## 📖 Documentación de Contacto entre Servicios

Los microservicios se comunican automáticamente a través de Eureka y API Gateway:

1. **Cliente externo** → envía peticiones al **API Gateway** (8080)
2. **API Gateway** → usa Load Balancer para enrutar a los servicios registrados en **Eureka**
3. **Servicios** → consumen de **MongoDB** local
4. **Logs** → enviados a **CloudWatch** en **LocalStack**

## 🐛 Troubleshooting

### Los servicios no se registran en Eureka

Verifica que Eureka Server esté saludable:
```bash
curl http://localhost:8761/eureka/apps
```

### MongoDB no conecta

Verifica que los contenedores estén corriendo:
```bash
docker ps | grep mongo
```

### CloudWatch no recibe logs

1. Verifica que LocalStack esté corriendo
2. Verifica la variable de entorno `CLOUDWATCH_ENDPOINT=http://localstack:4566`
3. Verifica los logs de CloudWatch en LocalStack UI

## 📚 Tecnologías Utilizadas

- **Spring Boot 3.2.0**
- **Spring Cloud 2023.0.0**
- **MongoDB 7.0**
- **LocalStack**
- **Docker & Docker Compose**
- **AWS CloudWatch Logs**
- **Eureka Server**
- **Spring Cloud Gateway**
- **Log4j2**

## 👨‍💼 Arquitecto

Diseño y configuración: Arquitecto Senior de Microservicios
