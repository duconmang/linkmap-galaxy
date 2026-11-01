import {readdir,readFile,mkdir,writeFile,lstat} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

// Pages serves main/root; web/dist remains the single editable web source.
const root=path.resolve(fileURLToPath(new URL('../',import.meta.url)));
const source=path.join(root,'web','dist');
const check=process.argv.includes('--check');
if(process.argv.slice(2).some(a=>a!=='--check'))throw new Error('Usage: node scripts/sync-web-pages.mjs [--check]');
const allowed=new Set(['.html','.js','.css','.json','.webmanifest','.svg','.png','.jpg','.webp']);
async function files(dir,prefix=''){
  const out=[];
  for(const entry of await readdir(dir,{withFileTypes:true})){
    if(entry.name.startsWith('.'))throw new Error(`Hidden file is not a web artifact: ${prefix+entry.name}`);
    if(entry.isSymbolicLink())throw new Error(`Refusing symlink: ${prefix+entry.name}`);
    if(entry.isDirectory())out.push(...await files(path.join(dir,entry.name),prefix+entry.name+'/'));
    else if(entry.isFile()&&allowed.has(path.extname(entry.name)))out.push(prefix+entry.name);
    else throw new Error(`Unexpected web artifact: ${prefix+entry.name}`);
  }
  return out;
}
const mismatches=[];let count=0;
for(const relative of await files(source)){
  const dest=path.resolve(root,relative);
  if(!dest.startsWith(root+path.sep)||relative.split('/').some(s=>s==='..'))throw new Error('Artifact escaped repository');
  // Only the assets directory and top-level static files may be published.
  if(relative.includes('/')&&!relative.startsWith('assets/'))throw new Error(`Unexpected public directory: ${relative}`);
  for(let dir=path.dirname(dest);dir!==path.dirname(root)&&dir!==root;dir=path.dirname(dir)){
    const stat=await lstat(dir).catch(e=>{if(e.code==='ENOENT')return null;throw e;});
    if(stat?.isSymbolicLink())throw new Error(`Refusing destination symlink: ${relative}`);
  }
  const stat=await lstat(dest).catch(e=>{if(e.code==='ENOENT')return null;throw e;});
  if(stat?.isSymbolicLink())throw new Error(`Refusing destination symlink: ${relative}`);
  const content=await readFile(path.join(source,relative));
  if(check){const current=await readFile(dest).catch(e=>{if(e.code==='ENOENT')return null;throw e;});if(!current?.equals(content))mismatches.push(relative);}
  else {await mkdir(path.dirname(dest),{recursive:true});await writeFile(dest,content);}
  count++;
}
if(mismatches.length){console.error(`Pages artifacts differ: ${mismatches.join(', ')}`);process.exitCode=1;}
else console.log(`${check?'Verified':'Synced'} ${count} public web artifacts.`);
