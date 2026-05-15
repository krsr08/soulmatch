@echo off
setlocal

set SUBSCRIPTION_ID=cf734a0e-2513-41f3-aaf6-6dd20a4d58a7
set RESOURCE_GROUP=soulmatch-rg
set VM_NAME=soulmatch-vm

echo Checking %VM_NAME% status...
where az >nul 2>&1
if errorlevel 1 (
  echo Azure CLI is not installed or not available in PATH.
  pause
  exit /b 1
)

call az account show >nul 2>&1
if errorlevel 1 (
  echo Azure CLI is not logged in. Run: az login
  pause
  exit /b 1
)

call az account set --subscription "%SUBSCRIPTION_ID%"
call az vm get-instance-view --resource-group "%RESOURCE_GROUP%" --name "%VM_NAME%" --query "instanceView.statuses[?starts_with(code, 'PowerState/')].displayStatus | [0]" --output tsv

pause
