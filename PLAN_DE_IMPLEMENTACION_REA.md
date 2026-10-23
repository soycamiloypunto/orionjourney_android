# Plan de Implementación REA & Catálogo Maestro de Recursos — *Orion's Journey*

> [!IMPORTANT]
> **Identidad del Proyecto Actualizada**:
> - **Título oficial**: **Orion's Journey** (*The Journey of Orion*)
> - **Dirección y Desarrollo**: **Cristian Camilo Tabares • Cristian Camilo Tabares Development**
> - **Personaje Principal**: **Orion** (con sistema de **Cambio de Traje / `SUIT`** de 10 trajes independientes centrados al píxel sin residuos de atlas).
> - **Package ID**: `com.cristiantabares.orionsjourney`
> - **APK Generado (Android 14 / 15 / 16 / 17 — Samsung Galaxy S25 Ultra)**: [OrionsJourney_S25Ultra_Android17.apk](file:///D:/CCTV/Projects/mobile/pufflenew/OrionsJourney_S25Ultra_Android17.apk)

---

## 1. Correcciones Visuales Aplicadas (Centrado de Orion, Menú de Niveles y Cielo Limpio)

1. **Extracción Individual y Centrado Exacto de los 10 Trajes de Orion (`orion_suit_01..10`)**:
   - **Causa raíz**: En el juego original las versiones del personaje venían pegadas dentro del atlas `texture_38f963.png` (`256x256`) y un corte por rejilla uniforme dejaba una línea de píxeles del personaje de la fila/columna vecina (el residuo lateral que viste en tu captura).
   - **Solución**: Se aislaron las bandas exactas de píxeles transparentes de cada traje y se exportaron como **10 archivos PNG independientes, limpios y centrados geométricamente en lienzos cuadrados (`53x53 px` con margen transparente simétrico)**. Ahora puedes abrir y editar cualquiera de los 10 archivos individuales sin tocar un atlas compartido.
2. **Corrección del Menú de Selección de Niveles (`level_btn_blue/red/yellow.png`)**:
   - **Causa raíz**: Las texturas originales `LevelSelectButtonBlue/Red/Yellow.png` contenían dos estados del botón juntos y su ancho no era simétrico, dejando una franja del segundo botón a la derecha; además, al estirarse al ancho de la pantalla `20:9` del S25 Ultra se deformaban horizontalmente.
   - **Solución**: Se extrajo únicamente el botón principal limpio en archivos cuadrados independientes (`level_btn_blue.png`, `level_btn_red.png`, `level_btn_yellow.png`) y en [`OrionGameView.java`](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/java/com/cristiantabares/orionsjourney/OrionGameView.java) se dibuja con proporción `1:1` cuadrada perfecta y una píldora inferior separada para el puntaje de anillos.
3. **Pantalla Principal Limpia (`renderMainMenu`)**:
   - Se eliminaron el cañón, el personaje saliendo disparado, la pila inferior de aros, los logos antiguos y el pino nevado (`bgArt04_texture_00.png`) del cielo del menú principal, dejando únicamente el cielo azul con nubes suaves (`bgArt01..03`), el título **ORION'S JOURNEY**, los botones (`PLAY`, `SUIT: [Nombre]`, `CONTROL`, `SOUND`) y tu crédito: **`Directed by Cristian Camilo Tabares • Cristian Camilo Tabares Development`**.

---

## 2. Catálogo Exhaustivo de Todos los Recursos (Sprites, Sonidos y Niveles)

Todos los archivos del juego están desacoplados como archivos individuales en:
`D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\`

Puedes reemplazar cualquier archivo `.png`, `.mp3`, `.wav` o `.txt` **uno por uno** conservando el mismo nombre de archivo y el juego lo cargará automáticamente.

### 2.1. Personaje Principal: Orion — Trajes (`assets/sprites/`)

| # | Nombre del Traje en el Juego | Archivo (Click para abrir) | Ruta Absoluta | Dimensiones | Uso en el Juego |
| :-: | :--- | :--- | :--- | :-: | :--- |
| 1 | **SOLAR** (Amarillo) | [orion_suit_01_solar.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_suit_01_solar.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_suit_01_solar.png` | `53x53 px` | Traje 1 de Orion (centrado sin residuos) |
| 2 | **COSMIC** (Morado) | [orion_suit_02_cosmic.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_suit_02_cosmic.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_suit_02_cosmic.png` | `53x53 px` | Traje 2 de Orion (centrado sin residuos) |
| 3 | **FROST** (Blanco) | [orion_suit_03_frost.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_suit_03_frost.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_suit_03_frost.png` | `51x51 px` | Traje 3 de Orion (centrado sin residuos) |
| 4 | **AMBER** (Naranja) | [orion_suit_04_amber.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_suit_04_amber.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_suit_04_amber.png` | `53x53 px` | Traje 4 de Orion (centrado sin residuos) |
| 5 | **TERRA** (Café) | [orion_suit_05_terra.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_suit_05_terra.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_suit_05_terra.png` | `51x51 px` | Traje 5 de Orion (centrado sin residuos) |
| 6 | **AZURE** (Azul - Default) | [orion_suit_06_azure.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_suit_06_azure.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_suit_06_azure.png` | `53x53 px` | Traje 6 por defecto de Orion |
| 7 | **CRIMSON** (Rojo) | [orion_suit_07_crimson.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_suit_07_crimson.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_suit_07_crimson.png` | `51x51 px` | Traje 7 de Orion (centrado sin residuos) |
| 8 | **STARLIGHT** (Rosado) | [orion_suit_08_starlight.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_suit_08_starlight.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_suit_08_starlight.png` | `53x53 px` | Traje 8 de Orion (centrado sin residuos) |
| 9 | **ONYX** (Negro) | [orion_suit_09_onyx.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_suit_09_onyx.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_suit_09_onyx.png` | `53x53 px` | Traje 9 de Orion (centrado sin residuos) |
| 10 | **EMERALD** (Verde) | [orion_suit_10_emerald.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_suit_10_emerald.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_suit_10_emerald.png` | `53x53 px` | Traje 10 de Orion (centrado sin residuos) |

### 2.2. Animación de Orion en Fuego / Modo Turbo (`assets/sprites/`)

| Frame | Archivo (Click para abrir) | Ruta Absoluta | Dimensiones | Uso en el Juego |
| :-: | :--- | :--- | :-: | :--- |
| 1 | [orion_fire_01.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_fire_01.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_fire_01.png` | `214x214 px` | Frame 1 de Orion envuelto en llamas (Modo Turbo) |
| 2 | [orion_fire_02.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_fire_02.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_fire_02.png` | `214x214 px` | Frame 2 de Orion envuelto en llamas (Modo Turbo) |
| 3 | [orion_fire_03.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_fire_03.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_fire_03.png` | `225x225 px` | Frame 3 de Orion envuelto en llamas (Modo Turbo) |
| 4 | [orion_fire_04.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_fire_04.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_fire_04.png` | `225x225 px` | Frame 4 de Orion envuelto en llamas (Modo Turbo) |
| 5 | [orion_fire_05.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_fire_05.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_fire_05.png` | `226x226 px` | Frame 5 de Orion envuelto en llamas (Modo Turbo) |
| 6 | [orion_fire_06.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/orion_fire_06.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\orion_fire_06.png` | `226x226 px` | Frame 6 de Orion envuelto en llamas (Modo Turbo) |

### 2.3. Cañones y Checkpoints (`assets/sprites/`)

| Tipo ID | Archivo (Click para abrir) | Ruta Absoluta | Dimensiones | Mecánica en los Niveles |
| :-: | :--- | :--- | :-: | :--- |
| `0, 1` | [cannon_green.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/cannon_green.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\cannon_green.png` | `140x140 px` | **Cañón Verde**: Disparo automático al entrar |
| `2..5` | [cannon_blue.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src\main\assets\sprites\cannon_blue.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\cannon_blue.png` | `140x140 px` | **Cañón Azul**: Espera toque para disparar (fijo u oscilante) |
| `6..13` | [cannon_purple.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/cannon_purple.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\cannon_purple.png` | `140x140 px` | **Cañón Morado**: Rotación controlada por el jugador (Tilt o botones ⟲/⟳) |
| `14` | [cannon_red.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/cannon_red.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\cannon_red.png` | `140x140 px` | **Cañón Rojo**: Cañón rápido de alta potencia |
| `16, 17` | [cannon_checkpoint.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/cannon_checkpoint.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\cannon_checkpoint.png` | `140x140 px` | **Cañón Checkpoint**: Guarda el punto de reaparición en el nivel |
| Flag | [checkpoint_flag.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/checkpoint_flag.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\checkpoint_flag.png` | `68x68 px` | **Bandera Amarilla**: Aparece sobre el cañón checkpoint al activarse |

### 2.4. Anillos Coleccionables y Meta (`assets/sprites/`)

| Objeto | Archivo (Click para abrir) | Ruta Absoluta | Dimensiones | Función en el Nivel |
| :--- | :--- | :--- | :-: | :--- |
| **Anillo Naranja** | [ring_orange.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/ring_orange.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\ring_orange.png` | `46x46 px` | Coleccionable estándar en niveles normales |
| **Anillo de Plata** | [ring_silver.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/ring_silver.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\ring_silver.png` | `46x46 px` | Medalla / Anillo de rango Plata |
| **Anillo de Oro** | [ring_gold.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/ring_gold.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\ring_gold.png` | `46x46 px` | Coleccionable dorado / Indicador de nivel superado |
| **Anillo de Fuego** | [ring_fire.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/ring_fire.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\ring_fire.png` | `58x58 px` | Coleccionable en Modo Turbo |
| **Gran Anillo Meta** | [giant_ring.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/giant_ring.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\giant_ring.png` | `156x156 px` | **Meta del Nivel (`Type 15`)**: Al tocarlo se completa el nivel |

### 2.5. Obstáculos, Interactivos y Jefes (`assets/sprites/`)

| Tipo ID | Archivo (Click para abrir) | Ruta Absoluta | Dimensiones | Comportamiento en el Nivel |
| :-: | :--- | :--- | :-: | :--- |
| `0..3` | [obstacle_piano.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/obstacle_piano.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\obstacle_piano.png` | `147x147 px` | **Piano**: Rebota a Orion hacia arriba o hacia abajo |
| `4` | [obstacle_cactus.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/obstacle_cactus.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\obstacle_cactus.png` | `124x124 px` | **Cactus**: Obstáculo con rebote elástico lateral |
| `5..8` | [balloon_green.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/balloon_green.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\balloon_green.png` | `118x118 px` | **Globo Verde**: Rebote suave ascendente |
| `5..8` | [balloon_red.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/balloon_red.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\balloon_red.png` | `125x125 px` | **Globo Rojo**: Rebote de gran impulso ascendente |
| `9, 10` | [SlingshotBalloons_texture_01.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/SlingshotBalloons_texture_01.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\SlingshotBalloons_texture_01.png` | `64x64 px` | Globos superiores de la resortera |
| `9, 10` | [SlingshotBarrel_texture_01.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/SlingshotBarrel_texture_01.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\SlingshotBarrel_texture_01.png` | `128x128 px` | Barril elástico de la resortera |
| `9, 10` | [SlingshotRope_texture_01.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/SlingshotRope_texture_01.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\SlingshotRope_texture_01.png` | `32x32 px` | Cuerda de la resortera |
| `11..13` | [cloud_poofy.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/cloud_poofy.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\cloud_poofy.png` | `204x204 px` | **Nube Esponjosa**: Impulsa a Orion al pasar sobre ella |
| `14, 15, 18` | [fan_01.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/fan_01.png), [fan_02.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/fan_02.png), [fan_03.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/fan_03.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\fan_01..03.png` | `187x187 px` | **Ventilador (3 frames)**: Empuja a Orion con corriente de viento |
| `17` | [plant_01.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/plant_01.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\plant_01.png` | `122x122 px` | **Planta Elástica**: Rebote vertical en Mundo 3 |
| `19..22` | [obstacle_anvil.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/obstacle_anvil.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\obstacle_anvil.png` | `125x125 px` | **Yunque Pesado**: Derriba a Orion hacia abajo si lo golpea |
| `Boss 1` | [boss_ship_1.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/boss_ship_1.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\boss_ship_1.png` | `238x238 px` | **Nave del Jefe (Fase 1)**: Aparece en Niveles 12 y 24 |
| `Boss 2` | [boss_ship_2.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/boss_ship_2.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\boss_ship_2.png` | `265x265 px` | **Nave del Jefe (Fase 2)**: Aparece al recibir impactos |
| `Magnet` | [boss_magnet.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/boss_magnet.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\boss_magnet.png` | `135x135 px` | **Imán del Jefe**: Parte inferior de la nave jefe |

### 2.6. Escenas de Mundos, Nubes de Fondo e Interfaz UI (`assets/sprites/`)

| Categoría | Archivo (Click para abrir) | Ruta Absoluta | Uso en el Juego |
| :--- | :--- | :--- | :--- |
| **Escena Mundo 1** | [bluesky_loading.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/bluesky_loading.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\bluesky_loading.png` | Ilustración de tarjeta del Mundo 1 (*Blue Sky*) |
| **Escena Mundo 2** | [sodasunset_loading.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/sodasunset_loading.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\sodasunset_loading.png` | Ilustración de tarjeta del Mundo 2 (*Sunset Horizon*) |
| **Escena Mundo 3** | [bonus_loading.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/bonus_loading.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\bonus_loading.png` | Ilustración de tarjeta del Mundo 3 (*Box Dimension*) |
| **Nubes del Cielo** | `bgArt01_texture_00.png` .. `bgArt03_texture_00.png` | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\bgArt01..03_texture_00.png` | Nubes flotantes del Menú Principal y Mundo 1 |
| **Decoración Niveles** | `bgArt04`, `07`, `09`, `18`, `25`, `28`, `32`, `42`, `53` | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\bgArt*.png` | Parallax de fondo dentro de los niveles de los 3 mundos |
| **Botón Nivel Mundo 1** | [level_btn_blue.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/level_btn_blue.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\level_btn_blue.png` | Botón cuadrado azul de selección de niveles (1–12) |
| **Botón Nivel Mundo 2** | [level_btn_red.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/level_btn_red.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\level_btn_red.png` | Botón cuadrado rojo/naranja de selección de niveles (13–24) |
| **Botón Nivel Mundo 3** | [level_btn_yellow.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/level_btn_yellow.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\level_btn_yellow.png` | Botón cuadrado amarillo de selección de niveles (25–36) |
| **Botón Play** | [play_button.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/play_button.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\play_button.png` | Botón principal `PLAY` en el menú de inicio |
| **Botones Slow-Mo** | [SlowMoButton_Active.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/SlowMoButton_Active.png), [SlowMoButton_Inactive.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/SlowMoButton_Inactive.png) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\SlowMoButton_*.png` | Botón de cámara lenta en niveles con cañones rápidos |
| **Popup Fin de Nivel** | [end-level_popup.png](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/sprites/end-level_popup.png), `end-level_*_button.png` | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\end-level_*.png` | Ventana y botones al completar un nivel |
| **Botones Pausa** | `pause_button.png`, `pause_resume.png`, `pause_restart.png`, `pause_quit.png` | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\sprites\pause_*.png` | Iconos de pausa durante la partida |

---

### 2.7. Música y Efectos de Sonido (`assets/audio/`)

| Tipo | Archivo (Click para abrir) | Ruta Absoluta | Cuándo suena |
| :--- | :--- | :--- | :--- |
| **Música Mundo 1 / Menú** | [Mus_BlueSky_Main.mp3](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/audio/Mus_BlueSky_Main.mp3) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\audio\Mus_BlueSky_Main.mp3` | Menú Principal y Niveles 1 al 11 |
| **Música Mundo 2** | [Mus_SodaSunset_Main.mp3](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/audio/Mus_SodaSunset_Main.mp3) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\audio\Mus_SodaSunset_Main.mp3` | Niveles 13 al 23 (*Sunset Horizon*) |
| **Música Mundo 3** | [Mus_BoxDimension_Main.mp3](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/audio/Mus_BoxDimension_Main.mp3) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\audio\Mus_BoxDimension_Main.mp3` | Niveles 25 al 36 (*Box Dimension*) |
| **Música Jefe (Boss)** | [Mus_Boss_Main.mp3](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/audio/Mus_Boss_Main.mp3) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\audio\Mus_Boss_Main.mp3` | Niveles de Jefe 12 y 24 |
| **SFX Botón UI** | [ButtonClick.wav](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/audio/ButtonClick.wav) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\audio\ButtonClick.wav` | Al tocar cualquier botón del menú |
| **SFX Entrar a Cañón** | [EnterCannon.wav](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/audio/EnterCannon.wav) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\audio\EnterCannon.wav` | Cuando Orion entra en un cañón |
| **SFX Disparo Cañón** | `CannonShot_01_a.wav`, `02_a.wav`, `03_a.wav` | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\audio\CannonShot_*.wav` | Cuando un cañón dispara a Orion |
| **SFX Recoger Anillo** | [PickupPuffleO1.wav](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/audio/PickupPuffleO1.wav) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\audio\PickupPuffleO1.wav` | Al recolectar un anillo en el aire |
| **SFX Rebote Globo** | [BalloonBounce.wav](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/audio/BalloonBounce.wav) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\audio\BalloonBounce.wav` | Al rebotar en globos, nubes o cactus |
| **SFX Rebote Piano** | [Piano_Bounce_a.wav](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/audio/Piano_Bounce_a.wav) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\audio\Piano_Bounce_a.wav` | Al rebotar sobre un piano |
| **SFX Golpe Yunque** | [AnvilHit.wav](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/audio/AnvilHit.wav) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\audio\AnvilHit.wav` | Al chocar contra un yunque o jefe |
| **SFX Nivel Completado** | [LevelComplete.wav](file:///D:/CCTV/Projects/mobile/pufflenew/app/src/main/assets/audio/LevelComplete.wav) | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\audio\LevelComplete.wav` | Al cruzar el Gran Anillo Meta (`Type 15`) |

---

### 2.8. Listado de los 36 Niveles y su Categoría por Mundo (`assets/levels/`)

Cada archivo `LevelN.txt` es un archivo de texto plano que define las coordenadas `(X, Y)`, ángulos y tipos de cañones, obstáculos y anillos del nivel:

| Mundo / Categoría | Niveles Incluidos | Ruta de los Archivos | Niveles Especiales |
| :--- | :--- | :--- | :--- |
| **Mundo 1: Blue Sky** | `Level1.txt` al `Level12.txt` | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\levels\Level1.txt` .. `Level12.txt` | `Level12.txt`: **Batalla de Jefe I** (`boss_ship_1.png`) |
| **Mundo 2: Sunset Horizon** | `Level13.txt` al `Level24.txt` | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\levels\Level13.txt` .. `Level24.txt` | `Level24.txt`: **Batalla de Jefe II** (`boss_ship_2.png`) |
| **Mundo 3: Box Dimension** | `Level25.txt` al `Level36.txt` | `D:\CCTV\Projects\mobile\pufflenew\app\src\main\assets\levels\Level25.txt` .. `Level36.txt` | Niveles avanzados con ventiladores, plantas elásticas y resorteras |
