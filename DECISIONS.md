# DECISIONS.md

Este documento registra las decisiones arquitectónicas fundamentales de **MiniToolbox**. Los agentes autónomos operando en Antigravity deben respetar estas reglas sin excepción y no deben proponer refactorizaciones que las violen.

## 1. Arquitectura Base y Modularidad
* **Módulo Único:** La aplicación se mantiene deliberadamente como un módulo único en Android. No proponer modularización prematura.
* **Stack de UI:** Interfaz construida exclusivamente de forma nativa con Kotlin y Jetpack Compose.

## 2. Sistema de Métricas (Pipeline de Datos)
El sistema de telemetría es propietario y su flujo es innegociable:
* **Almacenamiento Local:** Los agregados diarios se acumulan en el cliente sobre **DataStore**.
* **Orquestación:** La sincronización de envíos se maneja mediante **WorkManager**.
* **Backend:** Se utilizan **Firebase Cloud Functions** (estrictamente en TypeScript) para recibir y procesar los lotes, almacenando el resultado final en **Firestore**.
* **Visualización:** El dashboard de consumo se mantiene estático en **Firebase Hosting**.

## 3. Privacidad por Diseño (Restricción Crítica)
* **Cero Identificadores de Dispositivo:** Solo se envían y almacenan métricas agregadas.
* **Regla de Rechazo Automático:** Si una nueva métrica propuesta requiere seguir a un individuo, queda descartada. El cálculo debe hacerse localmente en el dispositivo y lo único que viaja a la nube es la categoría agregada.

## 4. Continuidad de los Datos Históricos
* **Inmutabilidad del Contrato:** Los identificadores de métricas son un contrato estricto. Renombrar una clave parte la serie histórica en dos.
* Cualquier cambio que rompa la continuidad de los datos requiere aprobación manual explícita antes de implementarse.

## 5. Documentación Honesta y Transparencia
* En el directorio `docs/` (glosarios de métricas, postmortems, ADRs) se debe registrar toda limitación conocida o sesgo de los datos, incluso si el resultado es incómodo.

## 6. Documentos Relacionados
* **Reglas de conducta del agente:** `.agentrules.md`
* **Reglas de proyecto y flujo de trabajo:** `.agents/AGENTS.md`
* **Deuda técnica (formato máquina):** `SCRATCHPAD.md`
* **Decisiones de producto pendientes:** `PRODUCT_BACKLOG.md`
* **Skill del pipeline de métricas:** `.agents/skills/metrics-pipeline/SKILL.md`