#!/bin/bash

# ১. অটোমেটিক gitignore চেক ও নিশ্চিত করা
if [ ! -f .gitignore ]; then
cat << 'IGN' > .gitignore
*.iml
.gradle
/local.properties
/.idea
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
IGN
fi

# ২. অপ্রয়োজনীয় ক্যাশ ফাইল রিমুভ করা
git rm -r --cached .gradle build 2>/dev/null || true

# ৩. ফাইল অ্যাড ও কমিট করা
git add .
MSG="${1:-Auto update and clean build}"
git commit -m "$MSG"

# ৪. গিটহাবে পুশ করা
echo "🚀 Pushing to GitHub..."
git push

# ৫. লাইভ বিল্ড ট্র্যাকিং চালু করা
echo "🔄 Monitoring Live Build..."
sleep 3
gh run view --watch
