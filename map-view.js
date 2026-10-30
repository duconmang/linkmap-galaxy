import {isNote,provider} from './protocol.js';
import {ageScale,pulse,platformKey,hash,RACK_W,RACK_H,RACK_GAP,RACK_AISLE,rackOrigin,dcBounds,toScreen,toWorld} from './map-model.js';

const colors={REACHABLE:'#25d6a0',UNAVAILABLE:'#fb648a',UNCERTAIN:'#f3c458',UNCHECKED:'#8393af'};
export class MapView {
  constructor(canvas,{onSelect,onCamera}){
    this.canvas=canvas;this.ctx=canvas.getContext('2d');this.onSelect=onSelect;this.onCamera=onCamera;this.mode='galaxy';this.zoom=1;this.pan={x:0,y:0};this.nodes=[];this.edges=[];this.active=new Set();this.selected=null;this.hover=null;this.effects=true;this.grid=true;this.connections=true;this.images={};this.cameras={};
    this.resizeObserver=new ResizeObserver(()=>this.resize());this.resizeObserver.observe(canvas);
    for(const name of ['facebook','tiktok','github','googledrive']){const im=new Image();im.onload=()=>{this.images[name]=im;this.draw();};im.src=`./assets/${name}.svg`;}
    this.pointers=new Map();canvas.addEventListener('pointerdown',e=>{canvas.focus({preventScroll:true});canvas.setPointerCapture(e.pointerId);this.pointers.set(e.pointerId,{x:e.offsetX,y:e.offsetY});this.dragged=false;this.start={x:e.offsetX,y:e.offsetY};});
    canvas.addEventListener('pointermove',e=>{
      const next={x:e.offsetX,y:e.offsetY};
      if(!this.pointers.has(e.pointerId)){const hit=this.hit(next.x,next.y);if(this.hover!==hit?.record.id){this.hover=hit?.record.id;canvas.style.cursor=hit?'pointer':'grab';this.draw();}return;}
      const old=this.pointers.get(e.pointerId);
      if(this.pointers.size===1){this.pan.x+=next.x-old.x;this.pan.y+=next.y-old.y;}else{const other=[...this.pointers.entries()].find(([id])=>id!==e.pointerId)[1],before=Math.hypot(old.x-other.x,old.y-other.y),after=Math.hypot(next.x-other.x,next.y-other.y);if(before)this.scale(after/before,(next.x+other.x)/2,(next.y+other.y)/2);}
      if(Math.hypot(next.x-this.start.x,next.y-this.start.y)>5)this.dragged=true;
      this.pointers.set(e.pointerId,next);this.draw();this.cameraChanged();
    });
    canvas.addEventListener('pointerup',e=>{const pinch=this.pointers.size>1;this.pointers.delete(e.pointerId);if(!this.dragged&&!pinch){const hit=this.hit(e.offsetX,e.offsetY);if(hit){this.selected=hit.record.id;this.draw();onSelect(hit.record);}}});
    canvas.addEventListener('pointercancel',e=>this.pointers.delete(e.pointerId));canvas.addEventListener('pointerleave',()=>{this.hover=null;this.draw();});
    canvas.addEventListener('wheel',e=>{e.preventDefault();this.scale(Math.exp(-e.deltaY*.0012),e.offsetX,e.offsetY);},{passive:false});
    canvas.addEventListener('keydown',e=>{if(e.key==='+'||e.key==='='){e.preventDefault();this.scale(1.25);}if(e.key==='-'){e.preventDefault();this.scale(.8);}if(e.key==='Home'){e.preventDefault();this.fit();}});
    this.motion=matchMedia('(prefers-reduced-motion: reduce)');this.motion.addEventListener('change',()=>this.animate());document.addEventListener('visibilitychange',()=>this.animate());this.animate();
  }
  setData(layout,edges,active){this.nodes=layout.nodes;this.layout=layout;this.edges=edges;this.active=active;this.draw();this.animate();}
  setMode(mode){this.cameras[this.mode]={zoom:this.zoom,pan:{...this.pan}};this.mode=mode;const saved=this.cameras[mode];if(saved){this.zoom=saved.zoom;this.pan={...saved.pan};}else this.needsFit=true;}
  resize(){const rect=this.canvas.getBoundingClientRect();if(!rect.width||!rect.height)return;const oldW=this.w,oldH=this.h;this.w=rect.width;this.h=rect.height;const dpr=Math.min(devicePixelRatio||1,2);this.dpr=dpr;this.canvas.width=Math.round(this.w*dpr);this.canvas.height=Math.round(this.h*dpr);if(!oldW||this.needsFit){this.fit();this.needsFit=false;}else{this.pan.x+=(this.w-oldW)/2;this.pan.y+=(this.h-oldH)/2;this.draw();}this.animate();}
  fit(ids=null){if(!this.w||!this.nodes.length){this.pan={x:this.w/2||0,y:this.h/2||0};this.zoom=1;this.draw();return;}
    const data=ids?this.nodes.filter(n=>ids.has(n.record.id)):this.nodes;const nodes=data.length?data:this.nodes;
    let left=Math.min(...nodes.map(n=>n.x))-55,right=Math.max(...nodes.map(n=>n.x))+55,top=Math.min(...nodes.map(n=>n.y))-55,bottom=Math.max(...nodes.map(n=>n.y))+70;
    if(this.mode==='dc'&&!ids){({left,top,right,bottom}=dcBounds(this.layout));}
    const inset=this.w>1050?255:0;const availableW=this.w-inset-120,availableH=this.h-170;
    this.zoom=Math.max(.12,Math.min(1.25,availableW/(right-left),availableH/(bottom-top)));this.pan={x:inset+(this.w-inset)/2-(left+right)/2*this.zoom,y:this.h*.48-(top+bottom)/2*this.zoom};this.draw();this.cameraChanged();
  }
  centerOne(id){const n=this.nodes.find(n=>n.record.id===id);if(!n)return;this.selected=id;this.zoom=this.mode==='dc'?1.65:2.3;this.pan={x:this.w*.53-n.x*this.zoom,y:this.h*.47-n.y*this.zoom};this.draw();this.cameraChanged();}
  scale(f,x=this.w/2,y=this.h/2){const next=Math.max(.1,Math.min(5,this.zoom*f));this.pan.x=x-(x-this.pan.x)*next/this.zoom;this.pan.y=y-(y-this.pan.y)*next/this.zoom;this.zoom=next;this.draw();this.cameraChanged();}
  resetScale(){this.scale(1/this.zoom);}
  cameraChanged(){this.onCamera?.({zoom:this.zoom,x:Math.round(-this.pan.x/this.zoom+this.w/this.zoom/2),y:Math.round(-this.pan.y/this.zoom+this.h/this.zoom/2)});}
  radius(n){return (this.mode==='dc'?12:12+18*ageScale(n.record.createdAt))*this.zoom;}
  hit(x,y){const p=toWorld({x,y},this.zoom,this.pan);let nearest=null,best=Infinity;for(const n of this.nodes){if(!this.active.has(n.record.id))continue;const distance=Math.hypot(p.x-n.x,p.y-n.y)*this.zoom;if(distance<best&&distance<=Math.max(13,this.radius(n)*1.15)){nearest=n;best=distance;}}return nearest;}
  animate(){clearInterval(this.timer);if(this.effects&&!this.motion.matches&&!document.hidden&&this.canvas.offsetParent!==null&&this.nodes.length)this.timer=setInterval(()=>this.draw(),100);this.draw();}
  path(x,y,r,star){const c=this.ctx;c.beginPath();if(star){for(let i=0;i<10;i++){const a=-Math.PI/2+i*Math.PI/5,k=i%2?r*.49:r;const px=x+Math.cos(a)*k,py=y+Math.sin(a)*k;if(i)c.lineTo(px,py);else c.moveTo(px,py);}c.closePath();}else c.roundRect(x-r,y-r,r*2,r*2,Math.max(2,4*this.zoom));}
  drawRacks(){
    // Room, floor, bays, rack shells and LEDs share this camera. No fixed rack wallpaper.
    const c=this.ctx,z=this.zoom,b=dcBounds(this.layout),width=b.right-b.left,height=b.bottom-b.top;
    c.save();c.translate(this.pan.x,this.pan.y);c.scale(z,z);
    const poly=(points,fill,stroke)=>{c.beginPath();points.forEach(([x,y],i)=>i?c.lineTo(x,y):c.moveTo(x,y));c.closePath();c.fillStyle=fill;c.fill();if(stroke){c.strokeStyle=stroke;c.lineWidth=1;c.stroke();}};
    const rect=(x,y,w,h,r,fill,stroke)=>{c.beginPath();c.roundRect(x,y,w,h,r);c.fillStyle=fill;c.fill();if(stroke){c.strokeStyle=stroke;c.lineWidth=1;c.stroke();}};
    // Raised access floor, rear wall and illuminated perimeter.
    c.shadowColor='#000000';c.shadowBlur=45*z;c.shadowOffsetY=20*z;
    rect(b.left,b.top+62,width,height-62,22,'#07141f','#285166');c.shadowBlur=0;c.shadowOffsetY=0;
    const floor=c.createLinearGradient(0,b.top,0,b.bottom);floor.addColorStop(0,'#132b3b');floor.addColorStop(.5,'#0a1d2a');floor.addColorStop(1,'#0a1420');
    rect(b.left+3,b.top+65,width-6,height-68,20,floor);
    c.save();c.beginPath();c.roundRect(b.left+8,b.top+70,width-16,height-80,16);c.clip();
    c.lineWidth=.7;c.strokeStyle='#49768b29';
    if(this.grid){for(let x=b.left;x<b.right;x+=48){c.beginPath();c.moveTo(x,b.top+66);c.lineTo(x,b.bottom);c.stroke();}
    for(let y=b.top+66;y<b.bottom;y+=48){c.beginPath();c.moveTo(b.left,y);c.lineTo(b.right,y);c.stroke();}}
    c.restore();
    const wall=c.createLinearGradient(0,b.top,0,b.top+80);wall.addColorStop(0,'#213e50');wall.addColorStop(.5,'#142c3c');wall.addColorStop(1,'#0c1b29');
    rect(b.left,b.top,width,85,14,wall,'#3a697a');
    c.strokeStyle='#4ad8ec';c.lineWidth=2;c.shadowColor='#22c6f0';c.shadowBlur=12*z;c.beginPath();c.moveTo(b.left+20,b.top+76);c.lineTo(b.right-20,b.top+76);c.stroke();c.shadowBlur=0;
    c.textAlign='left';c.textBaseline='alphabetic';c.font='600 14px ui-monospace,monospace';c.fillStyle='#b4e3f0';c.fillText('LINKMAP / PERSONAL DATA CENTER',b.left+28,b.top+34);
    c.font='10px ui-monospace,monospace';c.fillStyle='#7499b2';c.fillText('ROOM 01   /   BOOKMARK STORAGE',b.left+28,b.top+55);
    c.textAlign='right';c.fillStyle='#59dcb9';c.fillText(`${this.layout.rackCount} RACKS · ${this.nodes.length} ITEMS`,b.right-28,b.top+43);
    // Each row has a service aisle and fixed marked bays.
    const rows=Math.ceil(this.layout.rackCount/this.layout.columns);
    for(let row=0;row<rows;row++){
      const y=row*(RACK_H+RACK_AISLE)+RACK_H+76;
      rect(b.left+22,y-24,width-44,51,8,'#0b2432');
      c.strokeStyle='#3a9cac55';c.lineWidth=1;c.setLineDash([10,9]);c.beginPath();c.moveTo(b.left+35,y+10);c.lineTo(b.right-35,y+10);c.stroke();c.setLineDash([]);
      c.fillStyle='#5c879f';c.font='9px ui-monospace,monospace';c.textAlign='left';c.fillText(`SERVICE AISLE ${String(row+1).padStart(2,'0')}     →`,b.left+35,y-5);
    }
    for(let i=0;i<this.layout.rackCount;i++){
      const [x,y]=rackOrigin(i,this.layout.columns),W=RACK_W,H=RACK_H;
      const active=this.nodes.filter(n=>Math.floor(n.slot/30)===i&&this.active.has(n.record.id)).length;
      // Cabinet feet sit inside a registered floor pad; all geometry is in world units.
      rect(x-18,y-32,W+57,H+64,12,'#0b2635','#346377');
      c.strokeStyle='#3dc6d359';c.lineWidth=1;c.beginPath();c.moveTo(x-10,y+H+20);c.lineTo(x+W+26,y+H+20);c.stroke();
      c.fillStyle='#091018a0';c.beginPath();c.ellipse(x+W/2+20,y+H+17,W*.62,17,0,0,Math.PI*2);c.fill();
      poly([[x+W,y],[x+W+21,y-21],[x+W+21,y+H-21],[x+W,y+H]],'#102736','#376171');
      poly([[x,y],[x+21,y-21],[x+W+21,y-21],[x+W,y]],'#2c4859','#527488');
      const front=c.createLinearGradient(x,0,x+W,0);front.addColorStop(0,'#2a4353');front.addColorStop(.055,'#142a3b');front.addColorStop(.94,'#0c1a28');front.addColorStop(1,'#304d60');
      rect(x,y,W,H,7,front,'#4c7187');
      rect(x+11,y+42,W-22,H-67,3,'#050d16','#1f3b50');
      c.fillStyle='#a7c8da';c.font='600 11px ui-monospace,monospace';c.textAlign='left';c.fillText(`RACK ${String(i+1).padStart(2,'0')}`,x+19,y+26);
      c.textAlign='right';c.fillStyle='#4da2bc';c.font='9px ui-monospace,monospace';c.fillText(`${active}/30`,x+W-17,y+26);
      for(let row=0;row<10;row++){
        const sy=y+65+row*35;rect(x+20,sy,W-40,31,3,row%2?'#122637':'#0e2030','#2e455750');
        for(let line=0;line<4;line++){c.fillStyle='#3b5b6d';c.fillRect(x+27,sy+7+line*4,8,1);}
        for(let col=0;col<3;col++){rect(x+37+col*66,sy+3,24,24,4,'#091520','#355268');}
        c.fillStyle='#426176';c.fillRect(x+195,sy+9,5,13);
      }
      c.fillStyle='#3ba7bc';c.fillRect(x+17,y+H-17,46,2);c.fillStyle='#6c91a9';c.textAlign='right';c.font='8px ui-monospace,monospace';c.fillText('LINK STORAGE',x+W-16,y+H-13);
    }
    c.restore();
  }
  draw(){if(!this.w||!this.h)return;const c=this.ctx;c.setTransform(this.dpr,0,0,this.dpr,0,0);c.clearRect(0,0,this.w,this.h);if(this.mode==='dc'&&this.layout)this.drawRacks();
    const selectedNode=this.nodes.find(n=>n.record.id===this.selected);const selectedKey=selectedNode&&!isNote(selectedNode.record)?platformKey(selectedNode.record):null;
    if(this.connections){for(const edge of this.edges){const a=edge.from,b=edge.to;const active=this.active.has(a.record.id)&&this.active.has(b.record.id);if(!active)continue;c.globalAlpha=selectedKey?(selectedKey===edge.key ? .95 : .07):(this.mode==='dc' ? .52 : .33);c.strokeStyle=edge.color;c.lineWidth=selectedKey===edge.key?1.6:.8;const x=a.x*this.zoom+this.pan.x,y=a.y*this.zoom+this.pan.y,xx=b.x*this.zoom+this.pan.x,yy=b.y*this.zoom+this.pan.y;c.beginPath();c.moveTo(x,y);if(this.mode==='dc'){const keys=['FACEBOOK','TIKTOK','GITHUB','DRIVE','ONEDRIVE','GOOGLE'],lane=keys.includes(edge.key)?keys.indexOf(edge.key):hash(edge.key)%6;if(Math.abs(a.x-b.x)>RACK_W){c.quadraticCurveTo((x+xx)/2,Math.min(y,yy)-(42+lane*11)*this.zoom,xx,yy);}else{const dx=xx-x,dy=yy-y,length=Math.hypot(dx,dy)||1,bend=(9+lane*4)*this.zoom;c.quadraticCurveTo((x+xx)/2-dy/length*bend,(y+yy)/2+dx/length*bend,xx,yy);}}else c.lineTo(xx,yy);c.stroke();}c.globalAlpha=1;}
    const seconds=performance.now()/1000;
    for(let i=0;i<this.nodes.length;i++){const n=this.nodes[i],r=this.radius(n),{x,y}=toScreen(n,this.zoom,this.pan);if(x+r<0||y+r<0||x-r>this.w||y-r>this.h)continue;
      const record=n.record,active=this.active.has(record.id),chosen=this.selected===record.id,hover=this.hover===record.id,color=isNote(record)?'#efba6c':colors[record.health]??colors.UNCHECKED;const glow=this.effects&&!this.motion.matches?pulse(record.id,seconds):.65;
      c.globalAlpha=active?1:.15;c.lineWidth=chosen?2.5:1.25;c.strokeStyle=color;c.shadowColor=color;c.shadowBlur=active?(this.mode==='dc'?(2+glow*5)*this.zoom:(4+glow*15))*(chosen?1.7:1):0;
      this.path(x,y,r,this.mode==='galaxy');c.fillStyle=this.mode==='dc'?color+'77':'#071725';c.fill();c.stroke();c.shadowBlur=0;
      if(active){c.globalAlpha=.3+glow*.65;c.strokeStyle=chosen?'#f6fbff':color;c.lineWidth=1+glow;this.path(x,y,r+2*this.zoom,this.mode==='galaxy');c.stroke();}c.globalAlpha=active?1:.15;
      const p=provider(record.url),image=this.images[p[3]],inner=r*(this.mode==='galaxy'?.72:1.1);
      if(r>(this.mode==='dc'?4:7)){if(image){c.drawImage(image,x-inner/2,y-inner/2,inner,inner);}else{c.fillStyle='#ecf9ff';c.font=`600 ${Math.max(this.mode==='dc'?6:9,inner)}px system-ui`;c.textAlign='center';c.textBaseline='middle';c.fillText(isNote(record)?'≡':p[0]==='GOOGLE'?'G':p[0]==='ONEDRIVE'?'☁':'↗',x,y);}}
      if(active&&r>10&&(chosen||hover||this.nodes.length<18||(this.mode==='galaxy'&&this.zoom>.55&&i%23===0))){
        c.font=`500 ${chosen?12:10}px system-ui`;c.textAlign='center';c.textBaseline='middle';let title=record.title;while(c.measureText(title).width>155&&title.length>1)title=title.slice(0,-1);if(title!==record.title)title+='…';const width=c.measureText(title).width+18,ly=y+r+17;
        c.fillStyle='#061221e8';c.beginPath();c.roundRect(x-width/2,ly-11,width,22,11);c.fill();c.strokeStyle=color+'66';c.lineWidth=.7;c.stroke();c.fillStyle=chosen?'#ffffff':'#c5e2f4';c.fillText(title,x,ly);
      }
    }c.globalAlpha=1;
  }
}
