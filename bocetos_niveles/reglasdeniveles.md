# MANUAL TÉCNICO Y REGLAS DE DISEÑO DE NIVELES
## Orion's Journey — Motor Android 2D

Este documento define la **normativa estándar, física exacta, métricas y límites matemáticos** para el diseño y construcción de niveles en **Orion's Journey**.

Cualquier nivel diseñado por un humano (en papel o texto) o generado por un agente de Inteligencia Artificial debe cumplir **estrictamente** estas reglas para garantizar jugabilidad perfecta, sin colisiones erróneas, sin monedas inalcanzables y con coherencia estética.

---

## 1. FÍSICA EXACTA DEL MOTOR DEL JUEGO

El motor corre internamente en bucle de simulación por ticks (`dtTicks = 1.0` a 60 FPS):

| Variable Física | Valor en Código | Impacto en el Juego |
| :--- | :--- | :--- |
| **Velocidad de Disparo (`launchSpeed`)** | `40.0f` px/tick | Orion sale impulsado a 40 unidades por tick en la dirección del ángulo. |
| **Desplazamiento de Boca (`muzzleOffset`)** | `32.0f` px | Orion nace a 32 px del centro del cañón hacia donde apunta. |
| **Tiempo de Inmunidad (`cannonIgnoreTimer`)** | `0.22` segundos | Orion ignora el cañón que lo disparó para evitar re-entrar de inmediato. |
| **Gravedad (`gravity`)** | `+1.0f` px/tick² (hacia abajo) | Hace que cualquier tiro horizontal o ascendente describa una **parábola**. |
| **Fricción del Aire (`drag`)** | `0.98` (2% por frame) | Reduce progresivamente la velocidad horizontal y vertical. |
| **Fuerza de Control Aéreo (`airSteer`)** | `±0.8f` px/tick | Al presionar izquierda/derecha (o inclinar pantalla), Orion se desvía lateralmente. |

### Capacidad Real de Desvío Aéreo del Jugador
Durante un salto en el aire, el jugador puede corregir su trayectoria lateral en las siguientes magnitudes:
* **Vuelo Corto (0.3s / 20 frames):** Desvío de **±35 a 50 px**.
* **Vuelo Medio (0.6s / 40 frames):** Desvío de **±80 a 110 px**.
* **Vuelo Largo (0.9s / 60 frames):** Desvío de **±140 a 180 px**.

---

## 2. MÉTRICAS Y LÍMITES ESTRICTOS ENTRE CAÑONES

### Dimensiones y Hitboxes
* **Sprite visual del cañón:** $170 \times 112$ px renderizado a escala $0.75 \times \approx \mathbf{128 \times 84\text{ px}}$.
* **Hitbox de captura del cañón:** Radio $\mathbf{52\text{ px}}$ (diámetro $104\text{ px}$).
* **Hitbox de recolección de moneda:** Radio $\mathbf{48\text{ px}}$ (diámetro $96\text{ px}$).
* **Hitbox del Aro Gigante de Meta:** Radio $\mathbf{82\text{ px}}$ (diámetro $164\text{ px}$).
* **Hitbox de obstáculos sólidos (piano, yunque, cactus):** Radio $\mathbf{62\text{ px}}$.

```
   ┌───────── 128 px ─────────┐
   │         Cañón A          │
   └───────────( O )──────────┘
                 │
                 │  DISTANCIA ENTRE CENTROS (D)
                 │  • Mínimo Absoluto: 180 px (1 moneda)
                 │  • Mínimo Recomendado: 220 px (2-3 monedas)
                 │  • Estándar Medio: 280 - 360 px (4-5 monedas)
                 │  • Salto Largo: 400 - 500 px (6-8 monedas)
                 │  • Máximo con Ajuste: 550 px (en subida) / 650 px (en bajada)
                 ↓
   ┌───────────( O )──────────┐
   │         Cañón B          │
   └──────────────────────────┘
```

### Tabla de Límites de Distancia (Centro a Centro)

