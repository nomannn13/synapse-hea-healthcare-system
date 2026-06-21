const fs = require('fs');
const path = require('path');
const vm = require('vm');
const { spawnSync } = require('child_process');

const projectRoot = path.resolve(__dirname, '../..');
const targets = [
  path.join(projectRoot, 'backend'),
  path.join(projectRoot, 'frontend', 'js')
];

function walk(directory, extension) {
  const result = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    if (entry.name === 'node_modules') continue;
    const fullPath = path.join(directory, entry.name);
    if (entry.isDirectory()) result.push(...walk(fullPath, extension));
    else if (entry.name.endsWith(extension)) result.push(fullPath);
  }
  return result;
}

let failed = false;
for (const file of targets.flatMap((directory) => walk(directory, '.js'))) {
  const result = spawnSync(process.execPath, ['--check', file], { encoding: 'utf8' });
  if (result.status !== 0) {
    failed = true;
    console.error(`Syntax error: ${path.relative(projectRoot, file)}`);
    console.error(result.stderr);
  }
}

const htmlDirectory = path.join(projectRoot, 'frontend', 'pages');
for (const file of walk(htmlDirectory, '.html')) {
  const html = fs.readFileSync(file, 'utf8');
  const scripts = [...html.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/gi)];
  scripts.forEach((match, index) => {
    try {
      new vm.Script(match[1], { filename: `${file}:inline-script-${index + 1}` });
    } catch (error) {
      failed = true;
      console.error(error.message);
    }
  });
}

if (failed) process.exit(1);
console.log('All JavaScript and inline page scripts passed syntax checks.');
