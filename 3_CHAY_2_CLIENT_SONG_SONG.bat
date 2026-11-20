@echo off
chcp 65001 > nul
title CHAY 2 CLIENT THU VIEN SONG SONG
cd /d "%~dp0"

echo ======================================================================
echo   DANG KHOI DONG DONG THOI 2 CLIENT DE TEST SONG SONG VA DONG BO UDP
echo ======================================================================
echo.
echo 1. Dang mo Client thu nhat (Client 1)...
start "" javaw --enable-native-access=ALL-UNNAMED --module-path "lib" --add-modules javafx.controls,javafx.graphics,javafx.base -Dfile.encoding=UTF-8 -cp "build/classes;lib/*" ConnectFrame

timeout /t 1 /nobreak > nul

echo 2. Dang mo Client thu hai (Client 2)...
start "" javaw --enable-native-access=ALL-UNNAMED --module-path "lib" --add-modules javafx.controls,javafx.graphics,javafx.base -Dfile.encoding=UTF-8 -cp "build/classes;lib/*" ConnectFrame

echo.
echo ======================================================================
echo   DA KHOI DONG THANH CONG 2 CUA SO CLIENT!
echo   Huong dan test:
echo   - Cua so Client 1: Chon tai khoan 'sinhvien' (mk: 123456) -> Bam KET NOI
echo   - Cua so Client 2: Chon tai khoan 'sinhvien1' (mk: 123456) -> Bam KET NOI
echo   - Khi Client 1 Them/Muon sach -> Client 2 se tu dong cap nhat qua UDP!
echo ======================================================================
timeout /t 3 /nobreak > nul
