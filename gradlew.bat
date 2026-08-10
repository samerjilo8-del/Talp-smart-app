@rem
@rem Smart Teacher - Gradle launcher for Windows
@rem
@rem Note: the gradle-wrapper.jar binary is not committed to this repository.
@rem To generate it run once with a local Gradle 8.2 install:
@rem   gradle wrapper --gradle-version 8.2
@rem Or open the project in Android Studio, which generates it automatically.
@rem

@echo off
set DIR=%~dp0

where gradle >nul 2>&1
if %errorlevel%==0 (
    gradle %*
) else (
    echo Gradle wrapper jar not found and 'gradle' is not on the PATH.
    echo Install Gradle 8.2 (https://gradle.org/install/) and run:
    echo   gradle wrapper --gradle-version 8.2
    echo or open this project in Android Studio.
    exit /b 1
)
