:: Z80 Hacker execution script for windows
:: Version 0.2
:: Author: Luciano M. Christofoletti
:: luciano@christofoletti.com.br
:: 26/06/2017

@echo off
setlocal

:: set java options and jar name
@set JAVA_OPTS=-Xms256m -Xss256k
@set JAR_NAME=z80.hacker-0.1.jar

:: get current drive and directory
@set DRIVE=%~d0
@set CURRENT_DIR=%~dp0

@"%JAVA_HOME%\bin\java" %JAVA_OPTS% -jar "%CURRENT_DIR%\%JAR_NAME%" %1 %2 %3 %4 %5 %6
@if errorlevel 2 goto noJRE
@goto end

:noJRE
@echo.
@echo Failed to run java!
@echo Java runtime environment is required to run z80hacker.
@echo Edit the .bat file and set the JAVA_HOME variable.
@echo.
:end
