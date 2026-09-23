# RideGuard

RideGuard es una aplicación Android de código abierto que calcula la rentabilidad de ofertas visibles en Uber Driver y Cabify Driver. El análisis ocurre en el teléfono. La aplicación no acepta ni rechaza viajes y no simula toques.

## Estado del prototipo

La detección de Uber fue validada en un Xiaomi M2012K11AG mediante ADB y `uiautomator`. Una oferta real expuso estos textos en el árbol de accesibilidad:

```text
ARS3,903
ARS558/km (estimado)
A 6 min (1.4 km)
Viaje: 20 min (5.6 km)
```

Esto confirma que, en esa versión de Uber Driver, pago, tiempo y distancia pueden leerse sin OCR **cuando la oferta de Uber está visible**. RideGuard usa accesibilidad como fuente principal y mantiene ML Kit OCR como módulo opcional para pantallas o versiones que no expongan texto. El APK principal no incluye ni carga el modelo OCR.

Cuando otra app está delante, Android no permite leer una pantalla oculta de Uber o Cabify. RideGuard analiza sus **ventanas de oferta visibles** sobre otra app y, cuando la plataforma la emite, el texto de una notificación completa. Las ofertas flotantes de Uber y Cabify se comprobaron en el Xiaomi de prueba tanto dentro como fuera de las apps; alguna oferta de Cabify no produjo tarjeta durante las pruebas, por lo que la cobertura aún no es perfecta. Solo muestra métricas si encuentra **pago, minutos y kilómetros tanto de recogida como de viaje**. Un aviso que solo invita a abrir la app no basta. El comportamiento puede variar según versión, teléfono y plataforma.

DiDi queda pendiente: en las ofertas reales observadas en este teléfono, su ventana no expuso texto a Accesibilidad. Su parser experimental permanece en el repositorio como referencia, pero el servicio no escucha DiDi, el APK no incluye su módulo y no se activa OCR ni captura de pantalla.

## Cálculo

El tiempo usado para ARS/h incluye la recogida, el viaje y una espera estimada para que suba el pasajero. La distancia incluye recogida y viaje:

```text
tiempo total estimado = minutos hasta recogida + minutos del viaje + 1,28 min de espera al recoger
distancia total = km hasta recogida + km del viaje
ARS/km = pago / distancia total
ARS/h = pago × 60 / tiempo total estimado
```

