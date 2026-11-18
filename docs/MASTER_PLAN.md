# LISOM ASSISTANT SDK
## Plan maestro de desarrollo y estructura técnica

**Versión:** 1.0  
**Estado:** Planificación técnica  
**Tipo de proyecto:** Librería / SDK independiente  
**Tecnología principal:** Kotlin Multiplatform  
**Procesamiento:** Local y offline obligatorio  
**Servidor externo:** Ninguno

---

## 1. Descripción del proyecto

Lisom Assistant será un motor de voz independiente diseñado para integrarse en aplicaciones de terceros.

No será una aplicación independiente, no tendrá interfaz gráfica propia y no necesitará infraestructura de servidores.

Su función será escuchar al usuario, reconocer su voz, interpretar sus instrucciones y entregar órdenes estructuradas a la aplicación que lo integre.

La primera integración será con Lynox Music. Lynox Music permanecerá en su propio repositorio: este repositorio contiene exclusivamente Lisom Assistant.

## 2. Objetivos principales

- Reconocer comandos de voz en español sin conexión a Internet.
- Permitir activación mediante botón push-to-talk.
- Permitir activación mediante la palabra «Oye Lisom».
- Interpretar instrucciones mediante procesamiento local.
- Proporcionar respuestas de voz sin servicios externos.
- Entregar comandos estructurados a la aplicación anfitriona.
- Funcionar como librería reutilizable.
- Soportar Android, Windows e iOS.
- No depender de cuentas, suscripciones ni servidores.

## 3. Arquitectura general

La arquitectura tendrá cinco componentes principales:

1. **Lisom Core:** coordina el funcionamiento del motor.
2. **Lisom Speech:** reconoce la voz del usuario.
3. **Lisom Wake Word:** detecta la frase de activación.
4. **Lisom NLU:** interpreta las instrucciones.
5. **Lisom TTS:** genera respuestas habladas.

Los componentes se comunicarán mediante interfaces internas. La aplicación anfitriona recibirá los resultados mediante una API local del SDK.

### Flujo de funcionamiento

`Usuario → Micrófono → Wake Word → Speech Recognition → NLU → Comando estructurado → Aplicación anfitriona`

Para las respuestas:

`Aplicación anfitriona → Resultado → Lisom Core → TTS → Usuario`

## 4. Estructura de carpetas

```text
LisomAssistant/
├── lisom-core/
│   └── src/commonMain/
├── lisom-speech/
│   └── src/
│       ├── commonMain/
│       ├── androidMain/
│       ├── iosMain/
│       └── jvmMain/
├── lisom-wakeword/
│   └── src/
│       ├── commonMain/
│       ├── androidMain/
│       ├── iosMain/
│       └── jvmMain/
├── lisom-nlu/
│   └── src/commonMain/
├── lisom-tts/
│   └── src/
│       ├── commonMain/
│       ├── androidMain/
│       ├── iosMain/
│       └── jvmMain/
├── lisom-platform/
│   ├── android/
│   ├── windows/
│   └── ios/
├── lisom-models/
│   ├── speech/
│   ├── wakeword/
│   └── tts/
├── lisom-api/
│   └── src/commonMain/
├── lisom-testing/
│   └── src/commonTest/
├── lisom-demo/
├── docs/
│   ├── ARCHITECTURE.md
│   ├── API.md
│   ├── INSTALLATION.md
│   ├── PLATFORM_SUPPORT.md
│   └── OFFLINE_MODE.md
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

La estructura podrá evolucionar durante la implementación, siempre que se mantenga la separación modular y multiplataforma.

## 5. Función de cada módulo

### 5.1 Lisom Core

Será el núcleo del SDK.

Responsabilidades:
- Inicializar los módulos.
- Gestionar el ciclo de escucha.
- Coordinar reconocimiento e interpretación.
- Administrar estados.
- Gestionar errores.
- Entregar resultados a la aplicación anfitriona.

### 5.2 Lisom Speech

Se encargará del reconocimiento automático del habla (ASR).

Características:
- Reconocimiento offline.
- Español como idioma inicial.
- Procesamiento de audio local.
- Conversión de voz a texto.
- Soporte para modelos acústicos locales.

**Tecnología propuesta:** Sherpa-ONNX.

### 5.3 Lisom Wake Word

Detectará la palabra de activación: **«Oye Lisom»**.

Responsabilidades:
- Escucha de activación.
- Detección de la palabra clave.
- Activación del reconocimiento completo.
- Reducción de activaciones accidentales.

**Tecnología propuesta:** detección de palabras clave mediante Sherpa-ONNX.

### 5.4 Lisom NLU

Será el módulo de interpretación de lenguaje natural.

Ejemplos:
- «Reproduce música de Morat».
- «Busca canciones de Shakira».
- «Pausa la música».
- «Continúa reproduciendo».
- «Pasa a la siguiente canción».
- «Quiero escuchar las letras».
- «Reproduce mi lista favorita».

Transformará las frases en comandos estructurados.

### 5.5 Lisom TTS

Será responsable de la síntesis de voz.

Características:
- Respuestas habladas.
- Funcionamiento offline.
- Voz en español.
- Interfaz independiente del motor de síntesis.

Se seleccionará e integrará un motor TTS local compatible con las plataformas.

## 6. API pública del SDK

Lisom tendrá una API de programación para que las aplicaciones puedan integrarlo.

Ejemplo conceptual:

```kotlin
val lisom = LisomAssistant.create(
    config = LisomConfig(
        offlineRequired = true,
        wakeWord = "Oye Lisom"
    )
)

