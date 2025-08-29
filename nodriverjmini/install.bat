@echo off
setlocal enabledelayedexpansion

:: ==============================================
::        Parameters you can modify
:: ==============================================

set "GROUP_ID=com.vityazev_egor"
set "ARTIFACT_ID=nodriverjmini"
set "VERSION=1.2"

:: Specify the version suffix, as in the file name, for example, "-SNAPSHOT"
set "FILE_VERSION=%VERSION%-SNAPSHOT"

set "JAR_NAME=%ARTIFACT_ID%-%FILE_VERSION%.jar"
set "JAVADOC_JAR_NAME=%ARTIFACT_ID%-%FILE_VERSION%-javadoc.jar"

:: ==============================================
::         The script logic begins here
:: ==============================================

echo Building and packaging the project...
call mvn clean package javadoc:jar -DskipTests
if !errorlevel! neq 0 (
echo.
echo Error executing mvn clean package javadoc:jar
exit /b 1
)

echo.
echo Changing to the target directory...
cd target
if !errorlevel! neq 0 (
echo Failed to change to the target directory
exit /b 1
)

echo.
echo Searching for the main JAR file...
for /f "delims=" %%i in ('dir /s /b "!JAR_NAME!" /A:-D') do (
set "JAR_FILE=%%i"
goto :found_jar
)

:found_jar
if not defined JAR_FILE (
echo Main .jar file not found in the target directory
exit /b 1
)

echo Main JAR found: !JAR_FILE!
echo Installing the main JAR file to the local repository...
call mvn install:install-file -Dfile="!JAR_FILE!" -DgroupId=!GROUP_ID! -DartifactId=!ARTIFACT_ID! -Dversion=!VERSION! -Dpackaging=jar -Dname=!ARTIFACT_ID!
if !errorlevel! neq 0 (
echo.
echo Error installing the main .jar file
exit /b 1
)

echo.
echo Searching for the Javadoc JAR file...
for /f "delims=" %%i in ('dir /s /b "!JAVADOC_JAR_NAME!" /A:-D') do (
set "JAVADOC_JAR_FILE=%%i"
goto :found_javadoc
)

:found_javadoc
if not defined JAVADOC_JAR_FILE (
echo Javadoc file not found in the target directory
echo.
echo Main JAR file installation was successful, but the Javadoc file was not found.
exit /b 0
)

echo Javadoc JAR found: !JAVADOC_JAR_FILE!
echo Installing the Javadoc JAR file to the local repository...
call mvn install:install-file -Dfile="!JAVADOC_JAR_FILE!" -DgroupId=!GROUP_ID! -DartifactId=!ARTIFACT_ID! -Dversion=!VERSION! -Dpackaging=jar -Dclassifier=javadoc
if !errorlevel! neq 0 (
echo.
echo Error installing the Javadoc file
exit /b 1
)

echo.
echo Successfully installed !JAR_NAME! and !JAVADOC_JAR_FILE! to the local repository.

echo.
echo ==============================================
echo To use this library, add the following dependency to your pom.xml:
echo ==============================================
echo.
echo ^<dependency^>
echo   ^<groupId^>%GROUP_ID%^</groupId^>
echo   ^<artifactId^>%ARTIFACT_ID%^</artifactId^>
echo   ^<version^>%VERSION%^</version^>
echo ^</dependency^>

endlocal