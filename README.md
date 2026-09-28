# pedidos360-ms-auditoria

Microservicio a elección de Pedidos360 (EP3/EP4 — DSY1107).

## Qué hace

Bitácora de todo lo que pasa por el topic `p360.eventos`: su cola está
enlazada con `#`, así que recibe copia de cada evento (hoy
`orden.registrada`, mañana cualquiera que se agregue) y lo guarda con su
routing key, sin necesidad de conocer el formato de cada evento.

Expone `GET /auditoria` (últimos 50, o filtro `?ordenId=`).

## Desarrollo local

```powershell
docker compose up -d          # base propia (puerto 5435)
# el broker vive en pedidos360-ms-admin/docker/rabbitmq-cluster
$env:RABBITMQ_HOST="localhost"
mvn spring-boot:run           # puerto 8086
```
