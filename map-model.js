import {provider,isNote} from './protocol.js';

export const platformColors={GITHUB:'#51d9ff',FACEBOOK:'#648bff',TIKTOK:'#ff78b7',DRIVE:'#ffcc66',ONEDRIVE:'#bb92ff',GOOGLE:'#4de0b3'};
export function hash(text){let n=2166136261;for(const c of text){n^=c.charCodeAt(0);n=Math.imul(n,16777619);}return n>>>0;}
export function platformKey(r){if(isNote(r))return `note:${r.id}`;const p=provider(r.url);return p[0]==='OTHER'?new URL(r.url).hostname.toLowerCase().replace(/^www\./,''):p[0];}
export function platformColor(key){return platformColors[key]??`hsl(${hash(key)%360} 72% 68%)`;}
export function ageScale(createdAt,now=Date.now()){return .34+.66*Math.exp(-Math.max(0,now-(createdAt??now))/86400000/120);}
export function pulse(id,seconds){const seed=hash(id);return .5+.5*Math.sin(seconds/(8+(seed%61)/10)*Math.PI*2+(seed%1000)/1000*Math.PI*2);}
export function galaxyPosition(slot){if(slot===0)return [0,0];const angle=slot*2.399963229728653,radius=Math.sqrt(slot+1)*43+18;return [Math.cos(angle)*radius,Math.sin(angle)*radius*.84];}
export const RACK_CAPACITY=30,RACK_W=230,RACK_H=450,RACK_GAP=38;
export function rackColumns(count){const racks=Math.max(1,Math.ceil(count/RACK_CAPACITY));return racks<=6?racks:Math.ceil(Math.sqrt(racks*2));}
export function rackPosition(slot,columns){const rack=Math.floor(slot/RACK_CAPACITY),local=slot%RACK_CAPACITY;return [(rack%columns)*(RACK_W+RACK_GAP)+49+(local%3)*66,Math.floor(rack/columns)*(RACK_H+70)+80+Math.floor(local/3)*35];}
export function makeLayout(records,slots={},mode='galaxy'){
  const ordered=[...records].sort((a,b)=>(slots[a.id]??0)-(slots[b.id]??0)||a.id.localeCompare(b.id));
  const maxSlot=Math.max(0,...ordered.map((r,i)=>slots[r.id]??i));
  const columns=rackColumns(maxSlot+1);
  return {columns,rackCount:Math.max(1,Math.ceil((maxSlot+1)/RACK_CAPACITY)),nodes:ordered.map((record,i)=>{const slot=slots[record.id]??i;const [x,y]=mode==='dc'?rackPosition(slot,columns):galaxyPosition(slot);return {record,slot,x,y};})};
}
// A connected tree per platform: n−1 lines instead of n*(n−1)/2, preserving every position.
export function buildEdges(nodes){
  const groups=new Map();for(const n of nodes){if(isNote(n.record))continue;const key=platformKey(n.record);if(!groups.has(key))groups.set(key,[]);groups.get(key).push(n);}
  const edges=[];
  for(const [key,group] of groups){
    group.sort((a,b)=>a.record.id.localeCompare(b.record.id));if(group.length<2)continue;
    const distance=Array(group.length).fill(Infinity),parent=Array(group.length).fill(-1),visited=new Set();distance[0]=0;
    for(let step=0;step<group.length;step++){
      let nearest=-1;for(let i=0;i<group.length;i++)if(!visited.has(i)&&(nearest<0||distance[i]<distance[nearest]))nearest=i;
      visited.add(nearest);if(parent[nearest]>=0)edges.push({from:group[parent[nearest]],to:group[nearest],key,color:platformColor(key)});
      for(let i=0;i<group.length;i++)if(!visited.has(i)){const d=(group[nearest].x-group[i].x)**2+(group[nearest].y-group[i].y)**2;if(d<distance[i]){distance[i]=d;parent[i]=nearest;}}
    }
  }
  return edges;
}
export function matches(r,query){const fold=s=>String(s??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').toLowerCase();return fold([r.title,r.url,r.note,r.category,provider(r.url)[1]].join(' ')).includes(fold(query.trim()));}