| Clasificación | Distancia (px) | Cantidad de Monedas | Cuándo Usar |
| :--- | :---: | :---: | :--- |
| **PROHIBIDO** | `< 180 px` | 0 | **Inadmisible.** Los cañones se solapan visualmente y sus hitboxes colisionan. |
| **Corta (Rápida)** | `180 - 240 px` | 2 a 3 monedas | Ritmo rápido, transiciones ágiles de ángulo. |
| **Media (Estándar)** | `250 - 360 px` | 4 a 5 monedas | La distancia más equilibrada y placentera del juego. |
| **Larga (Vuelo amplio)** | `380 - 480 px` | 6 a 8 monedas | Para sentir la velocidad y altura del vuelo. |
| **Máxima (Ajuste aéreo)** | `500 - 550 px` | 8 a 10 monedas | **Exige corrección aérea:** el jugador debe maniobrar para atrapar el cañón. |
| **Caída Libre Vertical** | `hasta 650 px` | 6 a 10 monedas | Solo hacia abajo ($+90^\circ$ o $+45^\circ$). La gravedad asiste la caída. |
| **PROHIBIDO** | `> 550 px en subida` | — | **Orion caerá por gravedad** antes de alcanzar el cañón objetivo. |

---

## 3. LÍMITES VERTICALES DEL MUNDO (SUELO Y TECHO)

Cada nivel define en su cabecera `groundY` (la arena / suelo de muerte):

1. **Margen de Seguridad del Suelo (`groundY`):**
   * El cañón más bajo del nivel debe estar **al menos a 150 px por encima de `groundY`**.
   * *Ejemplo:* Si `groundY = 700`, ningún cañón puede tener $Y > 550$.
   * La parábola de cualquier disparo descendente nunca debe cortar $Y \ge groundY$.

2. **Margen del Techo (`ceilingY`):**
   * En Mundo 1 y 2 el techo automático es $Y_{\min} - 2500$ px (espacio de sobra).
   * En Mundo 3 y superiores el techo baja a $Y_{\min} - 650$ px. Ningún disparo vertical debe sobrepasar ese umbral.

---

## 4. ESTÁNDAR DE MONEDAS (PUFFLE-OS)

Para desbloquear el siguiente nivel con calificación perfecta, el juego exige recoger el **90% del total de monedas**.

### Estándares Oficiales de Cantidad
* **Nivel Corto / Tutorial (12 - 16 cañones):** Exactamente **50 a 60 monedas**.
* **Nivel Regular (18 - 24 cañones):** Exactamente **75 monedas** *(Estándar Recomendado)*.
* **Nivel Complejo / Desafío (25 - 32 cañones):** Exactamente **100 monedas** *(Estándar Máximo)*.

### Reglas Obligatorias de Posicionamiento de Monedas
1. **Cero Monedas Huérfanas:** Cada moneda debe estar ubicada sobre la línea de tiro real que une dos cañones o en el arco parabólico de vuelo.
2. **Separación Uniforme:** Las monedas de un tramo deben repartirse mediante interpolación equitativa:
   $$t = \frac{k}{N + 1} \quad \text{para } k = 1 \dots N$$
   $$X_k = X_1 + t \cdot (X_2 - X_1), \quad Y_k = Y_1 + t \cdot (Y_2 - Y_1)$$
3. **Monedas en Curva de Gravedad:**
   * En tramos cortos y medios ($D \le 350$ px), la línea recta coincide con la trayectoria.
   * En saltos largos ($D > 380$ px con ángulo horizontal o diagonal), la trayectoria cae entre $30$ y $80$ px por gravedad. Las monedas deben curvarse ligeramente hacia abajo para que el vuelo natural las atrape todas.

---

## 5. CATÁLOGO DE CAÑONES DEL MOTOR

