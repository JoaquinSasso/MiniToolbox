@echo off
setlocal enabledelayedexpansion

echo === CONVERSIÓN SELECTIVA DE PNG A SVG + OPTIMIZACIÓN SOLO SI ES NUEVO ===

REM Iterar todos los PNG en la carpeta actual
for %%F in (*.png) do (
    set "nombre=%%~nF"
    set "svg=%%~nF.svg"

    REM Verificar si ya existe el archivo SVG
    if not exist "!svg!" (
        echo.
        echo 🛠️ Procesando: %%F

        REM Paso 1: Convertir PNG a PBM (blanco y negro puro sin transparencia)
        magick "%%F" ^
            -background white -alpha remove -flatten ^
            -colorspace Gray -threshold 50%% ^
            "!nombre!.pbm"

        REM Verificar si el PBM fue creado correctamente
        if exist "!nombre!.pbm" (
            REM Paso 2: Vectorizar con Potrace
            potrace "!nombre!.pbm" -s -o "!nombre!.svg"

            REM Paso 3: Optimizar el SVG recién generado
            svgo "!nombre!.svg"

            REM Paso 4: Eliminar PBM
            del "%%~nF.pbm"


            echo ✅ SVG generado y optimizado: !nombre!.svg
        ) else (
            echo ❌ Error al generar PBM para %%F
        )
    ) else (
        echo ⏩ Ya existe: !svg! — se salta conversión y optimización
    )
)

echo.
echo ✅ Proceso completo: solo se convirtieron y optimizaron archivos nuevos.
pause
