Guía de Contribución a MiniToolbox 🛠️
¡Primero que nada, gracias por interesarte en mejorar MiniToolbox! Este proyecto nació de la curiosidad y el deseo de crear herramientas eficientes, y cualquier aporte que ayude a ese objetivo es más que bienvenido.

No quiero que las reglas sean un obstáculo, así que mantendremos el proceso simple y directo.

🚀 ¿Cómo puedo aportar?
Si tienes una idea para una nueva herramienta, encontraste un error o quieres optimizar algo de código, solo sigue estos pasos:

Haz un Fork del repositorio.

Crea una rama para tu cambio (ej: feature/nueva-calculadora o fix/error-sensores).

Realiza tus cambios manteniendo la legibilidad del código.

Envía un Pull Request.

📝 Requisitos mínimos
Para que podamos integrar tu aporte rápidamente, solo te pido dos cosas:

Mensaje de commit claro: No hace falta que sea una tesis, pero sí que explique qué hiciste (ej: "Agregada lógica para el conversor de divisas" es mejor que solo "update").

Actualización de Arquitectura: Si tu cambio introduce un nuevo subsistema, un nuevo módulo, o cambia drásticamente la estructura de carpetas o el pipeline de métricas, **debés actualizar [docs/architecture.md](docs/architecture.md)** en el mismo Pull Request. Los diagramas Mermaid deben reflejar el estado final del sistema.

Breve explicación: En el Pull Request, cuéntame en un par de líneas qué cambiaste y por qué crees que es una mejora para la app.

💡 Áreas donde puedes ayudar
Si no sabes por dónde empezar, aquí hay algunas ideas:

Nuevas Utilidades: ¿Falta alguna herramienta que uses a diario? ¡Agrégala!

Optimización: Si encuentras una forma de hacer que el parseo de datos sea aún más rápido que nuestra implementación actual con ProtoBuf, ¡me encantaría verla!

UI/UX: Mejoras en los componentes de Jetpack Compose para hacer la interfaz más fluida.

🧼 Higiene y Privacidad
Para mantener el repositorio profesional y seguro, seguimos estas reglas:
1. **Archivos de Trabajo:** Ningún archivo de trabajo de herramientas externas (planes de agente, scratchpads, exports de IDE) entra al repositorio. Si una herramienta genera artefactos, su directorio debe ir al `.gitignore` antes de la primera ejecución.
2. **Rutas Relativas:** Ninguna ruta absoluta de una máquina de desarrollo entra a la documentación. Todos los links a archivos del proyecto deben ser relativos al repositorio. Una ruta `file:///C:/Ruta/Absoluta/...` revela el entorno de quien la escribió y no funciona para nadie más.

📬 Contacto
Si tienes dudas técnicas antes de empezar a programar, no dudes en abrir un Issue o contactarme a través de mi perfil de GitHub.
