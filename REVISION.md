# **Revisión de Arquitectura y Código (Parte A)**

--- 

## **NOTA:** 
En la carpeta descargada (independientemente del lenguaje elegido), en la raíz no se encontraban ni el README.md ni el REVISION.md ni el servicio de TS, entiendo por lo tanto que el servicio a explicar es el que he de solucionar en la parte B, además de crear yo manualmente el README.md y el REVISION.md.

---

## Diagnóstico de Fallos y Antipatrones

- **Ausencia de resiliencia ante fallos transitorios:** El servicio original carece de políticas de reintento (retries) y retroseso (_backoff_), lo que provoca caídas inmediatas ante códigos de error comunes como el 503 o límites de tasa como el 429.
- **Debilidad en el tipado y contratos de datos:** Se observa un tipado laxo al procesar las respuestas de la API externa de los huéspedes y lotes policiales lo que explone al sistema a fallos a la hora de deserializar los datos.
- **Mala gestión de paginación:** La navegación por las páginas de reservas y viajeros asume estructuras estáticas o realiza bucles ciegos propensos a errores 404 cuando los errores de paginación no se resuelven de forma segura.

--- 

## Cosas a priorizar

- **Blindaje para la capa HTTP:** Es necesario hacer que sea tolerante a fallos para gestionar de forma nativa los códigos de estado transitorios y el control de saturación del tráfico.
- **Validación estricta (_Runtime Validation_):** Es necesario introducir validadores para asegurar que los objetos que entran de la API externa cumplan exactamente con los campos requeridos antes de procesar el aforo o la policía.
- **Mejorar la robustez de los flujos:** Incorporar _polling_ para los estados pendientes de los lotes de declaración policial, evitando respuestas prematuras o bloqueantes.

--- 

## Elementos Intocables

- **Separación de responsabilidades estructurales:** No se debería alterar la división conceptual entre el servidor HTTP, el cliente de comunicación externa y los módulos puros de lógica de negocio. Es decir, respetar la estructura del sistema.
- **Las reglas de negocio y calculos temporales:** Las funciones encargadas de determinar la edad cumplida en una fecha de referencia específica no se deben modificar para evitar discrepancias de cálculo en los límites de los umbrales de aforo (los menores de dos años).
  Este proyecto ha sido desarrollado como parte de un trabajo de investigación de Visión por Computador por:

---

## Autoría 

Este proyecto ha sido desarrollado como parte de la prueba técnica para WorkFactory:

* **Iván Pérez Díaz** - *Desarrollo de Software, Arquitectura y Lógica de Negocio* - [GitHub](https://github.com/ivanperezdiaz829)