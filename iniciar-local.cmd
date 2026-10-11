@echo off
setlocal
cd /d "%~dp0"
where py >nul 2>&1
if errorlevel 1 (
    where python >nul 2>&1
    if errorlevel 1 (
        echo Instala Python 3.10 o posterior y habilita el lanzador py.
        exit /b 1
    )
    python scripts\iniciar_local.py %*
) else (
    py -3 scripts\iniciar_local.py %*
)
exit /b %errorlevel%
