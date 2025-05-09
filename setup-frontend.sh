#!/bin/bash

# Create directory for environment variables
mkdir -p vue-vben-admin/apps/web-antd

# Set up environment configuration
cat > vue-vben-admin/apps/web-antd/.env <<EOF
# Basic configuration
VITE_APP_TITLE=无人机管理系统
VITE_APP_VERSION=1.0.0
VITE_APP_NAMESPACE=drone-admin

# API configuration
VITE_GLOB_API_URL=http://localhost:8080/api

# Disable mock service
VITE_USE_MOCK=false
EOF

echo "Frontend environment configured successfully!"
echo "You can now run the frontend with: cd vue-vben-admin && pnpm --filter @apps/web-antd dev"
echo "The frontend will connect to the Spring Boot backend running on http://localhost:8080/api" 