Los **1,28 minutos decimales** provienen de la media de espera *después de llegar al punto de recogida* en [un estudio de 416 viajes Uber/Lyft realizados por un investigador en Denver](https://wp-cpr.s3.amazonaws.com/uploads/2019/06/cu-uber-lyft-study.pdf) (mediana: 1 minuto). Equivalen a **1 min 16,8 s**; la app muestra **1 min 17 s** al redondear la pantalla al segundo, pero conserva 1,28 minutos en el cálculo. Es una referencia histórica provisional, **no un promedio argentino ni una medición personal**. No incluye espera entre viajes. Se suma una vez por oferta; no aumenta los kilómetros. [Uber indica que las tarifas por espera, cuando correspondan, se añaden al precio anunciado](https://help.uber.com/driving-and-delivering/article/calculation-of-prices?nodeId=470cd474-831c-4e01-8e5a-3032ca39bab1), por lo que el ingreso final podría diferir. La app explica esta suposición en la tarjeta «Espera al recoger» y enlaza el estudio.

Con la oferta observada: 26 minutos anunciados + 1,28 minutos decimales de espera = 27,28 minutos decimales estimados, 7,0 km, aproximadamente ARS 558/km y ARS 8.584/h. El panel muestra **27:17** (min:seg) al redondear al segundo, pero calcula con 27,28 minutos decimales.

Cada oferta se compara con dos mínimos que deben cumplirse **a la vez**: ARS/h y ARS/km. Los valores iniciales son ARS 15.000/h y ARS 650/km de lunes a miércoles, y ARS 18.000/h y ARS 750/km de jueves a domingo. Se pueden editar en la app. El panel es verde si cumple ambos, ámbar si queda como máximo un 5 % por debajo de alguno (y ninguno cae más lejos), y rojo si alguno queda más de un 5 % por debajo. El ámbar no significa que cumpla el mínimo; solo evita que una diferencia pequeña se vea igual que una oferta muy mala. No muestra etiquetas de clasificación.

La app pregunta cuántas horas sueles trabajar y muestra una estimación bruta de la jornada: objetivo ARS/h × horas habituales. Esa tarjeta no utiliza la espera estimada de 1 min 17 s; la espera se añade al tiempo de **cada oferta** antes de calcular el ARS/h del panel. La tarjeta es una referencia, no una predicción de ingresos reales.

Para un viaje individual, el pago mínimo que cumple ambos objetivos es el mayor entre `objetivo ARS/h × minutos estimados / 60` y `mínimo ARS/km × km totales`. Por ejemplo, una oferta de 17 minutos anunciados y 4 km que paga ARS 3.604 se evalúa con 18,28 minutos decimales (se muestran **18:17**): aproximadamente ARS 11.829/h y ARS 901/km. Aunque supera los mínimos por kilómetro, necesitaría ARS 4.570 de lunes a miércoles o ARS 5.484 de jueves a domingo para cumplir el objetivo por hora.

El color es un filtro de la **oferta bruta**, no una garantía de rentabilidad de la jornada. La espera al recoger puede diferir de la media histórica; la espera entre viajes, kilómetros sin oferta, combustible, mantenimiento y desgaste del auto reducen el resultado real. La tarjeta de jornada supone que cada minuto de trabajo se factura al objetivo indicado y no descuenta gastos. Los umbrales personales deben calibrarse con ingresos, horas conectadas y kilómetros de odómetro de varias jornadas reales.

Cada día puede habilitarse y tener horas de inicio y fin elegidas con el selector horario de Android. Configura y activa el lunes; **Copiar lunes a toda la semana** aplica ese horario y activa los siete días. Después puedes cambiar o desactivar cualquier día antes de guardar. El detector analiza ofertas únicamente dentro de esos horarios. Los turnos que cruzan medianoche conservan el perfil del día de inicio. Inicialmente todos los días están desactivados para que el usuario elija sus horas de trabajo.

## Arquitectura

- `app`: interfaz Jetpack Compose y configuración.
- `core:model`: ofertas, objetivos, métricas y contratos de parsers.
- `core:calculator`: cálculo y clasificación sin dependencias de UI.
- `core:settings`: persistencia local de objetivos, horas habituales y horarios.
- `detection:accessibility`: lectura limitada a paquetes de apps de conducción.
- `detection:ocr`: módulo opcional de reconocimiento con ML Kit desde un `Bitmap` suministrado; no se empaqueta en la app principal.
- `platforms:uber`, `platforms:cabify`: parsers activos independientes.
- `platforms:didi`: prototipo no incluido en el APK.
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
2. Configura los dos perfiles y tus horas habituales. Activa el lunes, toca sus horas para elegirlas y, si quieres, cópialas a toda la semana. Ajusta o desactiva los demás días y guarda.
3. Pulsa **Abrir Accesibilidad**.
4. Activa **Analizador de ofertas RideGuard**.
5. Dentro de tu horario, abre una app de conducción. Cuando aparezca una oferta completa, RideGuard mostrará únicamente ARS/h, ARS/km, tiempo total estimado y kilómetros totales. El panel desaparece al cerrarse la oferta o, como máximo, tras 18 segundos.

El servicio de Accesibilidad permanece habilitado a nivel del sistema; el horario controla el **análisis**, no el permiso. [Android gestiona el ciclo de vida del servicio](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) después de que lo habilita el usuario. Si el sistema o el fabricante lo desactiva al cerrar la app o reiniciar, vuelve a activarlo desde la tarjeta de estado y revisa las [restricciones de batería](https://developer.android.com/topic/performance/background-optimization) y el autoinicio del teléfono. Una app normal no puede concederse ese permiso por sí misma.

## Privacidad y seguridad

- El servicio declara únicamente los paquetes conocidos de Uber Driver y Cabify Driver; también puede leer eventos de notificación emitidos por esas apps.
- No se envían capturas, textos ni métricas a servidores.
- El overlay no recibe toques, para evitar interferir con la app de conducción.
- El servicio no declara capacidad para ejecutar gestos.
- No existe código para aceptar o rechazar ofertas.
- OCR recibe imágenes explícitas; la captura de pantalla con `MediaProjection` no está activada automáticamente.

## Consumo de recursos

El detector funciona por eventos mientras no hay oferta. Limita cada lectura a 350 nodos, 16.000 caracteres y espera al menos 750 ms entre escaneos. Solo mientras se muestra una tarjeta comprueba cada 1,5 segundos si la ventana de oferta sigue visible, para ocultarla poco después de que se cierre; la tarjeta dura como máximo 18 segundos. Libera nodos de accesibilidad en versiones antiguas de Android. El servicio corre en el proceso separado `:detector`, por lo que Android puede liberar la interfaz Compose mientras conduces. El overlay usa vistas Android pequeñas. ML Kit no forma parte del APK principal. La compilación `release` activa reducción y optimización de código y recursos.

Android reserva los servicios de accesibilidad para funciones que ayudan a usuarios con discapacidades. Una distribución pública debe revisar las políticas vigentes de Google Play y explicar claramente el propósito del servicio. Instalación directa o tiendas alternativas pueden tener requisitos distintos.

## Añadir un formato

Cada integración implementa `OfferParser`. Añade una muestra anonimizada como test antes de modificar expresiones regulares. Nunca incluyas nombres, direcciones, coordenadas ni capturas sin consentimiento.

## Licencia

Apache License 2.0. Consulta [LICENSE](LICENSE).