lisom.initialize()
lisom.startListening()

lisom.commands.collect { command ->
    // La aplicación procesa el comando.
}
```

Los comandos utilizarán modelos estructurados. Ejemplo conceptual:

```kotlin
sealed interface LisomCommand {
    data class SearchSong(val title: String) : LisomCommand
    data class PlayArtist(val artist: String) : LisomCommand
    data object Pause : LisomCommand
    data object Resume : LisomCommand
    data object Next : LisomCommand
    data object Previous : LisomCommand
    data object ShowLyrics : LisomCommand
}
```

Estos ejemplos representan el diseño inicial; la API definitiva se establecerá durante la implementación.

## 7. Integración con Lynox Music

Lynox Music será responsable de la interfaz gráfica, el catálogo musical, la reproducción y la gestión de las letras.

Lisom solamente interpretará las instrucciones y enviará los comandos.

Ejemplo: el usuario dice «Oye Lisom, reproduce música de Morat». Lisom entrega:

```kotlin
LisomCommand.PlayArtist("Morat")
```

Lynox Music recibe el comando y realiza la búsqueda y reproducción.

Lisom no necesita conocer los servidores, proveedores musicales ni la estructura interna del catálogo de Lynox Music.

**Límite entre proyectos:** Lynox Music será una aplicación anfitriona independiente. No se incorporará código exclusivo de Lynox Music a este repositorio.

## 8. Tecnologías

| Componente | Tecnología propuesta |
|---|---|
| Lenguaje | Kotlin |
| Arquitectura | Kotlin Multiplatform |
| Reconocimiento de voz | Sherpa-ONNX |
| Detección de activación | Sherpa-ONNX KWS |
| Procesamiento de órdenes | Kotlin local |
| Síntesis de voz | Motor TTS offline |
| Audio | Adaptadores nativos |
| Construcción | Gradle |
| Pruebas | Kotlin Test |

Se priorizarán componentes de código abierto y licencias compatibles con la distribución del SDK. Las versiones y licencias concretas se verificarán antes de integrar dependencias y modelos.

## 9. Compatibilidad de plataformas

### Android
- Integración mediante Kotlin.
- Acceso al micrófono.
- Push-to-talk.
- Detección de «Oye Lisom».
- Servicio en primer plano cuando sea necesario y permitido.
- Procesamiento local.

### Windows
- Integración mediante Kotlin/JVM.
- Acceso al micrófono.
- Reconocimiento local.
- Activación por palabra clave mientras el proceso esté activo.
- Comunicación con aplicaciones compatibles.

### iOS
- Integración mediante Kotlin/Native.
- Acceso al micrófono con permisos.
- Push-to-talk.
- Reconocimiento local.
- Activación por palabra clave cuando las condiciones del sistema lo permitan.

La escucha permanente en segundo plano no estará garantizada en iOS debido a las restricciones del sistema operativo.

La compilación y pruebas nativas de iOS requerirán macOS y Xcode. El desarrollo desde Windows podrá cubrir código compartido y JVM, pero no sustituye ese entorno nativo.

## 10. Funcionamiento sin servidores

Lisom no utilizará:
- AWS.
- VPS.
- Servidores privados.
- API REST externas.
- Whisper en la nube.
- Servicios de reconocimiento de pago.
- Suscripciones.
- Cuentas obligatorias.

Los modelos necesarios se distribuirán con la aplicación o se instalarán localmente.

El reconocimiento, la activación y la interpretación funcionarán sin Internet.

La aplicación anfitriona podrá utilizar sus propias fuentes de información o catálogo, pero eso no formará parte de la infraestructura de Lisom.

Las herramientas de compilación podrán necesitar Internet para descargar dependencias durante el desarrollo; esto no implica tráfico remoto durante el uso del SDK.

## 11. Plan de desarrollo

### Fase 1 — Preparación
- Crear el repositorio LisomAssistant.
- Configurar Kotlin Multiplatform.
- Crear los módulos.
- Definir interfaces públicas.
- Configurar Gradle.

### Fase 2 — Motor de reconocimiento
- Integrar Sherpa-ONNX.
- Incorporar un modelo ASR en español.
- Implementar captura de audio.
- Convertir voz en texto.
- Realizar pruebas offline.

### Fase 3 — Activación por voz
- Integrar detección de palabras clave.
- Preparar el modelo «Oye Lisom».
- Implementar estados de activación.
- Reducir falsos positivos.
- Probar el consumo de recursos.

### Fase 4 — Interpretación de órdenes
- Crear el intérprete NLU.
- Implementar comandos musicales.
- Reconocer diferentes formas de expresar una orden.
- Añadir tolerancia a errores de reconocimiento.
- Implementar pruebas unitarias.

### Fase 5 — Respuesta hablada
- Seleccionar motor TTS local.
- Integrar voz española.
- Generar respuestas.
- Gestionar interrupciones y reproducción de audio.

### Fase 6 — Adaptadores de plataforma
- Implementar Android.
- Implementar Windows.
- Preparar iOS.
- Verificar permisos y restricciones de audio.

### Fase 7 — Integración
- Integrar Lisom SDK en Lynox Music.
- Conectar comandos con el reproductor.
- Probar búsqueda musical.
- Probar reproducción y controles.
- Verificar integración con letras.

### Fase 8 — Publicación
- Documentar la API.
- Preparar versiones compiladas.
- Añadir pruebas automatizadas.
- Revisar licencias.
- Publicar versiones del SDK.

## 12. Requisitos de calidad

- Funcionamiento offline obligatorio.
- Sin dependencia de infraestructura externa.
- Separación entre motor y aplicación anfitriona.
- Código modular y reutilizable.
- Gestión de memoria y consumo de batería.
- Manejo de errores de micrófono.
- Gestión de permisos.
- Pruebas en diferentes condiciones de ruido.
- Compatibilidad con distintos modelos de dispositivos.

## 13. Alcance de la primera versión

La primera versión funcional deberá incluir:
- Reconocimiento de voz en español.
- Botón push-to-talk.
- Activación «Oye Lisom».
- Interpretación de comandos básicos.
- Respuestas mediante voz.
- API pública para aplicaciones.
- Integración de prueba con Lynox Music.
- Funcionamiento sin Internet.

## 14. Resultado esperado

Al finalizar, Lisom Assistant será una librería SDK independiente que cualquier aplicación compatible podrá integrar.

Lynox Music será su primera aplicación anfitriona, pero no será su única posible aplicación.

**Definición final del producto:**

Lisom Assistant es un motor de voz multiplataforma, modular, reutilizable y completamente local, diseñado para permitir que las aplicaciones incorporen reconocimiento de voz, activación por palabra clave, interpretación de instrucciones y respuestas habladas sin depender de servidores externos.

---

## Registro de implementación

Este plan define el objetivo del producto. El estado real de cada fase se registra por separado en [docs/ROADMAP.md](ROADMAP.md). Una fase no se considerará terminada solo por estar descrita en este documento.
