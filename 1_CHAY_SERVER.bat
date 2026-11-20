@echo off
chcp 65001 > nul
title SERVER THU VIEN - TCP 9999 ^& UDP 9998
cd /d "%~dp0"

echo ======================================================================
echo   HE THONG QUAN LY THU VIEN - MAY CHU SERVER (TCP 9999)
echo   Sinh vien: Tran Van Thinh - MSV: 2355010193
echo ======================================================================
echo.
echo Dang khoi dong Server va nap CSDL SQLite...
echo (De ngat Server, hay bam Ctrl + C hoac dong cua so nay)
echo.

java -Dfile.encoding=UTF-8 -cp "build/classes;lib/*" LibraryServer

pause
