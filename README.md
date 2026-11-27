# Diario Financiero

Aplicación web personal y local para registrar gastos e ingresos en pesos mexicanos (MXN), controlar presupuestos, dar seguimiento a tarjetas de crédito, importar movimientos bancarios en CSV, programar recurrentes/MSI y escribir notas diarias sobre decisiones de dinero.

Corre únicamente en `127.0.0.1` (localhost), para un solo usuario y con persistencia en un archivo local H2 que permanece en tu equipo.

> **Propósito de pruebas:** Además de ser una aplicación funcional, este proyecto también fue creado para practicar y ejecutar **pruebas unitarias y análisis de cobertura** usando inicialmente `mvn test` (JUnit 5) y **JaCoCo**.

---

## Funcionalidades principales

1. **Captura rápida y movimientos (`/movimientos`)**: registro ágil de gastos e ingresos (fecha de hoy por defecto y chips de categorías recientes), filtros por fecha/categoría/texto, edición, borrado y balance del periodo.
2. **Presupuestos y periodos (`/presupuestos`)**: vista por **mes completo** o **quincena** (1–15 y 16–fin de mes), presupuesto mensual por categoría con barra de avance (`Normal < 80 %`, `Aviso 80–99 %`, `Excedido >= 100 %`) y comparativo contra el mes anterior.
3. **Gráficas (`/graficas`)**: dona de gasto por categoría, barras de ingresos vs. gastos (6 o 12 meses) y línea de gasto acumulado diario contra el presupuesto mensual total (Chart.js `4.5.1`).
4. **Diario financiero (`/diario`)**: nota diaria con estado de ánimo (1–5) y etiquetas, línea de tiempo que une cada día con sus movimientos y su nota, resumen semanal (total gastado, categoría más alta, días sin gasto, notas) y búsqueda por texto o etiqueta.
5. **Tarjetas de crédito (`/tarjetas`)**: registro de tarjetas con día de corte y día de pago, gasto por ciclo de facturación, estimado a pagar en el próximo corte y avisos de pago cercano (5 días antes).
6. **Importación CSV bancaria (`/importacion`)**: formato configurable (columnas, separador y formato de fecha), vista previa antes de confirmar, detección de duplicados por hash SHA-256 con ocurrencia (permite dos compras reales iguales el mismo día y evita duplicar re-importaciones), `ReglaCategoria` automática y pantalla de revisión para movimientos en «Sin categoría».
7. **Recurrentes y Meses Sin Intereses (`/recurrentes`)**: generación automática con `@Scheduled` (respetando días 29, 30 y 31 en meses cortos y años bisiestos) y compras a MSI con reparto exacto de centavos para que las mensualidades sumen exactamente el total.
8. **Exportación y respaldos (`/respaldo`)**: exportación de movimientos a CSV (UTF-8 con BOM y protección contra fórmulas) y respaldo consistente en caliente de la base de datos H2 (`.zip` con fecha y hora).

---

## Requisitos

- **Java 21** (por ejemplo, Eclipse Temurin 21).
- No requiere instalar Maven globalmente: incluye **Maven Wrapper (`./mvnw`)**.

---

## Cómo ejecutar

En tu terminal dentro de la carpeta del proyecto:

```zsh
./mvnw spring-boot:run
```

Luego abre tu navegador en:

- **http://localhost:8080** (o **http://127.0.0.1:8080**)

Para detener la aplicación presiona `Ctrl + C`.

### Compilar y empaquetar JAR

```zsh
./mvnw clean package
```

---

## Pruebas unitarias y cobertura (`mvn test` y JaCoCo)

Este proyecto también está pensado como entorno de práctica para escribir y validar pruebas unitarias sobre el dominio y los servicios (`src/test/java`), comenzando con **Maven (`mvn test`)** y **JaCoCo** para medir cobertura de código.

### Ejecutar pruebas con Maven

Con Maven instalado globalmente:

```bash
mvn test
```

O usando el **Maven Wrapper** incluido en el repositorio:

- Linux / macOS:
  ```bash
  ./mvnw test
  ```
- Windows (PowerShell):
  ```powershell
  .\mvnw.cmd test
  ```

Para ejecutar una sola clase de prueba (por ejemplo `DineroTest`):

```bash
mvn test -Dtest=DineroTest
```

### Cobertura inicial con JaCoCo

El flujo inicial contempla usar **JaCoCo** junto con `mvn test` para generar reportes de cobertura de instrucciones y ramas en `target/site/jacoco/index.html`.

> **Nota:** Actualmente `pom.xml` incluye `junit-jupiter` (`5.11.4`), pero aún no tiene configurado `jacoco-maven-plugin`.

Para habilitar JaCoCo en `pom.xml` dentro de `<build><plugins>`, agrega:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Una vez configurado en `pom.xml`, ejecuta:

```bash
mvn test
```

Y abre el reporte HTML generado en:

```text
target/site/jacoco/index.html
```

---

## Configuración por variables de entorno

No hay secretos ni datos personales en el repositorio. Puedes personalizar las rutas y el puerto mediante variables de entorno:

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto HTTP local (la app siempre escucha solo en `127.0.0.1`). |
| `DIARIO_DB_PATH` | `./data/diario-financiero` | Ruta base del archivo de base de datos H2 (`.mv.db`). |
| `DIARIO_BACKUPS_DIR` | `./backups` | Carpeta local donde se guardan los respaldos `.zip` fechados. |
| `DIARIO_DB_USER` | `diario` | Usuario local de la base de datos H2. |
| `DIARIO_DB_PASSWORD` | *(vacío)* | Contraseña opcional para la base de datos H2 local. |

Ejemplo cambiando puerto y ruta de base de datos:

```zsh
SERVER_PORT=8081 DIARIO_DB_PATH=/ruta/segura/mi-diario ./mvnw spring-boot:run
```

> Las carpetas `data/` y `backups/`, así como archivos `*.mv.db` y `*.csv`, están excluidos en `.gitignore` para evitar subir datos reales a Git.

---

## Respaldos y restauración

1. Entra a **Respaldo** (`http://localhost:8080/respaldo`) y haz clic en **Crear respaldo ahora**.
2. Se generará un archivo `diario-financiero-YYYY-MM-DD_HH-mm-ss.zip` dentro de `./backups/`, que también puedes descargar desde el navegador para guardar una copia externa.
3. **Para restaurar**:
   - Detén la aplicación (`Ctrl + C`).
   - Descomprime el `.zip` y reemplaza el archivo `diario-financiero.mv.db` dentro de `./data/`.
   - Vuelve a arrancar con `./mvnw spring-boot:run`.

---

## Aviso importante

Esta aplicación es una herramienta personal de organización, registro y seguimiento de finanzas propias. **No constituye asesoría financiera, fiscal, legal ni contable.** Todas las decisiones económicas y tributarias son responsabilidad exclusiva del usuario.
