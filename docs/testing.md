# Convenciones de Testing en MiniToolbox

Este documento establece las guías y convenciones para la suite de pruebas del proyecto MiniToolbox.

## Principios Generales

1. **Pureza de la Lógica de Dominio**: La lógica de negocio (como el motor del Buscaminas) debe estar aislada de dependencias de Android (Context, Resources, etc.). Esto permite tests unitarios rápidos y determinísticos con JUnit 4 puro.
2. **Determinismo**: Todos los tests deben ser determinísticos. Se deben usar semillas fijas (`Long`) para cualquier lógica aleatoria. No usar `Random()` sin semilla.
3. **Tests Descriptivos**: Los nombres de los tests deben explicar la intención del negocio usando backticks para permitir espacios y caracteres especiales.
4. **Un Assert Conceptual por Test**: Cada test debe verificar una única propiedad o comportamiento.

## Estructura de Archivos

- `app/src/test/java/...`: Tests unitarios puros.
- `MinesTestFixtures.kt`: Helpers de construcción de objetos y utilidades compartidas para tests de un módulo específico.

## Cómo Correr los Tests

Para ejecutar la suite completa de tests unitarios desde la terminal:

```bash
./gradlew :app:testDebugUnitTest
```

Los reportes se generan en: `app/build/reports/tests/testDebugUnitTest/index.html`

## Convenciones de Nomenclatura

- Usar backticks: `` `el primer toque nunca es mina` ``.
- Estructura sugerida: `Sujeto de prueba > Escenario > Resultado esperado`.

## Política de Lint Baseline

El archivo `app/lint-baseline.xml` se utiliza exclusivamente para gestionar deuda técnica heredada que se planea resolver a futuro.

1. **No al Silencio Fácil**: No se deben agregar nuevos issues al baseline para silenciar warnings introducidos por cambios recientes. Si un cambio introduce un warning de Lint, debe corregirse en el mismo PR.
2. **Justificación**: Cualquier adición al baseline debe justificarse explícitamente en la descripción del Pull Request.
3. **Deuda Conocida**: Actualmente, el baseline contiene un `StaticFieldLeak` en `ProSilentInitializer.kt` relacionado con el subsistema de billing. Este se mantendrá hasta que se implemente inyección de dependencias (Hilt), momento en el cual se espera eliminar todas las referencias estáticas a `Context`.
4. **Test Seams**: Las costuras de test (como `metricsRepoFactory` en `Metrics.kt`) son ejemplos concretos de la necesidad de DI. Deben estar anotadas con `@VisibleForTesting` y ser eliminadas tras la migración a Hilt.

---

## Roadmap de Testing (Prioridades)

1. **Lógica de División de Gastos**:
   - Archivo: `tools/organizacion/divisorGastos/ReunionDetailScreen.kt` (o el ViewModel asociado).
   - **Problema**: Actualmente la lógica de liquidación de deudas está acoplada a la UI o recibe `Context`.
   - **Tarea**: Extraer un `DebtEngine` puro que reciba una lista de gastos y devuelva una lista de transacciones sugeridas. Testear exhaustivamente casos borde (saldos cero, divisiones con decimales, etc.).
2. **Validadores de Formatos**:
   - Implementar tests unitarios para cualquier lógica de validación de entradas de usuario (números, fechas).

## Notas Técnicas

- **Velocidad**: La suite completa debe correr en < 10 segundos.
- **Determinismo Iterativo**: Para propiedades que deben cumplirse siempre (ej. "el primer toque es seguro"), iterar sobre un rango de semillas fijas (ej. `0L..199L`) en un solo test para aumentar la cobertura sin perder determinismo.
