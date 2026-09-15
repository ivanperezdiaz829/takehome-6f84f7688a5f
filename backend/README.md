# Aforo y parte: Java

```bash
cp .env.example .env               # y pon dentro el token de tu correo
mvn exec:java@start                # tu API y la pantalla, en http://localhost:3000
mvn verify                         # lo que tiene que estar en verde antes de entregar
mvn test                           # tus pruebas
mvn exec:java@walkthrough          # el recorrido por consola, por si te ayuda a explorar
```

Java 21. Si no tienes Maven instalado, cambia `mvn` por `./mvnw` (o `mvnw.cmd` en Windows): el
wrapper viene incluido y se lo descarga solo. Jackson y JUnit ya están en el `pom.xml`; si te
viene bien traerte alguna dependencia más, tráetela: el `pom.xml` es tuyo. **Y eso incluye el
framework**: si prefieres Spring Boot o Javalin al `com.sun.net.httpserver` de aquí, adelante:
va con la estándar para que arranque sin instalar nada, no porque tenga que ser así.

Si algo no arranca a la primera, no es cosa tuya: avísanos y lo miramos.
Estos comandos se lanzan **desde `backend/`**, que es donde estás. La pantalla es el módulo de al
lado, `frontend/`, y la sirve este mismo servidor: lo que edites ahí se ve al recargar.

## Qué hay aquí

| | |
|---|---|
| `domain/Guest`, `Booking`, `PoliceReportLine` | Los tipos, cada uno en su sitio |
| `domain/Ages` | La edad en años cumplidos. Viene resuelta |
| `domain/Totals` | **Las dos funciones que tienes que escribir** |
| `Server` | Tu API. El enrutado está hecho; **cuatro manejadores están a 501** |
| `ApiClient` | El cliente de las dos APIs. Escrito, y escrito a medias |
| `http/Json` | El sobre de error, para que todos tus errores salgan iguales |
| `../frontend/index.html` | **Tu mitad de delante**, en el módulo de al lado. El HTML y el `fetch` están hechos; `renderList` y `renderDetail`, **vacías** |
| `CheckIT` (en `src/test`) | El comprobador. Arranca en rojo, y es un IT para no colarse en tu `mvn test` |
| `Main` | El recorrido por consola |

## Cómo empezar

Arranca el `check` antes de escribir nada: te dice dónde estás. Son 39 tests (doce por cada
reserva, más tres que no son de ninguna) y no explican las causas: cuando un número no cuadre, el
porqué es tuyo.

Y lee `ApiClient` **antes** de tocarlo. Está hecho, pero no está completo, y lo que le falta no
lleva un `TODO` encima.
