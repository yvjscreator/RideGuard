# RideGuard

RideGuard es una aplicación Android de código abierto que calcula la rentabilidad de ofertas visibles en Uber Driver, Cabify Driver y DiDi Driver. El análisis ocurre en el teléfono. La aplicación no acepta ni rechaza viajes y no simula toques.

## Estado del prototipo

La detección de Uber fue validada en un Xiaomi M2012K11AG mediante ADB y `uiautomator`. Una oferta real expuso estos textos en el árbol de accesibilidad:

```text
ARS3,903
ARS558/km (estimado)
A 6 min (1.4 km)
Viaje: 20 min (5.6 km)
```

Esto confirma que, en esa versión de Uber Driver, pago, tiempo y distancia pueden leerse sin OCR. RideGuard usa accesibilidad como fuente principal y mantiene ML Kit OCR como módulo opcional para pantallas o versiones que no expongan texto. El APK principal no incluye ni carga el modelo OCR.

Los parsers de Cabify y DiDi incluyen formatos iniciales y tests sintéticos. Deben validarse con ofertas reales de cada plataforma antes de considerarlos estables.

## Cálculo

Todos los cálculos incluyen recogida y viaje:

```text
tiempo total = minutos hasta recogida + minutos del viaje
distancia total = km hasta recogida + km del viaje
ARS/km = pago / distancia total
ARS/min = pago / tiempo total
ARS/h = pago × 60 / tiempo total
```

Con la oferta observada: 26 minutos, 7,0 km, aproximadamente ARS 558/km y ARS 9.007/h.

Hay dos modos de objetivo:

- Por hora: compara la oferta con un objetivo fijo de ARS/h.
- Por jornada: recalcula la tasa necesaria con la meta y el tiempo restantes. Si una jornada de 6 horas tiene una meta de ARS 120.000 y después de 3 horas acumula ARS 70.000, la tasa requerida pasa a ARS 16.667/h.

El mínimo ARS/km se evalúa siempre para proteger el costo del vehículo. Los porcentajes que separan verde, amarillo y rojo son configurables.

## Arquitectura

- `app`: interfaz Jetpack Compose y configuración.
- `core:model`: ofertas, objetivos, métricas y contratos de parsers.
- `core:calculator`: cálculo y clasificación sin dependencias de UI.
- `core:settings`: persistencia local de objetivos y jornada.
- `detection:accessibility`: lectura limitada a paquetes de apps de conducción.
- `detection:ocr`: módulo opcional de reconocimiento con ML Kit desde un `Bitmap` suministrado; no se empaqueta en la app principal.
- `platforms:uber`, `platforms:cabify`, `platforms:didi`: parsers independientes.
- `overlay`: panel flotante no interactivo mediante `TYPE_ACCESSIBILITY_OVERLAY`.

## Compilar

Requisitos:

- JDK 17
- Android SDK 36
- Un teléfono con Android 8.0 o posterior, o un emulador

```bash
./gradlew test
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Para compilar y validar también el módulo OCR:

```bash
./gradlew :detection:ocr:assemble
```

Después de instalar:

1. Abre RideGuard.
2. Configura tus objetivos.
3. Pulsa **Abrir Accesibilidad**.
4. Activa **Analizador de ofertas RideGuard**.
5. Abre una app de conducción. Cuando aparezca una oferta completa, RideGuard mostrará ARS/h y ARS/km. El panel desaparece al cerrarse la oferta o, como máximo, tras 18 segundos.

## Privacidad y seguridad

- El servicio declara únicamente los paquetes conocidos de Uber Driver, Cabify Driver y DiDi Driver.
- No se envían capturas, textos ni métricas a servidores.
- El overlay no recibe toques, para evitar interferir con la app de conducción.
- El servicio no declara capacidad para ejecutar gestos.
- No existe código para aceptar o rechazar ofertas.
- OCR recibe imágenes explícitas; la captura de pantalla con `MediaProjection` no está activada automáticamente.

## Consumo de recursos

El detector funciona por eventos; no hace polling. Limita cada lectura a 350 nodos, 16.000 caracteres y espera al menos 750 ms entre escaneos. Libera nodos de accesibilidad en versiones antiguas de Android. El servicio corre en el proceso separado `:detector`, por lo que Android puede liberar la interfaz Compose mientras conduces. El overlay usa vistas Android pequeñas y desaparece automáticamente. ML Kit no forma parte del APK principal. La compilación `release` activa reducción y optimización de código y recursos.

Android reserva los servicios de accesibilidad para funciones que ayudan a usuarios con discapacidades. Una distribución pública debe revisar las políticas vigentes de Google Play y explicar claramente el propósito del servicio. Instalación directa o tiendas alternativas pueden tener requisitos distintos.

## Añadir un formato

Cada integración implementa `OfferParser`. Añade una muestra anonimizada como test antes de modificar expresiones regulares. Nunca incluyas nombres, direcciones, coordenadas ni capturas sin consentimiento.

## Licencia

Apache License 2.0. Consulta [LICENSE](LICENSE).
