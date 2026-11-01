import {merge,normalize,recordId,defaultTitle,validateBatch,detectContent,isNote} from './protocol.js';
const channel=typeof BroadcastChannel!=='undefined'?new BroadcastChannel('linkmap'):null;
let dbPromise;
function database(){return dbPromise??=new Promise((resolve,reject)=>{
  const req=indexedDB.open('linkmap',1); req.onupgradeneeded=()=>req.result.createObjectStore('state');
  req.onsuccess=()=>resolve(req.result);req.onerror=()=>reject(req.error);
});}
function empty(){return {clock:0,seq:0,device:crypto.randomUUID(),records:{},outbox:[],seen:[],slots:{},nextSlot:0};}
export async function update(namespace,fn){
  const db=await database();return new Promise((resolve,reject)=>{
    const tx=db.transaction('state','readwrite'),store=tx.objectStore('state'),req=store.get(namespace);let result;
    req.onsuccess=()=>{try{const state=req.result??empty();result=fn(state);store.put(state,namespace);}catch(e){reject(e);tx.abort();}};
    tx.oncomplete=()=>{channel?.postMessage(namespace);resolve(result);};tx.onerror=()=>reject(tx.error);
  });
}
export async function read(namespace){const db=await database();return new Promise((resolve,reject)=>{
  const r=db.transaction('state').objectStore('state').get(namespace);r.onsuccess=()=>resolve(r.result??null);r.onerror=()=>reject(r.error);
});}
export function listen(fn){channel?.addEventListener('message',e=>fn(e.data));}
export async function list(namespace){const s=await read(namespace);return Object.values(s?.records??{}).filter(r=>!r.deleted).sort((a,b)=>a.createdAt-b.createdAt||a.id.localeCompare(b.id)).filter((r,i,all)=>isNote(r)||all.findIndex(x=>!isNote(x)&&x.url===r.url)===i);}
function enqueue(s,r){s.records[r.id]=r;s.outbox.push({seq:++s.seq,record:structuredClone(r)});if(s.slots[r.id]===undefined)s.slots[r.id]=s.nextSlot++;}
export async function save(namespace,input,existing){
  const incoming=detectContent(input.content??(isNote(input)?input.note:input.url));
  const kind=isNote(existing)||isNote(input)?'NOTE':incoming.kind;
  if(existing&&!isNote(existing)&&kind==='NOTE')throw new Error('Nhập URL hợp lệ để sửa liên kết');
  const chosen=kind==='NOTE'?'':input.chosenUrl??(incoming.urls.length===1?incoming.urls[0]:null);
  if(kind==='LINK'&&(!chosen||!incoming.urls.includes(chosen)))throw new Error('Chọn một link trong nội dung trước khi lưu');
  const id=existing?.id??(kind==='NOTE'?(input.id?.match(/^note-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/)?input.id:`note-${crypto.randomUUID()}`):(input.id?.match(/^url-[a-f0-9]{64}$/)?input.id:await recordId(chosen)));
  const body=kind==='NOTE'?incoming.text:normalize(incoming.text)===chosen?(input.note??existing?.note??''):[input.note??'',incoming.text].filter(Boolean).join('\n\n');
  if(body.length>64000)throw new Error('Nội dung vượt 64.000 ký tự');
  return update(namespace,s=>{
    if(kind==='LINK'&&Object.values(s.records).some(r=>!r.deleted&&!isNote(r)&&r.url===chosen&&r.id!==id))throw new Error('Link đã có trên bản đồ');
    const old=s.records[id];if(!existing&&old&&!old.deleted)throw new Error('Link đã có trên bản đồ');
    const changed=!old||old.url!==chosen;
    const r={id,kind,url:chosen,title:(input.title||existing?.title||(kind==='NOTE'?body.split('\n').find(v=>v.trim())?.slice(0,120):defaultTitle(chosen))).slice(0,200),note:body,category:(input.category??old?.category??'Cá nhân').slice(0,50),createdAt:old?.createdAt??input.createdAt??Date.now(),counter:++s.clock,device:s.device,deleted:false,
      health:changed?'UNCHECKED':old.health,reason:changed?(kind==='NOTE'?'':'Chờ kết quả kiểm tra từ Android'):old.reason,checkedAt:changed?0:old.checkedAt,httpCode:changed?null:old.httpCode,notFoundAt:changed?0:old.notFoundAt,healthDevice:changed?'':old.healthDevice};
    enqueue(s,r);return r;
  });
}
export function remove(namespace,id){return update(namespace,s=>{const old=s.records[id];if(old)enqueue(s,{...old,deleted:true,counter:++s.clock,device:s.device});});}
export function apply(namespace,fileId,batch){const records=validateBatch(batch);return update(namespace,s=>{
  for(const r of records){s.records[r.id]=merge(s.records[r.id],r);s.clock=Math.max(s.clock,r.counter);if(s.slots[r.id]===undefined)s.slots[r.id]=s.nextSlot++;}
  if(!s.seen.includes(fileId))s.seen.push(fileId);
});}
export function ack(namespace,seqs){return update(namespace,s=>{const ids=new Set(seqs);s.outbox=s.outbox.filter(x=>!ids.has(x.seq));});}
