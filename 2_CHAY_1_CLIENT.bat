@echo off
chcp 65001 > nul
title CLIENT THU VIEN
cd /d "%~dp0"

echo ======================================================================
echo   KHOI DONG 1 CLIENT GIAO DIEN THU VIEN (JAVAFX)
echo ======================================================================

start "" javaw --enable-native-access=ALL-UNNAMED --module-path "lib" --add-modules javafx.controls,javafx.graphics,javafx.base -Dfile.encoding=UTF-8 -cp "build/classes;lib/*" ConnectFrame
