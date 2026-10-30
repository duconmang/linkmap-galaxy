import {PROTOCOL,validateBatch} from './protocol.js';
import * as store from './storage.js';
export const DRIVE_SCOPE='https://www.googleapis.com/auth/drive.appdata';
let token=null,expires=0,session=null,driveGranted=false;
export function current(){return session;}
export function canSync(){return !!token&&driveGranted&&Date.now()<expires;}
export function signOut(){token=null;expires=0;session=null;driveGranted=false;}
function authorize(scopes,hint){
  return new Promise((resolve,reject)=>{
    const id=window.LINKMAP_CONFIG?.googleClientId;
    if(!id) return reject(new Error('Đăng nhập Google chưa được cấu hình. Cần OAuth Client ID chung của LinkMap.'));
    if(!window.google?.accounts?.oauth2)return reject(new Error('Không tải được Google. Kiểm tra kết nối và thử lại.'));
    google.accounts.oauth2.initTokenClient({client_id:id,scope:scopes,include_granted_scopes:true,hint,
      callback:r=>r.error?reject(new Error(r.error_description??r.error)):resolve(r),error_callback:()=>reject(new Error('Đã đóng hoặc chặn cửa sổ Google'))}).requestAccessToken({prompt:hint?'':'select_account'});
  });
}
export async function signIn(){
  const result=await authorize('openid email profile');
  const res=await fetch('https://www.googleapis.com/oauth2/v3/userinfo',{headers:{Authorization:`Bearer ${result.access_token}`}});
  if(!res.ok)throw new Error('Không xác minh được tài khoản Google');
  const profile=await res.json();if(!profile.sub||!profile.email_verified)throw new Error('Google chưa xác minh tài khoản này');
  token=result.access_token;expires=Date.now()+Math.max(0,result.expires_in-30)*1000;driveGranted=google.accounts.oauth2.hasGrantedAllScopes(result,DRIVE_SCOPE);
  session={sub:profile.sub,email:profile.email,name:profile.name??profile.email};return session;
}
export async function enableDrive(){
  if(!session)throw new Error('Đăng nhập Google trước');
  const result=await authorize(`openid email profile ${DRIVE_SCOPE}`,session.email);
  const res=await fetch('https://www.googleapis.com/oauth2/v3/userinfo',{headers:{Authorization:`Bearer ${result.access_token}`}});
  if(!res.ok)throw new Error('Không xác minh được quyền Drive');
  const profile=await res.json();if(profile.sub!==session.sub)throw new Error('Tài khoản Drive khác tài khoản LinkMap; chưa đồng bộ dữ liệu');
  if(!google.accounts.oauth2.hasGrantedAllScopes(result,DRIVE_SCOPE))throw new Error('Chưa cấp quyền dữ liệu ứng dụng Drive');
  token=result.access_token;expires=Date.now()+Math.max(0,result.expires_in-30)*1000;driveGranted=true;
}
async function api(url,options={}){
  const target=new URL(url);
  if(target.origin!=='https://www.googleapis.com'||!/^\/(drive\/v3\/files|upload\/drive\/v3\/files)(\/|\?|$)/.test(target.pathname))throw new Error('Đích Google API không hợp lệ');
  if(!canSync())throw new Error('Phiên Google hết hạn. Bấm Bật đồng bộ để kết nối lại.');
  const res=await fetch(url,{...options,headers:{...options.headers,Authorization:`Bearer ${token}`},signal:AbortSignal.timeout(30000)});
  if(!res.ok){if(res.status===401){token=null;driveGranted=false;}throw new Error(res.status===429?'Drive đang giới hạn tần suất. Thử lại sau.':`Google Drive phản hồi ${res.status}. Dữ liệu trên máy vẫn được giữ.`);}
  return res;
}
async function boundedJson(response,max=5*1024*1024){
  if(Number(response.headers.get('content-length'))>max)throw new Error('Nhật ký vượt 5 MB');
  const reader=response.body.getReader(),chunks=[];let bytes=0;
  try{while(true){const {done,value}=await reader.read();if(done)break;bytes+=value.byteLength;if(bytes>max)throw new Error('Nhật ký vượt 5 MB');chunks.push(value);}}catch(e){await reader.cancel();throw e;}
  const data=new Uint8Array(bytes);let offset=0;for(const c of chunks){data.set(c,offset);offset+=c.byteLength;}
  return JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(data));
}
let syncing=false;
export async function sync(namespace){
  if(syncing)return;const owner=session?.sub;if(namespace!==`google:${owner}`)throw new Error('Không đồng bộ thư viện khách hoặc tài khoản khác');
  syncing=true;
  try {
    let page='',pages=0;
    do {
      if(++pages>100)throw new Error('Nhật ký Drive quá lớn; dừng đồng bộ an toàn');
      const params=new URLSearchParams({spaces:'appDataFolder',q:"trashed = false and (appProperties has { key='linkmapProtocol' and value='1' } or appProperties has { key='linkmapProtocol' and value='2' })",fields:'nextPageToken,files(id,size)',pageSize:'1000',...(page?{pageToken:page}:{})});
      const data=await boundedJson(await api(`https://www.googleapis.com/drive/v3/files?${params}`));
      for(const f of data.files??[]){
        if(session?.sub!==owner)throw new Error('Tài khoản đã thay đổi');
        const state=await store.read(namespace);if(state?.seen.includes(f.id))continue;
        if(typeof f.id!=='string'||!/^[-\w]{1,256}$/.test(f.id)||!Number.isSafeInteger(Number(f.size))||Number(f.size)<0||Number(f.size)>5*1024*1024)throw new Error('File nhật ký không hợp lệ hoặc vượt 5 MB');
        const batch=await boundedJson(await api(`https://www.googleapis.com/drive/v3/files/${encodeURIComponent(f.id)}?alt=media`));validateBatch(batch);await store.apply(namespace,f.id,batch);
      }
      page=data.nextPageToken??'';
    }while(page);
    for(let count=0;count<200;count++){
      const state=await store.read(namespace),pending=[];let bytes=0;
      for(const entry of state?.outbox.slice(0,100)??[]){const size=new TextEncoder().encode(JSON.stringify(entry.record)).length;if(bytes+size>3*1024*1024)break;pending.push(entry);bytes+=size;}
      if(!pending.length)break;
      const boundary=`linkmap_${crypto.randomUUID()}`,name=`linkmap-${crypto.randomUUID()}.json`;
      const meta={name,mimeType:'application/json',parents:['appDataFolder'],appProperties:{linkmapProtocol:'2'}};
      const batch={protocol:PROTOCOL,records:pending.map(x=>x.record)};
      const body=`--${boundary}\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${JSON.stringify(meta)}\r\n--${boundary}\r\nContent-Type: application/json\r\n\r\n${JSON.stringify(batch)}\r\n--${boundary}--`;
      if(session?.sub!==owner)throw new Error('Tài khoản đã thay đổi');
      const uploaded=await(await api('https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id',{method:'POST',headers:{'Content-Type':`multipart/related; boundary=${boundary}`},body})).json();
      await store.apply(namespace,uploaded.id,batch);await store.ack(namespace,pending.map(x=>x.seq));
    }
  }finally{syncing=false;}
}
