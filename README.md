# Orion's Journey (*The Journey of Orion*)

**Orion's Journey** es un videojuego arcade de física balística 2D, precisión y aventura atmosférica para dispositivos **Android modernos (Android 8.0 a Android 17+, API 26–35+)**, dirigido y desarrollado por **Cristian Camilo Tabares** (**Cristian Camilo Tabares Development**).

---

## 1. Ficha Técnica del Proyecto

| Parámetro Técnico | Especificación |
| :--- | :--- |
| **Nombre Oficial** | **Orion's Journey** (*The Journey of Orion*) |
| **Director y Desarrollador** | **Cristian Camilo Tabares** (`Cristian Camilo Tabares Development`) |
| **Application ID / Package** | `com.cristiantabares.orionsjourney` |
| **Lenguaje y Motor** | **Java 11 / Android Native 2D Canvas Engine** (`SurfaceView` a 60–120 FPS con sub-stepping físico) |
| **Versión Mínima (`minSdk`)** | **API 26** (Android 8.0 Oreo) |
| **Versión Objetivo (`targetSdk`)** | **API 34 / 35** (Android 14 / 15 / 16 / 17 — probado en **Samsung Galaxy S25 Ultra**) |
| **Arquitectura Binaria** | **Universal 64-bit (`arm64-v8a` / `x86_64`)** — 100% compatible con páginas de memoria de **16 KB** |
| **Orientación y Pantalla** | `sensorLandscape` con modo inmersivo `Edge-to-Edge` y soporte para recortes de cámara (*Display Cutout*) |
| **Esquema de Firma APK** | **APK Signature Scheme v1 + v2 + v3** |

---

## 2. Arquitectura del Motor (`com.cristiantabares.orionsjourney`)

El código fuente principal se encuentra en `app/src/main/java/com/cristiantabares/orionsjourney/`:

1. **[`MainActivity.java`](app/src/main/java/com/cristiantabares/orionsjourney/MainActivity.java)**:
   - Gestiona el ciclo de vida de Android (`onResume`, `onPause`, `onDestroy`), la configuración de pantalla completa inmersiva (`WindowInsetsController`) y la navegación hacia atrás entre niveles, selector de mundos y menú principal.
2. **[`OrionGameView.java`](app/src/main/java/com/cristiantabares/orionsjourney/OrionGameView.java)**:
   - **Game Loop y Física Balística**: Hilo dedicado (`OrionGameThread`) con paso de tiempo delta (`dtSec`), integración cinemática con **3 sub-pasos por frame** para detección continua de colisiones, gravedad calibrada, resistencia aerodinámica y control de dirección en el aire mediante acelerómetro (**TILT**) o controles táctiles (**TOUCH**).
   - **Sistemas de Cañones y Propulsores Futuristas (Modelos 360° Metalizados)**:
     - *Azul Normal (`cannon_blue.png`)*: Cápsula táctil de disparo manual fijo con aletas estabilizadoras y flecha direccional hacia la boquilla.
     - *Rojo Rápido (`cannon_red.png`)*: Propulsor blindado de alta velocidad con turbinas laterales, llama de escape y franjas de advertencia.
     - *Morado Direccionable 360° (`cannon_purple.png`)*: Cañón rotatorio con pantalla de radar, visor óptico y engranaje de orientación accionado por el jugador.
     - *Amarillo Checkpoint (`cannon_checkpoint.png`)*: Módulo de exploración solar con indicador de bandera y aura holográfica de punto de control al activarse.
     - *Verde Automático (`cannon_green.png`)*: Drone de lanzamiento inmediato con ojo robótico, cables de potencia y boquilla de plasma.
     - *Cañones Desplazables (`Tipos 12, 13, 17, 18, 19, 20, 21, 22`)*: Se trasladan horizontal o verticalmente en el mapa conservando fijo su ángulo de propulsión.
   - **Obstáculos Interactivos y Jefes**:
     - Rebotadores elásticos, globos de impulso, nubes efímeras, corrientes de viento de ventiladores, resorteras vectoriales (`Slingshot`) y batallas contra naves jefe en los niveles **12** y **24**.
   - **Menú Principal con Parallax 2D Multicapa y Estética Post-Apocalíptica**:
     - Fondo ilustrado ([`menu_wallpaper.jpg`](app/src/main/assets/sprites/menu_wallpaper.jpg)) acompañado de **5 capas de profundidad Parallax** controladas en tiempo real por el acelerómetro 2D del dispositivo (`tiltX`, `tiltY`), con estructuras de edificios en ruinas ([`menu_ruin_01..05.png`](app/src/main/assets/sprites/menu_ruin_01.png)) y fragmentos de roca ([`menu_rock_01..04.png`](app/src/main/assets/sprites/menu_rock_01.png)) levitando en gravedad cero junto a partículas de polvo cósmico.
     - Paleta cromática unificada con las cinemáticas (obsidiana `#14100D`, bronce envejecido `#B88A58`, marfil cerámico `#F4EFE6`, verde esmeralda antiguo `#1E826E` y cian bioluminiscente `#4CE8F5`) aplicada en el Menú Principal, Selector de Mundos (*Crónicas del Cosmos*) y Selector de Niveles.
     - Banda sonora orquestal épica y esperanzadora con arpegios estelares ([`menu_orion.wav`](app/src/main/assets/audio/menu_orion.wav)).
   - **Cinemática Narrativa Interactiva (`STATE_STORY_CUTSCENE`)**:
     - Sistema de presentación estilo observatorio con **10 planos ilustrados** (`assets/story/plano1.jpg` a `plano10.jpg`) y **16 fragmentos de subtítulos** bilingües (ES/EN) antes del **Nivel 1** (con botón **`🎬 HISTORIA` / `🎬 STORY`** en el selector de niveles para volver a verla en cualquier momento).
     - Efecto de escritura letra por letra estilo máquina de escribir (*typewriter*) con sonido sutil (`typewriter_tick.wav`), navegación libre (`← ANTERIOR`, `SIGUIENTE →`, toque en pantalla, `SALTAR ⏭` y **`▶ INICIAR NIVEL`** en la última diapositiva) y banda sonora orquestal original melancólica y esperanzadora (`story_intro.wav`).
   - **Progresión Exigente del 90% y Multilenguaje**:
     - Desbloqueo progresivo que exige recolectar al menos el **90% de los aros** de cada nivel para habilitar el siguiente nivel y el siguiente mundo.
     - Selector de idioma en tiempo real (**Español / Inglés**) desde la pantalla principal.
