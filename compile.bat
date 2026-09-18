@echo off
echo =======================================================
echo  Compiling Multi-Area Campus Network Simulator in Java
echo =======================================================
if not exist "bin" mkdir bin
javac -encoding UTF-8 -d bin src\com\campusnet\model\*.java src\com\campusnet\protocol\*.java src\com\campusnet\simulator\*.java src\com\campusnet\cli\*.java src\com\campusnet\ui\*.java src\com\campusnet\test\*.java src\com\campusnet\Main.java
if %errorlevel% equ 0 (
    echo [SUCCESS] Compilation finished successfully!
    echo Run the simulator with: run.bat
) else (
    echo [ERROR] Compilation failed with error code %errorlevel%
)
