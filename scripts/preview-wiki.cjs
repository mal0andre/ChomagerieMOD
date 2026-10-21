#!/usr/bin/env node
// Generate an offline preview of the English player wiki. Requires the marked package.
const fs = require('node:fs');
const path = require('node:path');

async function main() {
  const { marked } = await import(require.resolve('marked'));
  const root = path.resolve(__dirname, '..');
  const wiki = path.join(root, 'wiki');
  const output = path.join(root, 'build/wiki/preview');
  const media = 'https://raw.githubusercontent.com/wiki/mal0andre/ChomagerieMOD/';
  const pages = fs.readdirSync(wiki).filter(name => name.endsWith('.md') && !name.startsWith('_'));
  const slugs = new Set(pages.map(name => name.slice(0, -3)));
  function render(name) {
    return marked.parse(fs.readFileSync(path.join(wiki, name), 'utf8'))
      .replaceAll(media, '')
      .replace(/href="([^"#]+)"/g, (match, slug) => slugs.has(slug) ? `href="#${slug}"` : match)
      .replaceAll('<img ', '<img loading="lazy" ');
  }
  const articles = pages.map(name => `<article id="${name.slice(0, -3)}" hidden>${render(name)}</article>`).join('\n');
  fs.mkdirSync(output, { recursive: true });
  fs.cpSync(path.join(wiki, 'images'), path.join(output, 'images'), { recursive: true });
  fs.writeFileSync(path.join(output, 'index.html'), `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Chomagerie · Player Wiki</title>
<style>
*{box-sizing:border-box}body{margin:0;color:#222b26;background:#f3f4ef;font:16px/1.65 system-ui,sans-serif}
header{padding:20px 32px;border-bottom:1px solid #d9ddd2;background:#fff}header a{font-size:24px;font-weight:750;color:inherit;text-decoration:none}header p{margin:3px 0 0;color:#64715f}
.layout{display:grid;grid-template-columns:240px minmax(0,960px);max-width:1280px;margin:auto;align-items:start}
nav{padding:24px;position:sticky;top:0;max-height:100vh;overflow:auto;font-size:14px}nav h2{font-size:18px}nav h3{font-size:12px;text-transform:uppercase;letter-spacing:.08em;color:#65745e;margin-top:24px}nav ul{padding-left:16px}nav a{text-decoration:none}nav a[aria-current=page]{color:#193d16;font-weight:750}
main{padding:28px 40px 64px;background:#fff;min-height:85vh;min-width:0}a{color:#386c2c}h1{font-size:34px;line-height:1.2;margin:4px 0 24px}h2{font-size:23px;margin-top:36px}h3{margin-top:32px}p{max-width:76ch}img{display:block;max-width:100%;height:auto;margin:22px 0}img[src*="/recipes/"],img[src$="locking-tools.png"]{width:528px;image-rendering:pixelated}
table{border-collapse:collapse;max-width:100%;display:block;overflow-x:auto}td,th{padding:9px 13px;border:1px solid #dfe3da;text-align:left}th{background:#f3f6ed}code{padding:2px 4px;background:#f0f2eb;border-radius:3px}pre{background:#f0f2eb;padding:18px;overflow:auto}details{border:1px solid #dfe3da;padding:12px 18px;margin:12px 0}summary{cursor:pointer}footer{margin-top:48px;border-top:1px solid #dfe3da;padding-top:16px;font-size:14px;color:#64715f}
@media(max-width:760px){.layout{display:block}nav{position:static;max-height:260px;border-bottom:1px solid #dfe3da}main{padding:24px 18px}header{padding:16px 18px}}
</style></head><body><header><a href="#Home">Chomagerie 🧀</a><p>Player Wiki · Minecraft 26.3 · Chomagerie 1.6.1</p></header>
<div class="layout"><nav aria-label="Wiki pages">${render('_Sidebar.md')}</nav><main>${articles}<footer>${render('_Footer.md')}</footer></main></div>
<script>
function showPage(){
  const slug=decodeURIComponent(location.hash.slice(1))||'Home';
  const page=Array.from(document.querySelectorAll('article')).find(article=>article.id===slug)||document.getElementById('Home');
  document.querySelectorAll('article').forEach(article=>{article.hidden=article!==page});
  document.querySelectorAll('nav a').forEach(link=>{if(link.hash==='#'+page.id)link.setAttribute('aria-current','page');else link.removeAttribute('aria-current')});
  document.title=page.querySelector('h1').textContent+' · Chomagerie';
  window.scrollTo(0,0);
}
addEventListener('hashchange',showPage);showPage();
</script></body></html>`);
  console.log(`Offline preview: ${path.join(output, 'index.html')} (${pages.length} pages)`);
}
main().catch(error => { console.error(error); process.exitCode = 1; });
