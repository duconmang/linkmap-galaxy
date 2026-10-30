import {isNote,provider} from './protocol.js';
import {ageScale,pulse,platformKey,RACK_W,RACK_H,RACK_GAP} from './map-model.js';

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
    if(this.mode==='dc'&&!ids){left=-30;top=-35;right=this.layout.columns*(RACK_W+RACK_GAP)-RACK_GAP+30;bottom=Math.ceil(this.layout.rackCount/this.layout.columns)*(RACK_H+70)-70+35;}
    const inset=this.w>1050?255:0;const availableW=this.w-inset-120,availableH=this.h-170;
    this.zoom=Math.max(.12,Math.min(1.25,availableW/(right-left),availableH/(bottom-top)));this.pan={x:inset+(this.w-inset)/2-(left+right)/2*this.zoom,y:this.h*.48-(top+bottom)/2*this.zoom};this.draw();this.cameraChanged();
  }
  centerOne(id){const n=this.nodes.find(n=>n.record.id===id);if(!n)return;this.selected=id;this.zoom=this.mode==='dc'?1.65:2.3;this.pan={x:this.w*.53-n.x*this.zoom,y:this.h*.47-n.y*this.zoom};this.draw();this.cameraChanged();}
  scale(f,x=this.w/2,y=this.h/2){const next=Math.max(.1,Math.min(5,this.zoom*f));this.pan.x=x-(x-this.pan.x)*next/this.zoom;this.pan.y=y-(y-this.pan.y)*next/this.zoom;this.zoom=next;this.draw();this.cameraChanged();}
  resetScale(){this.scale(1/this.zoom);}
  cameraChanged(){this.onCamera?.({zoom:this.zoom,x:Math.round(-this.pan.x/this.zoom+this.w/this.zoom/2),y:Math.round(-this.pan.y/this.zoom+this.h/this.zoom/2)});}
  radius(n){return (this.mode==='dc'?15:12+18*ageScale(n.record.createdAt))*this.zoom;}
  hit(x,y){return [...this.nodes].reverse().find(n=>this.active.has(n.record.id)&&Math.hypot(x-(n.x*this.zoom+this.pan.x),y-(n.y*this.zoom+this.pan.y))<=Math.max(13,this.radius(n)*1.15));}
  animate(){clearInterval(this.timer);if(this.effects&&!this.motion.matches&&!document.hidden&&this.canvas.offsetParent!==null&&this.nodes.length)this.timer=setInterval(()=>this.draw(),100);this.draw();}
  path(x,y,r,star){const c=this.ctx;c.beginPath();if(star){for(let i=0;i<10;i++){const a=-Math.PI/2+i*Math.PI/5,k=i%2?r*.49:r;const px=x+Math.cos(a)*k,py=y+Math.sin(a)*k;if(i)c.lineTo(px,py);else c.moveTo(px,py);}c.closePath();}else c.roundRect(x-r,y-r,r*2,r*2,Math.max(2,4*this.zoom));}
  drawRacks(){const c=this.ctx,z=this.zoom;for(let i=0;i<this.layout.rackCount;i++){
    const wx=(i%this.layout.columns)*(RACK_W+RACK_GAP),wy=Math.floor(i/this.layout.columns)*(RACK_H+70),x=wx*z+this.pan.x,y=wy*z+this.pan.y,W=RACK_W*z,H=RACK_H*z;if(x+W<0||x>this.w||y+H<0||y>this.h)continue;
    const fill=c.createLinearGradient(x,0,x+W,0);fill.addColorStop(0,'#1e3448');fill.addColorStop(.07,'#0e1c2c');fill.addColorStop(.93,'#091522');fill.addColorStop(1,'#294357');
    c.shadowColor='#00c7ff18';c.shadowBlur=18;c.fillStyle=fill;c.beginPath();c.roundRect(x,y,W,H,9*z);c.fill();c.shadowBlur=0;c.strokeStyle='#4c748a80';c.lineWidth=1;c.stroke();
    c.fillStyle='#06111d';c.fillRect(x+12*z,y+43*z,W-24*z,H-68*z);c.fillStyle='#6d9fb5';c.font=`600 ${Math.max(8,11*z)}px ui-monospace,monospace`;c.textAlign='left';c.fillText(`LINKMAP / RACK ${String(i+1).padStart(2,'0')}`,x+16*z,y+26*z);
    for(let row=0;row<10;row++){const sy=y+(65+row*35)*z;c.fillStyle=row%2?'#132a3c':'#102233';c.fillRect(x+20*z,sy,W-40*z,31*z);c.strokeStyle='#66869922';c.strokeRect(x+20*z,sy,W-40*z,31*z);for(let line=0;line<5;line++){c.fillStyle='#54768a35';c.fillRect(x+28*z,sy+(6+line*4)*z,9*z,1*z);}c.fillStyle='#466378';c.fillRect(x+196*z,sy+9*z,5*z,14*z);}
    c.fillStyle='#599bb1';c.font=`${Math.max(8,8*z)}px ui-monospace,monospace`;c.fillText('USER-OWNED DATA · 30 NODES',x+17*z,y+431*z);
    c.strokeStyle='#42bff530';c.beginPath();c.moveTo(x+W*.08,y+H+5);c.lineTo(x+W*.92,y+H+5);c.stroke();
  }}
  draw(){if(!this.w||!this.h)return;const c=this.ctx;c.setTransform(this.dpr,0,0,this.dpr,0,0);c.clearRect(0,0,this.w,this.h);if(this.mode==='dc'&&this.layout)this.drawRacks();
    const selectedNode=this.nodes.find(n=>n.record.id===this.selected);const selectedKey=selectedNode&&!isNote(selectedNode.record)?platformKey(selectedNode.record):null;
    if(this.connections){for(const edge of this.edges){const a=edge.from,b=edge.to;const active=this.active.has(a.record.id)&&this.active.has(b.record.id);if(!active)continue;c.globalAlpha=selectedKey?(selectedKey===edge.key?.95:.09):(this.mode==='dc'?.26:.33);c.strokeStyle=edge.color;c.lineWidth=selectedKey===edge.key?1.6:.8;const x=a.x*this.zoom+this.pan.x,y=a.y*this.zoom+this.pan.y,xx=b.x*this.zoom+this.pan.x,yy=b.y*this.zoom+this.pan.y;c.beginPath();c.moveTo(x,y);if(this.mode==='dc'){c.lineTo((x+xx)/2,y);c.lineTo((x+xx)/2,yy);}c.lineTo(xx,yy);c.stroke();}c.globalAlpha=1;}
    const seconds=performance.now()/1000;
    for(let i=0;i<this.nodes.length;i++){const n=this.nodes[i],r=this.radius(n),x=n.x*this.zoom+this.pan.x,y=n.y*this.zoom+this.pan.y;if(x+r<0||y+r<0||x-r>this.w||y-r>this.h)continue;
      const record=n.record,active=this.active.has(record.id),chosen=this.selected===record.id,hover=this.hover===record.id,color=isNote(record)?'#efba6c':colors[record.health]??colors.UNCHECKED;const glow=this.effects&&!this.motion.matches?pulse(record.id,seconds):.65;
      c.globalAlpha=active?1:.15;c.lineWidth=chosen?2.5:1.25;c.strokeStyle=color;c.shadowColor=color;c.shadowBlur=active?(4+glow*15)*(chosen?1.7:1):0;
      this.path(x,y,r,this.mode==='galaxy');c.fillStyle=this.mode==='dc'?color:'#071725';c.fill();c.stroke();c.shadowBlur=0;
      if(active){c.globalAlpha=.3+glow*.65;c.strokeStyle=chosen?'#f6fbff':color;c.lineWidth=1+glow;this.path(x,y,r+2*this.zoom,this.mode==='galaxy');c.stroke();}c.globalAlpha=active?1:.15;
      const p=provider(record.url),image=this.images[p[3]],inner=r*(this.mode==='galaxy'?.72:1.1);
      if(r>7){if(image){c.drawImage(image,x-inner/2,y-inner/2,inner,inner);}else{c.fillStyle=this.mode==='dc'?'#071725':'#ecf9ff';c.font=`600 ${Math.max(9,inner)}px system-ui`;c.textAlign='center';c.textBaseline='middle';c.fillText(isNote(record)?'≡':p[0]==='GOOGLE'?'G':p[0]==='ONEDRIVE'?'☁':'↗',x,y);}}
      if(active&&r>10&&(chosen||hover||this.nodes.length<18||(this.mode==='galaxy'&&this.zoom>.55&&i%23===0))){
        c.font=`500 ${chosen?12:10}px system-ui`;c.textAlign='center';c.textBaseline='middle';let title=record.title;while(c.measureText(title).width>155&&title.length>1)title=title.slice(0,-1);if(title!==record.title)title+='…';const width=c.measureText(title).width+18,ly=y+r+17;
        c.fillStyle='#061221e8';c.beginPath();c.roundRect(x-width/2,ly-11,width,22,11);c.fill();c.strokeStyle=color+'66';c.lineWidth=.7;c.stroke();c.fillStyle=chosen?'#ffffff':'#c5e2f4';c.fillText(title,x,ly);
      }
    }c.globalAlpha=1;
  }
}
