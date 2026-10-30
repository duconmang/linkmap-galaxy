export const PROTOCOL = 'linkmap.journal/2';
export const isNote=r=>r?.kind==='NOTE';
export function detectContent(input) {
  const text=String(input).trim();
  if(!text||text.length>64000)throw new Error('Nhập nội dung từ 1 đến 64.000 ký tự');
  const direct=normalize(text);
  if(direct)return {text,urls:[direct],kind:'LINK'};
  const found=[];const ranges=[];
  for(const m of text.matchAll(/https?:\/\/[^\s<>"']+/gi)) {
    ranges.push([m.index,m.index+m[0].length]);
    const url=normalize(m[0].replace(/[.,;!?)\]}]+$/g,''));if(url)found.push(url);
  }
  for(const m of text.matchAll(/\b(?:[a-z0-9-]+\.)+(?:com|org|net|io|dev|app|vn|edu|co|ai|me)(?:\/[^\s<>"']*)?/gi)) {
    if(ranges.some(([a,b])=>m.index>=a&&m.index<b)||text[m.index-1]==='@')continue;
    const url=normalize(m[0].replace(/[.,;!?)\]}]+$/g,''));if(url)found.push(url);
  }
  const urls=[...new Set(found)];return {text,urls,kind:urls.length?'LINK':'NOTE'};
}
export const providers = [
  ['FACEBOOK','Facebook',['facebook.com','fb.com','fb.watch'],'facebook'],
  ['TIKTOK','TikTok',['tiktok.com'],'tiktok'], ['GITHUB','GitHub',['github.com','github.io'],'github'],
  ['DRIVE','Google Drive',['drive.google.com','docs.google.com'],'googledrive'],
  ['ONEDRIVE','OneDrive',['onedrive.live.com','1drv.ms','sharepoint.com'],'cloud'],
  ['GOOGLE','Google',['google.com'],'google'], ['OTHER','Khác',[],'link']
];
export function normalize(input) {
  const value=String(input).trim();
  if (!value || value.length>8192 || /\s/.test(value)) return null;
  const candidate=value.includes('://')?value:`https://${value}`;
  try {
    const u=new URL(candidate);
    if(!['https:','http:'].includes(u.protocol)||u.username||u.password||!u.hostname||(!u.hostname.includes('.')&&!u.hostname.startsWith('[')&&u.hostname!=='localhost')) return null;
    // Keep raw path, escaping, query and fragment exactly like Android.
    return u.protocol.toLowerCase()+candidate.substring(candidate.indexOf(':')+1);
  } catch {return null;}
}
export function provider(url) {
  if(!url)return ['NOTE','Ghi chú',[],'note'];
  const host=new URL(url).hostname.toLowerCase();
  return providers.find(p=>p[2].some(d=>host===d||host.endsWith(`.${d}`)))??providers.at(-1);
}
export function defaultTitle(url) {
  const u=new URL(url), host=u.hostname.replace(/^www\./,''), path=u.pathname.split('/').filter(Boolean).at(-1);
  return path?`${host} · ${decodeURIComponent(path).slice(0,64)}`:host;
}
export async function recordId(url) {
  const bytes=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(url));
  return 'url-'+Array.from(new Uint8Array(bytes),v=>v.toString(16).padStart(2,'0')).join('');
}
export function compareRevision(a,b) { return a.counter-b.counter || (a.device===b.device?0:a.device>b.device?1:-1); }
export function merge(a,b) {
  if(!a)return structuredClone(b);
  if(a.id!==b.id)throw new Error('Record ID mismatch');
  const winner=compareRevision(a,b)>=0?a:b;
  if(isNote(winner))return {...winner,healthDevice:''};
  const candidates=[a,b].filter(v=>!isNote(v)&&v.url===winner.url);
  const health=candidates.sort((x,y)=>x.checkedAt-y.checkedAt || (x.healthDevice===y.healthDevice?0:x.healthDevice>y.healthDevice?1:-1) || compareRevision(x,y)).at(-1);
  return {...winner,health:health.health,reason:health.reason,checkedAt:health.checkedAt,httpCode:health.httpCode,notFoundAt:health.notFoundAt,healthDevice:health.healthDevice};
}
export function validateRecord(r) {
  if(!r||typeof r!=='object'||!['LINK','NOTE'].includes(r.kind??'LINK')||(isNote(r)?!/^note-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/.test(r.id)||r.url!==''||!r.note?.trim():!/^url-[a-f0-9]{64}$/.test(r.id)||normalize(r.url)!==r.url)||!Number.isSafeInteger(r.counter)||r.counter<0||r.counter>9007199254740000||typeof r.device!=='string'||!r.device||r.device.length>128||typeof r.deleted!=='boolean')throw new Error('Nhật ký đồng bộ không hợp lệ');
  for(const [key,max] of [['title',200],['note',64000],['category',50],['reason',500],['healthDevice',128]]) if(typeof r[key]!=='string'||r[key].length>max)throw new Error('Dữ liệu đồng bộ vượt giới hạn');
  if(!['UNCHECKED','REACHABLE','UNAVAILABLE','UNCERTAIN'].includes(r.health)||!Number.isSafeInteger(r.checkedAt)||r.checkedAt<0||!Number.isSafeInteger(r.createdAt)||r.createdAt<0||!Number.isSafeInteger(r.notFoundAt)||r.notFoundAt<0)throw new Error('Trạng thái đồng bộ không hợp lệ');
  return r;
}
export function validateBatch(data) {
  if(!['linkmap.journal/1',PROTOCOL].includes(data?.protocol)||!Array.isArray(data.records)||data.records.length>1000)throw new Error('Định dạng Drive chưa được hỗ trợ');
  return data.records.map(validateRecord);
}
export function spiral(slot) {
  if(slot===0)return [0,0];
  const ring=Math.ceil((Math.sqrt(slot+1)-1)/2),side=2*ring,d=(2*ring+1)**2-1-slot;
  if(d<side)return [ring-d,ring]; if(d<side*2)return [-ring,ring-(d-side)];
  if(d<side*3)return [-ring+(d-side*2),-ring]; return [ring,-ring+(d-side*3)];
}
export const healthInfo={REACHABLE:['Truy cập được','#1aa76d','✓'],UNAVAILABLE:['Không truy cập được','#d54e65','×'],UNCERTAIN:['Cần xác minh','#c69226','?'],UNCHECKED:['Chưa kiểm tra','#52617a','·']};
