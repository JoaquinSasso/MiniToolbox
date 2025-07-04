# 📊 Pruebas de rendimiento - MiniToolbox

Se realizaron pruebas de carga y parseo de un dataset local de países (~250 elementos) para evaluar distintas técnicas de serialización. El objetivo fue optimizar tiempos de carga para una experiencia de usuario fluida.

## 🔬 Entorno de prueba
- Dispositivo: Google Pixel 8
- Build: Release
- Dataset: 250 países con campos de tipo `string`, `repeated string` y `int64`
- Dataset offline (almacenado en `assets/`)

## 📝 Formatos probados

| Formato            | Tamaño archivo | Lectura (ms) | Parseo (ms) | Total (ms) |
|--------------------|----------------|--------------|-------------|------------|
| JSON + Moshi        | 132 KB          | 5 ms         | 445 ms      | ~451 ms    |
| JSON + Gson         | 132 KB          | 5 ms         | 70 ms       | ~75 ms     |
| Protobuf Lite (bin) | 26 KB           | 1 ms         | 40 ms       | ~45 ms     |

## ⚡ Observaciones
- **JSON + Moshi**: El parseo resultó considerablemente más lento (445 ms). Esto puede deberse a cómo se mapearon los modelos o la configuración usada (Moshi suele ser más eficiente en modelos simples).
- **JSON + Gson**: Rendimiento aceptable (~70 ms de parseo), mejor que Moshi en este caso.
- **Protobuf Lite**: Redujo significativamente el tamaño del archivo y los tiempos de parseo (~40 ms). El binario es más compacto y eficiente para lectura.

## 💡 Decisión final
Se eligió **Protobuf Lite** como formato final porque:
- Achica el archivo de datos (de 132 KB a 26 KB).
- Mejora los tiempos de carga y parseo (45 ms en el dispositivo de prueba).
- Proporciona un formato binario portable y eficiente para un dataset estático.

## 📌 Notas
- La optimización se consideró suficiente dado que el parseo ocurre una sola vez al abrir la herramienta.
- Tecnologías como FlatBuffers o Cap’n Proto se descartaron por no justificar la complejidad adicional en este contexto.

---
