@echo off
REM JavaFX Application Launcher
REM --------------------------

REM 1. Configure JavaFX Path (uncomment and adjust)
REM set JAVA_HOME="C:\Program Files\Java\jdk-17.0.14+7"
REM set PATH_TO_FX="C:\Java\javafx-sdk-21.0.2\lib"

REM 2. Set combined module path (JDK + JavaFX)
set MODULE_PATH=%JAVA_HOME%\jmods;%PATH_TO_FX%

REM 3. Run the application
REM java --module-path "%MODULE_PATH%" ^
REM     --add-modules javafx.controls,javafx.fxml ^
REM     -jar "target\jNsi.jar"

java --module-path "%MODULE_PATH%" ^
      --add-modules javafx.controls,javafx.fxml ^
      -jar "target\jFerzl.jar"

REM 4. Error handling
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Failed to launch application (Error: %errorlevel%)
    pause
)