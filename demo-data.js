// Preview-only records. Never passed to IndexedDB, export or the Google Drive outbox.
const topics=['Jetpack Compose','Android','Kotlin','SwiftUI','Thiết kế UI','Design system','Figma','Ghi chú','Dự án cá nhân','Tài liệu học tập','Portfolio','Ảnh tham khảo','Sách','Nghiên cứu AI','Lập trình web','Đồng bộ dữ liệu','Công việc','Ý tưởng sản phẩm','Motion design','Typography','Màu sắc','Du lịch','Nhiếp ảnh','Âm nhạc','Video đã lưu'];
const repositories=['android/compose-samples','android/architecture-samples','android/nowinandroid','JetBrains/kotlin','JetBrains/compose-multiplatform','Kotlin/kotlinx.coroutines','Kotlin/kotlinx.serialization','square/okhttp','square/retrofit','coil-kt/coil','androidx/androidx','google/iosched','flutter/flutter','facebook/react','vercel/next.js','vuejs/core','sveltejs/svelte','microsoft/TypeScript','vitejs/vite','tailwindlabs/tailwindcss','supabase/supabase','firebase/firebase-android-sdk','google/material-design-icons','rust-lang/rust','golang/go'];
const pages=['Google','Microsoft','NASA','NatGeo','Adobe','figma','GitHub','AndroidOfficial','Samsung','Apple','NVIDIA','Intel','Meta','YouTube','Spotify','Netflix','TED','Duolingo','Canva','NotionHQ','OpenAI','BBCNews','Reuters','TheEconomist','MIT'];
const handles=['tiktok','nasa','natgeo','google','microsoft','adobe','figma','github','android','samsung','apple','nvidia','intel','youtube','spotify','netflix','tedtoks','duolingo','canva','notion','openai','bbcnews','reuters','mit','nfl'];
const healthAt=i=>i%19===0?'UNCHECKED':i%11===0?'UNCERTAIN':i%7===0?'UNAVAILABLE':'REACHABLE';
export const demoData=Array.from({length:150},(_,i)=>{
  const row=Math.floor(i/6),kind=i%6,topic=topics[row];
  const [url,title]=[
    [`https://www.facebook.com/${pages[row]}`,`Facebook · ${pages[row]}`],
    [`https://www.tiktok.com/@${handles[row]}`,`TikTok · @${handles[row]}`],
    [`https://github.com/${repositories[row]}`,`GitHub · ${repositories[row]}`],
    [`https://drive.google.com/drive/u/0/search?q=${encodeURIComponent(topic)}`,`Drive · ${topic}`],
    [`https://onedrive.live.com/?qt=search&q=${encodeURIComponent(topic)}`,`OneDrive · ${topic}`],
    [`https://www.google.com/search?q=${encodeURIComponent(topic)}`,`Google · ${topic}`]
  ][kind];
  return {id:`demo-${String(i+1).padStart(3,'0')}`,url,title,note:'Link mẫu để xem bố cục. Màu trạng thái được mô phỏng, chưa kiểm tra truy cập.',kind:'LINK',createdAt:Date.now()-((i*37)%540)*86400000,category:['Design & Giao diện','Dev / GitHub & Android','Tài liệu & Sách','Khám phá & Media'][Math.floor(i/6)%4],health:healthAt(i),checkedAt:0,reason:'Trạng thái minh họa, không phải kết quả kiểm tra link.'};
});
