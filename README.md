# CodeIDE Lab

Laboratorio aparte para responder una pregunta antes de tocar CodeIDE:
**puede esta tablet mostrar una app dentro de un panel nuestro?**

No contiene nada de CodeIDE. Es una app minima de pruebas.

## Instalar

1. Espera a que termine la accion **Compilar APK** (pestana Actions).
2. Abre la pestana **Releases**, descarga `app-debug.apk` e instalalo.
3. Cada compilacion se firma con una llave de prueba distinta: **desinstala la version anterior** antes de instalar la nueva.

## Pruebas

| Boton | Que mide |
| --- | --- |
| 1. Actividad propia en panel | Si Android deja crear una pantalla virtual y dibujar una actividad nuestra dentro del panel |
| 2. Ajustes en panel | Lo mismo con una app ajena (Ajustes). Es el resultado que mas importa |
| 3. Catalogo: abrir normal | Lista las apps instaladas y las abre a pantalla completa con Android |
| 4. Catalogo: probar en panel | Intenta abrir la app elegida dentro del panel |

## Como leer los resultados

- **Se ve dentro del panel**: la imagen se puede mostrar.
- **Aparece a pantalla completa**: Android rechazo ponerla en el panel.
- **Error en el registro**: copia el texto con el boton *Copiar resultados*.
- **Toque en la actividad propia**: si el contador no sube, ver la app no basta; falta resolver como llevarle los toques.

## Limites conocidos

- Es una prueba de viabilidad, no un producto.
- Solo se prueba con las APIs publicas de Android. No se usa root ni se evade ninguna proteccion.
- Los resultados dependen del fabricante y de la version de Android.