| Tipo | Color / Sprite | Comportamiento | Parámetros CSV |
| :---: | :--- | :--- | :--- |
| **`1`** | **Verde** (`cannon_green`) | **Automático Instantáneo:** Dispara de inmediato al entrar en el ángulo configurado. | `1, x, y, angle, 1, 1` |
| **`11`** | **Azul** (`cannon_blue`) | **Manual Fijo:** Espera que el jugador toque la pantalla para disparar en un ángulo fijo. | `11, x, y, angle, 1, 1` |
| **`2`** | **Azul** (`cannon_blue`) | **Manual Oscilante Horario:** Oscila $-90^\circ$ (o el paso configurado) con pausa en extremos. | `2, x, y, angle, 1, 1 [, rotStep, numSteps]` |
| **`3`** | **Azul** (`cannon_blue`) | **Manual Oscilante Antihorario:** Oscila $+90^\circ$ ping-pong. | `3, x, y, angle, 1, 1 [, rotStep, numSteps]` |
| **`6`** | **Azul** (`cannon_blue`) | **Manual Giratorio 360°:** Rota en 4 pasos continuos de $+90^\circ$. | `6, x, y, angle, 1, 1` |
| **`7`** | **Azul** (`cannon_blue`) | **Manual Gran Amplitud:** Oscila 2 pasos de $-90^\circ$ (amplitud total $180^\circ$). | `7, x, y, angle, 1, 1` |
| **`8`** | **Azul** (`cannon_blue`) | **Manual Gran Amplitud:** Oscila 2 pasos de $+90^\circ$ (amplitud total $180^\circ$). | `8, x, y, angle, 1, 1` |
| **`12`** | **Azul Móvil** | **Móvil Horizontal (+X):** Se desplaza $+240$ px a la derecha al entrar Orion. | `12, x, y, angle, 1, 1` |
| **`13`** | **Azul Móvil** | **Móvil Vertical (+Y):** Se desplaza $+240$ px hacia abajo al entrar Orion. | `13, x, y, angle, 1, 1` |
| **`17`** | **Azul Móvil** | **Móvil Horizontal (-X):** Se desplaza $-240$ px a la izquierda al entrar Orion. | `17, x, y, angle, 1, 1` |
| **`18`** | **Azul Móvil** | **Móvil Vertical (-Y):** Se desplaza $-240$ px hacia arriba al entrar Orion. | `18, x, y, angle, 1, 1` |
| **`19`** | **Azul Patrulla** | **Patrulla Continua Horizontal:** Se mueve permanentemente $\pm 240$ px. | `19, x, y, angle, 1, 1` |
| **`20`** | **Azul Patrulla** | **Patrulla Continua Vertical:** Se mueve permanentemente $\pm 240$ px. | `20, x, y, angle, 1, 1` |
| **`14`** | **Rojo** (`cannon_red`) | **Rápido Continuo:** Rota sin pausa en 5 pasos de $+72^\circ$ (exige timing preciso). | `14, x, y, angle, 1, 1` |
| **`15`** | **Morado** (`cannon_purple`) | **Controlable:** El jugador arrastra el dedo en pantalla para rotarlo libremente en 360°. | `15, x, y, angle, 1, 1` |
| **`16`** | **Checkpoint** (`cannon_checkpoint`) | **Punto de Control:** Al entrar, guarda el punto de reaparición si el jugador muere. | `16, x, y, angle, 1, 1` |

---

## 6. CATÁLOGO DE OBSTÁCULOS E INTERACTIVOS

| Tipo | Objeto | Efecto Físico sobre Orion |
| :---: | :--- | :--- |
| **`3`** | **Globo Verde** | Rebote suave hacia arriba ($Vy = -28$, amortigua $Vx$). |
| **`10`** | **Globo Rosa** | Súper rebote vertical elástico ($Vy = -38$). |
| **`9`** | **Nube de Vapor** | Se desvanece al contacto. Permite ocultar rutas o monedas secretas. |
| **`11`** | **Tirachinas / Slingshot** | Captura a Orion. El jugador estira hacia atrás y suelta para salir disparado. |
| **`12 - 16`** | **Ventilador / Corriente de Aire** | Empuja continuamente a Orion en la dirección del viento (fuerza $1.12$, alcance $350$ px). |
| **`1, 2, 4, 17`** | **Pianos / Cactus / Rocas** | Obstáculo sólido. Rebota violentamente a Orion perdiendo velocidad. |
| **`19 - 22`** | **Yunques de Acero** | Obstáculo pesado de caída o patrulla. Rebote seco. |
| **`25, 27`** | **Jefe / Boss** | Requiere varios impactos ($HP=8$) para destruirse y liberar el Aro de Meta. |

---

## 7. DISEÑO DE JUGABILIDAD: RUTAS, BIFURCACIONES Y HABILIDAD

Un gran nivel nunca es un pasillo recto aburrido. Debe alternar:

### A. Bifurcación (Decisión Riesgo vs Recompensa)
Un cañón oscilante azul ofrece dos salidas:
* **Ruta Rápida (Directa):** Apunta directo al siguiente cañón seguro hacia la meta (fácil, da solo 2-3 monedas).
* **Ruta de Monedas (Riesgosa):** Apunta a un bucle superior o lateral que contiene 8-12 monedas, pero requiere saltos más precisos o pasa cerca de un obstáculo, regresando luego al camino principal.

```
                  ┌── [CV 4] ──(••••••)──> [CV 5] ──┐  (Ruta Monedas: +12)
                  │                                  ↓
[CA 1] ──(••)──> [CA 2] ────────(••••)───────────> [CA 6] ──> [META]
                       (Ruta Rápida Directa: +4)
```

