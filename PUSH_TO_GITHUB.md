# Push this project to GitHub

Recommended repository name:

```text
synapse-hea-healthcare-system
```

## Option A — from the source ZIP

After creating an empty GitHub repository under `nomannn13`:

```bash
unzip Synapse-HEA-Portfolio.zip
cd Synapse-HEA-Portfolio

git init -b main
git config user.name "Nomaan Munshi"
git config user.email "nomaanmunshii@gmail.com"
git add .
git commit -m "Build portfolio-ready Synapse HEA revision"

git remote add origin https://github.com/nomannn13/synapse-hea-healthcare-system.git
git push -u origin main
```

## Option B — preserve the prepared commit

```bash
git clone Synapse-HEA-Portfolio.bundle synapse-hea-healthcare-system
cd synapse-hea-healthcare-system

git remote remove origin
git remote add origin https://github.com/nomannn13/synapse-hea-healthcare-system.git
git push -u origin main
```

After pushing, add these repository topics:

```text
nodejs express mysql healthcare-management jwt authentication docker software-engineering
```

Pin the repository on your GitHub profile only after:

1. Docker or native MySQL startup succeeds on your own computer.
2. You capture two or three screenshots.
3. You understand the files listed in the README and audit notes.
4. You keep the project-origin attribution.
