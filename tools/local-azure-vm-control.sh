#!/usr/bin/env bash
set -euo pipefail

# Local helper to control the SoulMatch Azure VM for development billing.
# Use "stop" to deallocate the VM; deallocated VMs do not incur VM compute charges.

SUBSCRIPTION_ID="cf734a0e-2513-41f3-aaf6-6dd20a4d58a7"
RESOURCE_GROUP="soulmatch-rg"
VM_NAME="soulmatch-vm"

usage() {
  cat <<EOF
Usage:
  ./tools/local-azure-vm-control.sh start
  ./tools/local-azure-vm-control.sh stop
  ./tools/local-azure-vm-control.sh deallocate
  ./tools/local-azure-vm-control.sh restart
  ./tools/local-azure-vm-control.sh status

Notes:
  - "stop" and "deallocate" both run Azure deallocate to save compute cost.
  - You must be logged in to Azure CLI first: az login
EOF
}

require_az() {
  if ! command -v az >/dev/null 2>&1; then
    echo "Azure CLI is not installed or not available in PATH."
    exit 1
  fi

  if ! az account show >/dev/null 2>&1; then
    echo "Azure CLI is not logged in. Run: az login"
    exit 1
  fi

  az account set --subscription "$SUBSCRIPTION_ID"
}

vm_status() {
  az vm get-instance-view \
    --resource-group "$RESOURCE_GROUP" \
    --name "$VM_NAME" \
    --query "instanceView.statuses[?starts_with(code, 'PowerState/')].displayStatus | [0]" \
    --output tsv
}

action="${1:-}"

case "$action" in
  start)
    require_az
    echo "Starting $VM_NAME..."
    az vm start --resource-group "$RESOURCE_GROUP" --name "$VM_NAME" --output none
    echo "$VM_NAME status: $(vm_status)"
    ;;

  stop|deallocate)
    require_az
    echo "Deallocating $VM_NAME to stop VM compute billing..."
    az vm deallocate --resource-group "$RESOURCE_GROUP" --name "$VM_NAME" --output none
    echo "$VM_NAME status: $(vm_status)"
    ;;

  restart)
    require_az
    echo "Restarting $VM_NAME..."
    az vm restart --resource-group "$RESOURCE_GROUP" --name "$VM_NAME" --output none
    echo "$VM_NAME status: $(vm_status)"
    ;;

  status)
    require_az
    echo "$VM_NAME status: $(vm_status)"
    ;;

  *)
    usage
    exit 1
    ;;
esac
