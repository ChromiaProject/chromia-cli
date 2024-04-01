@echo off

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and CHR_OPTS to pass JVM options to this script.
set LOG_FOLDER="%TMP%\chromia"
set DEFAULT_JVM_OPTS=-Duser.language=en -Duser.country=US -DCHR_LOG_FOLDER=%LOG_FOLDER%

@rem Find java.exe from JAVA_HOME
if defined JAVA_HOME goto findJavaFromJavaHome
@rem Find java.exe from RELL_JAVA
if defined RELL_JAVA goto findJavaFromRellJava
set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo.
echo ERROR: JAVA_HOME or RELL_JAVA is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME or RELL_JAVA variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromRellJava
set JAVA_HOME=%RELL_JAVA:"=%
set JAVA_EXE=%RELL_JAVA%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: RELL_JAVA is set to an invalid directory: %RELL_JAVA%
echo.
echo Please set the RELL_JAVA variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\*


@rem Execute chr
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %CHR_OPTS%  -classpath "%CLASSPATH%" com.chromia.MainKt %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable CHR_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%CHR_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