### B. Ajuste Aéreo Obligatorio (Skill Shot)
* El cañón emisor apunta en un ángulo que pasaría a **60 - 90 px** al costado del cañón receptor.
* Las monedas se colocan curvadas hacia la boca receptora.
* El jugador **debe mantener presionado izquierda/derecha en el aire** para corregir su trayectoria. Si no lo hace, pasa de largo y cae al vacío.

### C. Trampa de Timing (Ventana de Oportunidad)
* Un cañón automático receptor móvil (tipo 19 patrullando) o un obstáculo que cruza la línea.
* El jugador en el cañón manual azul debe esperar a que el receptor esté alineado antes de disparar.

---

## 8. SINTAXIS ESTÁNDAR DEL ARCHIVO `.txt` DEL NIVEL

Los archivos se guardan en `app/src/main/assets/levels/LevelX.txt` y usan **punto decimal (`.`) estricto**:

```csv
worldNum,groundY,startX,startY,finishX,finishY,totalRings

[Background]
type,x,y,scaleX,scaleY

[Cannons]
type,x,y,angle,scaleX,scaleY[,rotStep,numSteps]

[Obstacles]
type,x,y,angle,scaleX,scaleY

[PuffleOs]
type,x,y,scaleX,scaleY
```

* **Línea 1:**
  * `worldNum`: Número de mundo (1 = Ruinas Cósmicas, 2 = Desierto/Cactus, 3 = Nieve/Hielo, 4 = Espacio).
  * `groundY`: Límite inferior donde Orion muere (ejemplo: `700.00`).
  * `startX, startY`: Coordenadas iniciales donde cae Orion (ejemplo: `100.00, 400.00`).
  * `finishX, finishY`: Coordenadas del gran Aro de Meta (ejemplo: `2400.00, -300.00`).
  * `totalRings`: Cantidad total de monedas del nivel (ejemplo: `75` o `100`).

---

## 9. FORMATO ASCII ESTÁNDAR PARA DIBUJAR EN PAPEL O TEXTO

Cuando diseñes un nivel para pasárselo al asistente o a otro diseñador, utiliza esta plantilla normalizada:

```
=====================================================
NIVEL [NÚMERO] - [TÍTULO DEL NIVEL]
=====================================================
MUNDO: 1
MONEDAS TOTALES: 75
GROUND_Y: 700

LEYENDA:
CA(ángulo) = Cañón Azul Manual
CV(ángulo) = Cañón Verde Automático
CP         = Cañón Checkpoint
CM         = Cañón Morado Manual 360°
(••••)     = Monedas (cada punto = 1 moneda)
[AJUSTE]   = Requiere presionar izquierda/derecha en el aire
[DECISIÓN] = Bifurcación entre ruta segura y ruta de monedas

MAPA:
[INICIO] (X=100, Y=350)
   ↓
[CA 1] (-45°) ──(••••)──> [CV 2] (0°) ──(••••)──> [CA 3] (-90° / 0°)
                                                       │
                                  ┌────────────────────┴────────────────────┐
                           [RUTA RÁPIDA]                             [RUTA MONEDAS]
                                  │ (0°)                                    │ (-90°)
                               (••••)                                   (••••••••)
                                  ↓                                         ↓
                               [CV 4]                                    [CV 5]
                                  │                                         │
                                  └────────────────────┬────────────────────┘
                                                       ↓
                                                    [CA 6] ──(••••••)──> [META]
=====================================================
```

---

## 10. REGLA DE ORO DE VERIFICACIÓN (CHECKLIST PRE-COMPILACIÓN)

Antes de generar el APK con un nivel nuevo, verificar automáticamente:
1. [ ] **Distancias:** Ningún par de cañones consecutivos está a menos de **180 px** ni a más de **550 px** (en subida).
2. [ ] **Superposición:** Ningún centro de cañón está a menos de **150 px** de otro elemento estático.
3. [ ] **Piso:** Ningún cañón tiene $Y > groundY - 150$.
4. [ ] **Monedas:** La suma de todas las monedas del archivo es exactamente igual al número en la cabecera (`totalRings`).
5. [ ] **Decimales:** Todos los números flotantes usan punto (`.`) y **jamás coma (`,`)**.
6. [ ] **Rutas:** Cada cañón automático apunta directamente al hitbox de captura (radio 52) del cañón siguiente.
7. [ ] **Aro Meta:** Colocado a distancia segura de disparo final (250 a 400 px del último cañón).
