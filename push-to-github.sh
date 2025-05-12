#!/bin/bash

# Exit on error
set -e

echo "Preparing to push drone9 repository to GitHub..."

# Make sure .gitignore is applied
echo "Applying .gitignore rules..."
git add .
git commit -m "Update .gitignore and prepare for GitHub push" || echo "No changes to commit"

# Remove large directories from git tracking
echo "Removing node_modules from git tracking..."
git rm -r --cached vue-vben-admin/node_modules || echo "vue-vben-admin/node_modules not in git index"

# Configure git for large files
echo "Configuring git for larger files..."
git config http.postBuffer 524288000
git config http.lowSpeedLimit 1000
git config http.lowSpeedTime 180

# Push in smaller chunks
echo "Pushing to GitHub..."
git push origin main || echo "Push failed, try manually with: git push -u origin main --force"

echo "Done! If the push failed, you may need to push manually with: git push -u origin main --force" 