@echo off
if not exist "bin\com\campusnet\Main.class" (
    echo Binaries not found. Running compile.bat first...
    call compile.bat
)
echo Starting Multi-Area Campus Network Simulator GUI...
java -cp bin com.campusnet.Main %*
