@echo off
setlocal
cd /d "%~dp0"
echo Eliminando restos retirados de OneHouse v1.13.3 REV1...
for %%F in (
"app\src\main\java\com\onehouse\app\core\storage\RetiredFeatureCleanup.kt"
"app\src\main\java\com\onehouse\app\importer\InsideControlParser.kt"
"app\src\main\java\com\onehouse\app\importer\InsideControlImporter.kt"
"app\src\main\java\com\onehouse\app\importer\InsideControlProjectRepository.kt"
"app\src\main\java\com\onehouse\app\importer\ImportedKnxObject.kt"
) do (
  if exist %%F del /q %%F
)
if exist "app\src\main\java\com\onehouse\app\importer" rmdir "app\src\main\java\com\onehouse\app\importer" 2>nul
echo Limpieza REV1 aplicada.
endlocal