3. **[`LevelData.java`](app/src/main/java/com/cristiantabares/orionsjourney/LevelData.java)**:
   - Analizador (*parser*) de los **36 niveles** estructurados en archivos de texto plano (`assets/levels/Level1.txt` .. `Level36.txt`), divididos en 3 mundos de 12 niveles cada uno:
     - **Mundo 1**: *Blue Sky* (Niveles 1–12)
     - **Mundo 2**: *Sunset Horizon* (Niveles 13–24)
     - **Mundo 3**: *Box Dimension* (Niveles 25–36)
4. **[`SpriteCache.java`](app/src/main/java/com/cristiantabares/orionsjourney/SpriteCache.java)**:
   - Sistema de caché en memoria `ARGB_8888` que carga sprites individuales, centrados y libres de residuos desde `assets/sprites/`, incluyendo los **10 trajes de Orion** (`orion_suit_01_solar.png` a `orion_suit_10_emerald.png`), los 6 cuadros de animación en fuego (`orion_fire_01..06.png`), cañones, anillos coleccionables, obstáculos y elementos de interfaz.
5. **[`AudioController.java`](app/src/main/java/com/cristiantabares/orionsjourney/AudioController.java)**:
   - Motor de audio dual basado en `MediaPlayer` para las pistas musicales en bucle de cada mundo/jefe y `SoundPool` de baja latencia (hasta 10 canales simultáneos con variación dinámica de tono/pitch al recolectar anillos) desde `assets/audio/`.

---

## 3. Estructura de Directorios de Recursos (`assets/`)

```text
app/src/main/
├── AndroidManifest.xml
├── assets/
│   ├── audio/          # Música de mundos (.mp3), banda sonora orquestal (story_intro.wav) y SFX (.wav)
│   ├── levels/         # Definición de coordenadas y entidades de los 36 niveles (Level1.txt - Level36.txt)
│   ├── sprites/        # Sprites individuales en PNG (Personaje Orion, trajes, cañones, obstáculos y UI)
│   └── story/          # Planos ilustrados de la cinemática (plano1.jpg - plano10.jpg) y guion (historia.txt)
├── java/com/cristiantabares/orionsjourney/
│   ├── AudioController.java
│   ├── LevelData.java
│   ├── MainActivity.java
│   ├── OrionGameView.java
│   └── SpriteCache.java
└── res/
    ├── drawable/       # Recursos gráficos generales y tema Fullscreen
    ├── mipmap-mdpi/    # Iconos estándar y redondos 48x48 px
    ├── mipmap-hdpi/    # Iconos estándar y redondos 72x72 px
    ├── mipmap-xhdpi/   # Iconos estándar y redondos 96x96 px
    ├── mipmap-xxhdpi/  # Iconos estándar y redondos 144x144 px
    ├── mipmap-xxxhdpi/ # Iconos estándar y redondos 192x192 px
    ├── values/         # Strings en inglés y tema Fullscreen
    └── values-es/      # Strings localizados en español
```

---

## 4. Compilación y Generación del APK

El proyecto es compatible tanto con **Android Studio / Gradle** como con compilación directa mediante el **Android SDK Build-Tools (`aapt2`, `javac`, `d8`, `zipalign`, `apksigner`)**. Los recursos dentro de `assets/` se empaquetan sin compresión (`STORED`) y con rutas POSIX (`/`) para acceso directo mediante `AssetManager`.

---

**© Cristian Camilo Tabares • Cristian Camilo Tabares Development**
