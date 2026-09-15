# **Workfactory Takehome - Reservas y Partes Policiales**

--- 

## Cálculo de aforo, gestión de huéspedes y resiliencia de API en tiempo real

![Java](https://img.shields.io/badge/Java-21-red?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-blue?logo=apachemaven&logoColor=white)
![Vanilla JS](https://img.shields.io/badge/Frontend-Vanilla%20JS-yellow?logo=javascript&logoColor=white)
![Status](https://img.shields.io/badge/Status-Terminado-green)

## Parte A: Revisión de Código
Para la parte teórica (Parte A) donde se analiza un servicio TypeScript existente buscando antipatrones, fallos de seguridad y mejoras:

### [**Leer la Revisión de Código (REVISION.md)**](REVISION.md)

---

## ¿En qué consiste? (Parte B)
Este proyecto implementa un servicio backend y su correspondiente interfaz gráfica para gestionar las reservas de una villa y calcular correctamente dos métricas de negocio que no deben mezclarse:

1. **La capacidad (Aforo):** Los menores de 2 años no ocupan plaza.
2. **El parte policial:** Todos los viajeros cuentan, sin excepción por edad.

---

## Instalación y Uso

1. **Configurar el entorno:**
   Copia el archivo de ejemplo y añade tu token de la API:
   ```bash
   cd backend
   cp .env.example .env
   # Edita el .env y pon: API_TOKEN=tu_token_aqui y API_BASE=[https://join.workfactory.es/api](https://join.workfactory.es/api)

2. **Ejecutar el servidor Backend + Pantalla en localhost:3000:**
    ```bash
    .\mvnw.cmd exec:java@start
    ```

3. **Lanzar tests:**
    ```bash
    .\mvnw.cmd verify
    ```

---

## Decisión del Lenguaje

He seleccionada Java porque junto con Python son los lenguajes que más controlo y en general para cosas como sistemas, aplicaciones, web, etc, estoy mucho más cómodo usando Java, entre otros, por los siguientes motivos:

- **Tipado estático y seguridad en tiempo de compilación:** Permite detectar errores de mapeo de datos o contratos de la API antes de ejecutar el código lo que hace más sencilla su depuración.
- **Procesamiento funcional de colecciones:** La API de _Streams_ de Java facilita la transformación y el filtrado de los listados de huéspedes y reservas de forma limpia y legible.
- **Ecosistema estándar:** Gracias al servidor HTTP nativo y al cliente HTTP moderno de Java, es posible levantar una arquitectura robusta tolerante a fallos sin necesidad de añadir _frameworks_ pesados de terceros.
- **Experiencia personal:** En mi experiencia, para cualquier sistema estructurado con un leguaje como Java es mucho más fácil de gestionar que con Python (también he trabajado con TypeScript pero para la prueba he preferido ir a lo que más conozco).

---

### Uso de la IA

Para el desarrollo de la prueba técnica se ha hecho uso del módelo de IA de google **Gemini** realizando **_Pair Programming_** evitando peticiones de soliciones completas y enfocándo su uso en la resolución de problemas de arquitectura e integración, comparando mi opinión personal de desarrollo con las respuestas del modelo.

**Cosas que se le han pedido al modelo durante el desarrollo:**
- Opinión acerca del diseño resiliente HTTP con el control de reintentos y retroceso exponencial (_backoff_).
- El mapeo estricto de las reglas de normalización de los datos policiales, parentescos y tipos de documentos del fichero [Totals.java](backend/src/main/java/es/workfactory/occupancy/domain/Totals.java).
- Asistencia en el diagnóstico de los fallos devueltos por la _suite_ de integración ([CheckIT.java](backend/src/test/java/es/workfactory/occupancy/CheckIT.java)).


**Cosas descartadas y aceptadas:** Se adoptaron patrones estándar de resiliencia de red y expresiones regulares de validación documental supervisando y refactorizando de manera exhaustiva cada línea para garantizar el acoplamiento exacto con los requisitos del dominio y las pruebas automatizadas.

---

## Cosas que me he encontrado y la manera en la que las he resuelto

**Inestabilidad y códigos transitorios (`503` y `429`):**
- **Problema:** La API externa devuelve caídas temporales y límites de velocidad.
- **Solución:** Se programó un bucle de reintentos inteligente con incremento exponencial del tiempo de espera en [ApiClient](backend/src/main/java/es/workfactory/occupancy/ApiClient.java) para capturar tanto los códigos `503` como los `429`.

**Paginación y códigos `404` inesperados:**
- **Problema:** El test automatizado intentaba seguir enlaces de paginación que apuntaban a páginas inexistentes en el servidor _upstream_.
- **Solución:** Se blindó el cliente y el servidor local para procesar los metadatos de paginación de manera robusta y controlada.

**Sondeo asíncrono (_Polling_) de los lotes policiales:**
- **Problema:** Las declaraciones enviadas a la autoridad policial requerían un breve tiempo de procesamiento, devolviendo inicialmente un estado `pending`.
- **Solución:** Se implementó un blucle de sondeo corto en `ApiClient.getBatch` para reintentar la consulta del lote hasta que la autoridad devuelva el estado procesado con todos los contadores de aceptados y rechazados.

---

## Decisiones tomadas

- **Aislamiento total del dominio de negocio:** Se decidió centralizar las matemáticas en la clase [Totals.java](backend/src/main/java/es/workfactory/occupancy/domain/Totals.java), separando estrictamente el cálculo del aforo de la generación de la parte policial. Esto se hizo para prevenir mezclar métricas y seguir el principio de responsabilidad única.
- **Uso del cliente HTTP sin dependencias externas:** Se decidió usar exclusivamente las librerías nativas de Java junto con Jackson para el parseo JSON asegunrando que el despliegue y compilación mediante Maven sean rápidos y libres de conflictos.

---

## Cosas que se han dejado fuera a propósito

**Ampliaciones estéticas o funcionalidades avanzadas en el FrontEnd:** Se ha decidido priorizar el cumplimiento de todos los tests y la estabilidad del backend. La interfaz gráfica se ha limitado estrictamente a renderizar los datos asociados (`renderList` y `renderDetail`) añadiendo una interfaz gráfica muy simple basada en una plantilla cargada de otro proyecto personal.

---

## Mayores dificultades encontradas

En donde estuve más tiempo fue en ajustar la sincronización de las llamadas concurrentes de la _suite_ de pruebas de interrogación para evitar colapsar los límites de velocidad de la API externa, equilibrando los retardos de red con los tiempos de respuesta exigidos por JUnit.

---

## Autoría

Este proyecto ha sido desarrollado como parte de la prueba técnica para WorkFactory:

* **Iván Pérez Díaz** - *Desarrollo de Software, Arquitectura y Lógica de Negocio* - [GitHub](https://github.com/ivanperezdiaz829)