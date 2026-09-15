# **Workfactory Takehome - Reservas y Partes Policiales**

## Cálculo de aforo, gestión de huéspedes y resiliencia de API en tiempo real

![Java](https://img.shields.io/badge/Java-21-red?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-blue?logo=apachemaven&logoColor=white)
![Vanilla JS](https://img.shields.io/badge/Frontend-Vanilla%20JS-yellow?logo=javascript&logoColor=white)
![Status](https://img.shields.io/badge/Status-En%20Desarrollo-orange)

## Parte A: Revisión de Código
Para la parte teórica (Parte A) donde se analiza un servicio TypeScript existente buscando antipatrones, fallos de seguridad y mejoras:

### [👉 **Leer la Revisión de Código (REVISION.md)**](REVISION.md)

---

## ¿En qué consiste? (Parte B)
Este proyecto implementa un servicio backend y su correspondiente interfaz gráfica para gestionar las reservas de una villa y calcular correctamente dos métricas de negocio que no deben mezclarse:

1. **La capacidad (Aforo):** Los menores de 2 años no ocupan plaza.
2. **El parte policial:** Todos los viajeros cuentan, sin excepción por edad.

### Características Principales
* **Resiliencia ante fallos (503):** El cliente de la API implementa un patrón de *Retry con Exponential Backoff* para asegurar que el servicio sobrevive a las caídas intermitentes de la API de Workfactory.
* **Separación de responsabilidades:** Las matemáticas puras del negocio están aisladas en el dominio (`Totals.java`).
* **Ligereza:** Sin frameworks pesados. Servidor HTTP nativo de Java (`com.sun.net.httpserver`) y Vanilla JS en el frontend.

---

## Decisiones Técnicas y Respuestas

### ¿Por qué elegí Java 21?
Elegí Java por su tipado estático, la madurez de sus herramientas de testeo e integración (Maven, JUnit) y la robustez que ofrece a la hora de manejar concurrencia y clientes HTTP. Además, Java 21 incluye mejoras sustanciales en su `HttpClient` nativo que hacen muy limpio el manejo de peticiones sin depender de librerías externas.

### ¿Cómo usé la IA y con qué estrategia?
He utilizado un LLM (Gemini/ClaudeAI) a modo de **Pair Programmer**:
* **Estrategia:** En lugar de pedirle que me resuelva la prueba entera ("Zero-shot"), lo he utilizado paso a paso. Primero le pedí ayuda para estructurar los pasos, luego para identificar la trampa arquitectónica de los errores `503` y, finalmente, he ido iterando el código con su ayuda para resolver problemas de entorno (PowerShell vs CMD) y dependencias.
* **Qué acepté:** La implementación del *Exponential Backoff* para el cliente HTTP, ya que es un patrón estándar.
* **Qué tiré/Corregí:** Ajustes en los bucles de reintento y configuración del archivo `.env` que el modelo formateó con caracteres markdown erróneos.

### Qué me encontré por el camino y cómo lo resolví
* **Problema:** Errores al lanzar Maven desde PowerShell en Windows y problemas con la ruta del archivo `.env`.
* **Solución:** Ajustar la navegación de directorios en consola y utilizar `.\mvnw.cmd` para garantizar que se usaba el wrapper local de Maven.
* **Problema:** La API de pruebas devuelve códigos `503 Service Unavailable` aleatorios.
* **Solución:** Implementar un bucle de hasta 5 reintentos en `ApiClient.java`, duplicando el tiempo de espera entre cada uno (`500ms`, `1s`, `2s`...).

### Qué dejé fuera a propósito
*(Por completar al final de la prueba)*

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

## Autoría

Este proyecto ha sido desarrollado como parte de un trabajo de investigación de Visión por Computador por:

* **Iván Pérez Díaz** - *Desarrollo de Software, Arquitectura y Lógica de Negocio* - [GitHub](https://github.com/ivanperezdiaz829)