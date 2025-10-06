@ECHO OFF
where mvn >NUL 2>&1
IF %ERRORLEVEL% EQU 0 (
  mvn %*
) ELSE (
  ECHO Maven not found. Please install Maven or run 'mvn' directly.
  EXIT /B 1
)
