#!/bin/bash
# Find local IP address for network configuration

# Try different methods to find local IP
if command -v hostname &> /dev/null; then
    IP=$(hostname -I | awk '{print $1}')
    if [[ $IP =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo $IP
        exit 0
    fi
fi

# Alternative method using ip command
if command -v ip &> /dev/null; then
    IP=$(ip route get 1 2>/dev/null | awk '{print $7; exit}')
    if [[ $IP =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo $IP
        exit 0
    fi
fi

# Fallback to ifconfig
if command -v ifconfig &> /dev/null; then
    IP=$(ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1' | head -n1)
    if [[ $IP =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo $IP
        exit 0
    fi
fi

echo "Could not determine local IP address. Please find it manually in your network settings."
exit 1