const KEY='igris-neet-v1';
const BASE_TITLE='IGRIS — NEET Command Center';
const MISSION_TYPES=['Practice','Revision','Lecture'];
const today=()=>{const d=new Date();return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0')};
const addDays=(isoDate,days)=>{
  const [y,m,d]=String(isoDate||today()).split('-').map(Number);
  const dt=new Date(y,(m||1)-1,(d||1)+days,12,0,0);
  return dt.getFullYear()+'-'+String(dt.getMonth()+1).padStart(2,'0')+'-'+String(dt.getDate()).padStart(2,'0');
};
const seed=()=>({
  tasks:[],
  chapters:{Physics:[],Chemistry:[],Biology:[]},
  sessions:[],
  mistakes:[],
  revisions:[],
  tests:[],
  vault:[],
  quizzes:[],
  quizAttempts:[],
  upcomingTests:[],
  settings:{name:'IGRIS',currentRank:'4 Lakh',upcomingRank:'10k',geminiKey:'',notifyEnabled:true}
});

let db=load(), current='dashboard', missionTab='open', mistakeTab='open', practiceTab='quiz', trackTab='Physics', strengthFilter='All', vaultFilter='All', attemptChapterFilter='All', selectedQuizSubject=null;
let timerSeconds=25*60, timerInitial=25*60, timerRunning=false, timerInterval=null, clockInterval=null, quizClockInterval=null, reminderInterval=null;
let activeQuiz=null, activeQuizMeta={subject:'All',chapter:'All'}, quizIdx=0, quizScore=0, quizAnswered=false, quizPickedIdx=null, quizWrongList=[], quizElapsedSec=0, retestIdx=0, aiOutput='';
let topBannerHidden=false, activeReminderTaskId=null;
let timerEndAt=0, timerLabel='', wakeLock=null, ongoingNotifLast=0;
let timerMissionId=null, missionRanSecs=0, runStartedAt=0, missionSessionId=null, lastMissionFlush=0;
let vaultChapterFilter='All', mistakeOpenGroups=new Set(), activeQuizKey=null, activeQuizArgs=null;
let mockLabFix=8, mockLabFocusId=null;
let skipDrill=false, drillNet=0, drillForced=0, drillRight=0, drillWrong=0, drillSkip=0;

const SAMPLE_JSON=JSON.stringify([
  {
    "subject":"Biology",
    "chapter":"Cell Cycle and Cell Division",
    "q":"Crossing over takes place between non-sister chromatids of homologous chromosomes during which stage of meiosis I?",
    "opts":["Leptotene","Pachytene","Diplotene","Diakinesis"],
    "ans":1,
    "mark":"Imp",
    "exp":"Recombination nodules appear during Pachytene where crossing over occurs between non-sister chromatids."
  },
  {
    "subject":"Physics",
    "chapter":"Laws of Motion",
    "q":"A block of mass 5 kg rests on a horizontal surface (mu = 0.2). What horizontal force is just sufficient to move it? (g = 10 m/s^2)",
    "opts":["5 N","10 N","15 N","20 N"],
    "ans":1,
    "mark":"Hard",
    "exp":"Limiting friction f_max = mu * N = 0.2 * (5 * 10) = 10 N."
  }
],null,2);

function load(){
  try{
    const parsed=JSON.parse(localStorage.getItem(KEY)||'{}');
    const base=seed();
    return {...base,...parsed,settings:{...base.settings,...(parsed.settings||{})}};
  }catch(e){return seed()}
}
function save(){localStorage.setItem(KEY,JSON.stringify(db));}
function esc(s=''){return String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function toast(s){let t=document.getElementById('toast');if(!t)return;t.textContent=s;t.classList.add('show');setTimeout(()=>t.classList.remove('show'),2300)}
function dateText(d=today()){return new Date(d+'T12:00:00').toLocaleDateString('en-IN',{weekday:'short',day:'numeric',month:'short',year:'numeric'})}

async function requestBrowserNotifyPermission(showToast=true){
  if(!('Notification' in window)){
    if(showToast)toast('Browser notifications are not supported here.');
    return false;
  }
  let perm=Notification.permission;
  if(perm==='default'){
    try{perm=await Notification.requestPermission()}catch(_){perm=Notification.permission}
  }
  const ok=perm==='granted';
  db.settings.notifyEnabled=ok;
  save();
  if(current==='focus'||current==='missions')render();
  if(showToast){
    if(ok){
      const err=await sendTopNotification('IGRIS test ✅','Notification bar working hai.','igris-test');
      toast(err?('Notification error: '+err):'Test notification bhej di, shade check karo.');
    }else toast('Notification permission blocked.');
  }
  return ok;
}
window.requestBrowserNotifyPermission=()=>requestBrowserNotifyPermission(true);

async function sendTopNotification(titleText,bodyText,tag='igris-top',opts={}){
  if(!('Notification' in window))return 'Notification not supported';
  if(Notification.permission!=='granted')return 'Permission: '+Notification.permission;
  if(db.settings.notifyEnabled===false)return 'Notifications disabled in settings';
  const o={body:bodyText,tag,icon:'./icon-192.png',badge:'./icon-192.png',...opts};
  if(o.silent)delete o.vibrate;
  else if(!o.vibrate)o.vibrate=[200,100,200];
  try{
    if('serviceWorker' in navigator){
      const reg=await navigator.serviceWorker.getRegistration();
      if(reg){await reg.showNotification(titleText,o);return null}
    }
    new Notification(titleText,o);
    return null;
  }catch(e){return String(e&&e.message||e)}
}
async function closeOngoingNotif(){
  try{
    if('serviceWorker' in navigator){
      const reg=await navigator.serviceWorker.getRegistration();
      if(reg){(await reg.getNotifications({tag:'igris-focus-timer'})).forEach(n=>n.close())}
    }
  }catch(_){}
}

function updateTopFocusUI(){
  const banner=document.getElementById('topFocusBanner');
  const txtEl=document.getElementById('topFocusText');
  const activeOrPaused=timerRunning || timerSeconds!==timerInitial;
  if(banner && txtEl){
    if(activeOrPaused && !topBannerHidden){
      banner.hidden=false;
      txtEl.textContent=`${timerRunning?'FOCUS RUNNING':'FOCUS PAUSED'} • ${fmtMMSS(timerSeconds)} REMAINING`;
    }else{
      banner.hidden=true;
    }
  }
  document.title=timerRunning?`⏱ ${fmtMMSS(timerSeconds)} — IGRIS`:BASE_TITLE;
}

window.hideTopFocusBanner=()=>{
  topBannerHidden=true;
  updateTopFocusUI();
};

function formatReminderLabel(isoLocal){
  if(!isoLocal)return '';
  const dt=new Date(isoLocal);
  if(Number.isNaN(dt.getTime()))return isoLocal;
  return dt.toLocaleString('en-IN',{day:'numeric',month:'short',hour:'2-digit',minute:'2-digit'});
}

function showReminderAlert(task){
  activeReminderTaskId=task.id;
  const bar=document.getElementById('reminderAlertBar');
  const tEl=document.getElementById('reminderAlertTitle');
  const bEl=document.getElementById('reminderAlertBody');
  const openBtn=document.getElementById('reminderOpenBtn');
  if(bar && tEl && bEl){
    tEl.textContent=`MISSION REMINDER • ${task.subject||'GENERAL'}`;
    bEl.textContent=`${task.text} (${formatReminderLabel(task.reminderAt)})`;
    bar.hidden=false;
  }
  if(openBtn){
    openBtn.onclick=()=>{
      dismissReminderAlert();
      navigate('missions');
    };
  }
  toast(`Reminder: ${task.text}`);
  sendTopNotification(`IGRIS Mission Reminder (${task.subject||'General'})`,task.text,'igris-mission-reminder',{requireInteraction:true,renotify:true});
}

window.dismissReminderAlert=()=>{
  activeReminderTaskId=null;
  const bar=document.getElementById('reminderAlertBar');
  if(bar)bar.hidden=true;
};

window.clearTaskReminder=(id)=>{
  const t=db.tasks.find(x=>x.id===id);
  if(!t)return;
  delete t.reminderAt;
  delete t.reminded;
  save();
  render();
  toast('Mission reminder removed.');
};

window.snoozeTaskReminder=(id,mins=10)=>{
  const t=db.tasks.find(x=>x.id===id);
  if(!t)return;
  const nextDt=new Date(Date.now()+mins*60000);
  const localIso=`${nextDt.getFullYear()}-${String(nextDt.getMonth()+1).padStart(2,'0')}-${String(nextDt.getDate()).padStart(2,'0')}T${String(nextDt.getHours()).padStart(2,'0')}:${String(nextDt.getMinutes()).padStart(2,'0')}`;
  t.reminderAt=localIso;
  t.reminded=false;
  dismissReminderAlert();
  save();
  render();
  toast(`Snoozed for ${mins} mins.`);
};

function checkDueReminders(){
  const now=Date.now();
  let changed=false;
  for(const t of db.tasks){
    if(t.done || !t.reminderAt || t.reminded)continue;
    const dueMs=new Date(t.reminderAt).getTime();
    if(!Number.isNaN(dueMs) && dueMs<=now){
      t.reminded=true;
      changed=true;
      showReminderAlert(t);
      break;
    }
  }
  const dueRev=typeof dueRevisionsList==='function'?dueRevisionsList():[];
  if(dueRev.length && db.settings.lastRevAlert!==today()){
    db.settings.lastRevAlert=today();
    changed=true;
    const first=dueRev[0];
    showReminderAlert({
      id:'rev-'+first.id,
      subject:first.subject||'Revision',
      text:`Spaced revision due: ${first.chapter} (+${dueRev.length>1?(dueRev.length-1)+' more':''})`,
      reminderAt:today()
    });
    sendTopNotification('IGRIS Revision Due',`${dueRev.length} chapter(s) need spaced revision`,'igris-revision',{requireInteraction:true});
  }
  if(changed)save();
}

window.toggleZenMode=force=>{
  const next=typeof force==='boolean'?force:!document.body.classList.contains('zen-mode');
  document.body.classList.toggle('zen-mode',next);
  toast(next?'Full view enabled (Sidebar hidden).':'Sidebar restored.');
};
const zenBtn=document.getElementById('zenBtn');
if(zenBtn)zenBtn.onclick=()=>toggleZenMode();

function ensureTimerFullEl(){
  let el=document.getElementById('timerFull');
  if(el)return el;
  el=document.createElement('div');
  el.id='timerFull';
  el.className='timer-full';
  el.hidden=true;
  el.innerHTML=`<button type="button" class="icon-btn tf-close" aria-label="Close" onclick="toggleTimerFullscreen(false)">×</button>
  <div class="tf-label" id="tfLabel"></div>
  <div class="tf-time" id="tfTime">00:00:00</div>
  <div class="tf-state" id="tfState"></div>
  <div class="actions" style="justify-content:center;margin-top:30px"><button type="button" class="btn" id="tfToggle">Start focus</button><button type="button" class="btn secondary" id="tfReset">Reset</button></div>`;
  document.body.appendChild(el);
  el.querySelector('#tfToggle').onclick=()=>startTimer();
  el.querySelector('#tfReset').onclick=()=>resetCurrentTimer();
  return el;
}
function updateTimerFullDOM(){
  const el=document.getElementById('timerFull');
  if(!el||el.hidden)return;
  const {h,m,s}=timerParts(timerSeconds);
  document.getElementById('tfTime').textContent=`${h}:${m}:${s}`;
  document.getElementById('tfToggle').textContent=timerRunning?'Pause':'Start focus';
  document.getElementById('tfState').textContent=timerRunning?'FOCUS SESSION ACTIVE':(timerSeconds<=0?'SESSION COMPLETE':(timerSeconds!==timerInitial?'PAUSED':'READY WHEN YOU ARE'));
  document.getElementById('tfLabel').textContent=timerLabel?('MISSION • '+timerLabel):'';
}
window.toggleTimerFullscreen=async force=>{
  const el=ensureTimerFullEl();
  const next=typeof force==='boolean'?force:el.hidden;
  el.hidden=!next;
  document.body.classList.toggle('timer-full-open',next);
  if(next){
    updateTimerFullDOM();
    try{if(!document.fullscreenElement&&document.documentElement.requestFullscreen)await document.documentElement.requestFullscreen()}catch(_){}
    try{if('wakeLock' in navigator)wakeLock=await navigator.wakeLock.request('screen')}catch(_){}
  }else{
    try{if(document.fullscreenElement)await document.exitFullscreen()}catch(_){}
    try{if(wakeLock){await wakeLock.release();wakeLock=null}}catch(_){}
  }
};
window.resetCurrentTimer=()=>{
  const keepId=timerMissionId, keepLabel=timerLabel;
  setTimer(Math.round(timerInitial/60)||25);
  if(keepId){timerMissionId=keepId;timerLabel=keepLabel;updateTimerDOM();refreshFocusPanels()}
};

document.addEventListener('keydown',e=>{
  if(e.key==='Escape' && document.body.classList.contains('zen-mode'))toggleZenMode(false);
  if(e.key==='Escape' && document.body.classList.contains('timer-full-open'))toggleTimerFullscreen(false);
});

const IDB_NAME='igris-files-db', STORE_NAME='files';
function openFilesDB(){
  return new Promise((res,rej)=>{
    const req=indexedDB.open(IDB_NAME,1);
    req.onupgradeneeded=()=>req.result.createObjectStore(STORE_NAME,{keyPath:'id'});
    req.onsuccess=()=>res(req.result);
    req.onerror=()=>rej(req.error);
  });
}
async function storeFile(file){
  const id=crypto.randomUUID();
  const dataUrl=await new Promise((res,rej)=>{
    const r=new FileReader();
    r.onload=()=>res(r.result);
    r.onerror=()=>rej(r.error);
    r.readAsDataURL(file);
  });
  const idb=await openFilesDB();
  await new Promise((res,rej)=>{
    const tx=idb.transaction(STORE_NAME,'readwrite');
    tx.objectStore(STORE_NAME).put({id,name:file.name,type:file.type,dataUrl});
    tx.oncomplete=res;
    tx.onerror=()=>rej(tx.error);
  });
  return {id,name:file.name,type:file.type};
}
async function getStoredFile(id){
  const idb=await openFilesDB();
  return new Promise((res,rej)=>{
    const tx=idb.transaction(STORE_NAME,'readonly');
    const req=tx.objectStore(STORE_NAME).get(id);
    req.onsuccess=()=>res(req.result);
    req.onerror=()=>rej(req.error);
  });
}
async function deleteStoredFile(id){
  if(!id)return;
  const idb=await openFilesDB();
  const tx=idb.transaction(STORE_NAME,'readwrite');
  tx.objectStore(STORE_NAME).delete(id);
}
let previewToken=0, previewPdf=null, previewObserver=null;
async function blobOfRec(rec){return (await fetch(rec.dataUrl)).blob()}
window.downloadStoredFile=async id=>{
  const rec=await getStoredFile(id);
  if(!rec){toast('File not found.');return}
  const blob=await blobOfRec(rec);
  const a=document.createElement('a');
  a.href=URL.createObjectURL(blob);
  a.download=rec.name;
  document.body.appendChild(a);a.click();a.remove();
  setTimeout(()=>URL.revokeObjectURL(a.href),60000);
  toast('Download ho gaya — ReadEra / Files se khol lo.');
};
window.shareStoredFile=async id=>{
  const rec=await getStoredFile(id);
  if(!rec){toast('File not found.');return}
  try{
    const blob=await blobOfRec(rec);
    const file=new File([blob],rec.name,{type:rec.type||blob.type});
    if(navigator.canShare&&navigator.canShare({files:[file]})){await navigator.share({files:[file],title:rec.name});return}
  }catch(e){if(e&&e.name==='AbortError')return}
  toast('Share sheet nahi mili, download kar raha hoon.');
  downloadStoredFile(id);
};
function loadPdfJs(){
  if(window.pdfjsLib)return Promise.resolve(window.pdfjsLib);
  return new Promise((res,rej)=>{
    const sc=document.createElement('script');
    sc.src='https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js';
    sc.onload=()=>{
      if(!window.pdfjsLib){rej(new Error('pdfjs missing'));return}
      window.pdfjsLib.GlobalWorkerOptions.workerSrc='https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';
      res(window.pdfjsLib);
    };
    sc.onerror=()=>rej(new Error('pdfjs load failed'));
    document.head.appendChild(sc);
  });
}
async function renderPdfInto(container,blob,token){
  const lib=await loadPdfJs();
  const buf=await blob.arrayBuffer();
  const pdf=await lib.getDocument({data:buf}).promise;
  if(token!==previewToken){try{pdf.destroy()}catch(_){}return}
  previewPdf=pdf;
  container.innerHTML='';
  const width=Math.max(280,Math.min((container.clientWidth||800)-4,1000));
  const first=await pdf.getPage(1);
  const v0=first.getViewport({scale:1});
  const estH=Math.round(width*v0.height/v0.width);
  if(previewObserver)previewObserver.disconnect();
  const dpr=Math.min(window.devicePixelRatio||1,2);
  previewObserver=new IntersectionObserver(entries=>{
    entries.forEach(async en=>{
      if(!en.isIntersecting)return;
      const holder=en.target;
      previewObserver&&previewObserver.unobserve(holder);
      if(token!==previewToken)return;
      try{
        const page=await pdf.getPage(+holder.dataset.n);
        const sc=width/page.getViewport({scale:1}).width;
        const vp=page.getViewport({scale:sc*dpr});
        const cv=document.createElement('canvas');
        cv.width=vp.width;cv.height=vp.height;
        cv.style.width=width+'px';cv.style.height=(vp.height/dpr)+'px';
        await page.render({canvasContext:cv.getContext('2d'),viewport:vp}).promise;
        if(token!==previewToken)return;
        holder.style.minHeight='';
        holder.innerHTML='';
        holder.appendChild(cv);
      }catch(_){}
    });
  },{rootMargin:'800px 0px'});
  for(let n=1;n<=pdf.numPages;n++){
    const h=document.createElement('div');
    h.className='pdf-page';h.dataset.n=n;h.style.minHeight=estH+'px';
    h.innerHTML=`<span class="muted" style="font-size:10px">Page ${n}</span>`;
    container.appendChild(h);
    previewObserver.observe(h);
  }
}
window.previewFile=async(fileId)=>{
  const rec=await getStoredFile(fileId);
  if(!rec){toast('File not found in local vault.');return}
  const token=++previewToken;
  document.getElementById('previewTitle').textContent=rec.name;
  const body=document.getElementById('previewBody');
  const isPdf=rec.type==='application/pdf'||/\.pdf$/i.test(rec.name||'');
  body.innerHTML=`<div class="preview-actions"><button type="button" class="btn" onclick="shareStoredFile('${fileId}')">📤 Open with… (ReadEra / Drive)</button><button type="button" class="btn secondary" onclick="downloadStoredFile('${fileId}')">⬇ Download</button></div>`
    +(isPdf?'<div id="pdfPages" class="pdf-pages"><div class="muted" style="font-size:11px;padding:20px">PDF load ho rahi hai…</div></div>':`<img src="${rec.dataUrl}" alt="${esc(rec.name)}">`);
  document.getElementById('previewModal').classList.add('open');
  if(isPdf){
    try{
      const blob=await blobOfRec(rec);
      await renderPdfInto(document.getElementById('pdfPages'),blob,token);
    }catch(e){
      if(token===previewToken){
        const box=document.getElementById('pdfPages');
        if(box)box.innerHTML='<div class="muted" style="font-size:11px;line-height:1.7;padding:20px">In-app PDF viewer load nahi hua (internet chahiye). Upar <b>Open with…</b> ya <b>Download</b> se ReadEra / Drive me khol lo.</div>';
      }
    }
  }
};
window.closePreview=()=>{
  previewToken++;
  if(previewObserver){previewObserver.disconnect();previewObserver=null}
  if(previewPdf){try{previewPdf.destroy()}catch(_){}previewPdf=null}
  document.getElementById('previewModal').classList.remove('open');
  document.getElementById('previewBody').innerHTML='';
};

const dateChipEl=document.getElementById('dateChip');
if(dateChipEl)dateChipEl.textContent=dateText();
const menuBtnEl=document.getElementById('menuBtn');
if(menuBtnEl)menuBtnEl.onclick=()=>document.getElementById('sidebar').classList.toggle('open');
document.querySelectorAll('.nav').forEach(b=>b.onclick=()=>navigate(b.dataset.view));

function navigate(v){
  clearInterval(quizClockInterval);
  current=v;
  document.querySelectorAll('.nav').forEach(b=>b.classList.toggle('active',b.dataset.view===v));
  document.getElementById('sidebar').classList.remove('open');
  render();
}
function title(s){document.getElementById('viewTitle').textContent=s}
function stat(label,value,note){return `<div class="stat"><div class="stat-label">${label}</div><div class="stat-value">${value}</div><div class="stat-note">${note}</div></div>`}
function progress(label,done,total){let p=total?Math.round(done/total*100):0;return `<div class="progress-row"><div class="progress-meta"><span>${label}</span><span>${p}%</span></div><div class="bar"><span style="width:${p}%"></span></div></div>`}
function todayTasks(){return db.tasks.filter(t=>t.date===today())}
function totalMinutes(){return db.sessions.filter(s=>s.date===today()).reduce((a,s)=>a+s.minutes,0)}
function totalQuestionsToday(){return db.sessions.filter(s=>s.date===today()).reduce((a,s)=>a+(s.questions||0),0)}
function examDate(){return db.settings.examDate||'2027-05-02'}

function getCountdownParts(){
  const [y,m,d]=examDate().split('-').map(Number);
  const target=new Date(y,m-1,d,9,0,0);
  const diff=Math.max(0,target-new Date());
  return {
    diff,
    days:Math.floor(diff/864e5),
    hrs:Math.floor((diff%864e5)/36e5),
    mins:Math.floor((diff%36e5)/6e4),
    secs:Math.floor((diff%6e4)/1e3)
  };
}
function pad2(n){return String(n).padStart(2,'0')}
function fmtMMSS(sec){return `${pad2(Math.floor(sec/60))}:${pad2(sec%60)}`}

function countdown(){
  const {days,hrs,mins,secs}=getCountdownParts();
  const all=Object.values(db.chapters).flat(),total=all.length,left=all.filter(c=>!c.done).length,custom=!!db.settings.examDateSet;
  const dt=new Date(examDate()+'T12:00:00').toLocaleDateString('en-IN',{weekday:'short',day:'numeric',month:'short',year:'numeric'});
  const pace=days>30&&total?`<div class="cd-note">Chapters left: <b>${left}</b> of ${total}. Aim for about <b>${(left/Math.max(Math.floor((days-30)/7),1)).toFixed(1)}</b> chapters/week to finish 30 days early.</div>`:'';
  return `<div class="card countdown">
    <div>
      <div class="eyebrow">NEET 2027 LIVE COUNTDOWN • ${custom?'YOUR DATE':'TENTATIVE'}: ${dt}</div>
      <div class="flip-group">
        <div class="flip-unit"><div class="flip-val" id="cdDays">${pad2(days)}</div><div class="flip-lbl">DAYS</div></div>
        <div class="flip-unit"><div class="flip-val" id="cdHrs">${pad2(hrs)}</div><div class="flip-lbl">HRS</div></div>
        <div class="flip-unit"><div class="flip-val" id="cdMins">${pad2(mins)}</div><div class="flip-lbl">MINS</div></div>
        <div class="flip-unit"><div class="flip-val" id="cdSecs">${pad2(secs)}</div><div class="flip-lbl">SECS</div></div>
      </div>
      ${pace}
    </div>
    <div class="cd-set">
      <label class="eyebrow" for="examDateInput">EXAM DATE</label>
      <div class="actions" style="margin-top:6px">
        <input type="date" id="examDateInput" value="${examDate()}" style="width:auto">
        <button type="button" class="btn secondary" onclick="setExamDate()">Save</button>
      </div>
      <div class="muted" style="font-size:9px;margin-top:6px">Update once NTA releases the official bulletin.</div>
    </div>
  </div>`;
}

function startLiveCountdown(){
  clearInterval(clockInterval);
  clockInterval=setInterval(()=>{
    const elD=document.getElementById('cdDays');
    if(!elD)return;
    const {days,hrs,mins,secs}=getCountdownParts();
    elD.textContent=pad2(days);
    document.getElementById('cdHrs').textContent=pad2(hrs);
    document.getElementById('cdMins').textContent=pad2(mins);
    document.getElementById('cdSecs').textContent=pad2(secs);
  },1000);
}

window.setExamDate=()=>{
  const v=document.getElementById('examDateInput').value;
  if(!/^\d{4}-\d{2}-\d{2}$/.test(v)){toast('Pick a valid date.');return}
  db.settings.examDate=v;db.settings.examDateSet=true;save();render();toast('Exam date saved.');
};

window.editRanks=()=>{
  const cur=prompt('Enter your Current Rank (e.g. 4 Lakh, 1.2 Lakh):',db.settings.currentRank||'4 Lakh');
  if(cur===null)return;
  const up=prompt('Enter your Upcoming Target Rank (e.g. 10k, 5k, AIR 500):',db.settings.upcomingRank||'10k');
  if(up===null)return;
  db.settings.currentRank=cur.trim()||'4 Lakh';
  db.settings.upcomingRank=up.trim()||'10k';
  save();render();toast('Rank targets updated.');
};

function backlogDueList(){
  const out=[];
  for(const sub of Object.keys(db.chapters)){
    for(const c of db.chapters[sub]){
      const revDone=['r1','r2','r3','r4','r5'].filter(k=>c.t&&c.t[k]).length;
      const isWeak=c.strength==='Weak'||(c.weakTopics&&c.weakTopics.trim());
      if(isWeak || (c.done && revDone===0)){
        const reason=isWeak?(c.weakTopics?`Weak: ${c.weakTopics}`:'Weak chapter'):'Completed • Rev 1 pending';
        out.push({sub,name:c.name,reason});
      }
    }
  }
  return out;
}

function dashboard(){
  title('Dashboard');
  let tasks=todayTasks(),done=tasks.filter(t=>t.done).length,mins=totalMinutes(),qCount=totalQuestionsToday(),openMistakes=db.mistakes.filter(m=>!m.closed).length;
  const curRank=esc(db.settings.currentRank||'4 Lakh'), upRank=esc(db.settings.upcomingRank||'10k');
  const dueList=backlogDueList().slice(0,5);
  return `<div class="hero">
    <div class="hero-row">
      <div>
        <div class="eyebrow">SYSTEM INITIALIZED • NEET 2027</div>
        <h1>Welcome back, 𓆩⚔️𓆪 IGRIS 💙</h1>
        <p>Your mission is not to be perfect. Your mission is to show up, learn from errors, and make the next attempt better.</p>
      </div>
      <div class="rank-box">
        <div class="rank">
          <button type="button" class="rank-edit" title="Edit Ranks" onclick="editRanks()">✎</button>
          <small>CURRENT RANK</small>
          <b>${curRank}</b>
        </div>
        <div class="rank upcoming">
          <button type="button" class="rank-edit" title="Edit Ranks" onclick="editRanks()">✎</button>
          <small>UPCOMING RANK</small>
          <b>${upRank}</b>
        </div>
      </div>
    </div>
  </div>
  ${countdown()}
  <div class="stats">
    ${stat('STREAK',streakLabel(),'6h focus or 60% missions daily')}
    ${stat('MISSIONS COMPLETED',`${done}/${tasks.length}`,'Today’s task progress')}
    ${stat('FOCUS TIME',`${Math.floor(mins/60)}h ${mins%60}m`,`${qCount} Qs logged today`)}
    ${stat('OPEN MISTAKES',openMistakes,`${db.mistakes.length} total tracked`)}
  </div>
  <div class="two-col">
    <div>
      <div class="section-head"><h2>Today's Missions</h2><small>${dateText()}</small></div>
      <div class="card">
        ${tasks.length?tasks.map(taskHTML).join(''):'<div class="empty">No missions added yet. Create your first daily target.</div>'}
        <div class="actions" style="margin-top:12px"><button class="btn" onclick="navigate('missions')">Manage missions</button></div>
      </div>
      <div class="section-head"><h2>Focus — last 7 days</h2><small>MINUTES</small></div>
      <div class="card">${barChart(dayKeys(7).map(dayLabel),dayKeys(7).map(k=>db.sessions.filter(x=>x.date===k).reduce((a,x)=>a+x.minutes,0)),'bar-focus')}</div>
      <div class="section-head"><h2>Subject Progress</h2><small>CHAPTERS MARKED COMPLETE</small></div>
      <div class="card">${['Physics','Chemistry','Biology'].map(s=>progress(s,db.chapters[s].filter(c=>c.done).length,db.chapters[s].length)).join('')||'No chapters yet'}</div>
    </div>
    <div>
      <div class="section-head"><h2>Quick Actions</h2><small>COMMAND SHORTCUTS</small></div>
      <div class="card">
        <div class="actions">
          <button class="btn" onclick="navigate('missions')">＋ Add mission</button>
          <button class="btn secondary" onclick="navigate('practice')">🩺 Practice & Quiz</button>
          <button class="btn secondary" onclick="navigate('aitutor')">✦ AI Tutor</button>
          <button class="btn secondary" onclick="navigate('vault')">🗂 Study Vault</button>
        </div>
      </div>
      <div class="section-head"><h2>Revision Due & Weak Backlog</h2><small>PRIORITY QUEUE</small></div>
      <div class="card">
        ${dueList.length?dueList.map(w=>`<div class="list-item"><div><b>${esc(w.name)}</b><div class="muted">${esc(w.sub)} • ${esc(w.reason)}</div></div><span class="tag gold">DUE</span></div>`).join(''):'<div class="empty">No weak or revision-due chapters flagged right now.</div>'}
        ${dueList.length?`<div class="actions" style="margin-top:10px"><button type="button" class="btn" onclick="generateWeakMissions()">＋ Auto missions from weak chapters</button></div>`:''}
      </div>
      <div class="section-head"><h2>Recent Activity</h2><small>LATEST RECORDS</small></div>
      <div class="card">${recentActivity()}</div>
    </div>
  </div>`;
}

function normalizeMissionType(t={}){
  if(t.missionType && MISSION_TYPES.includes(t.missionType))return t.missionType;
  const s=`${t.text||''} ${t.subject||''}`.toLowerCase();
  if(/\b(lec|lecture|class|video|oneshot|one shot)\b/.test(s))return 'Lecture';
  if(/\b(rev|revision|revise|notes|ncert|recall)\b/.test(s))return 'Revision';
  return 'Practice';
}

function missionTypeTagClass(mType){
  if(mType==='Practice')return 'tag blue';
  if(mType==='Revision')return 'tag gold';
  return 'tag purple';
}

window.startMissionFocus=(id)=>{
  const t=db.tasks.find(x=>x.id===id);
  const mins=parseInt(t?.focusMins,10);
  if(!t||!mins||mins<1){toast('Is mission ki focus duration set nahi hai.');return}
  pauseMissionClock();
  flushMissionSession();
  clearInterval(timerInterval);
  timerRunning=false;
  timerEndAt=0;
  resetMissionRun();
  timerMissionId=t.id;
  timerLabel=t.text;
  closeOngoingNotif();
  topBannerHidden=false;
  timerInitial=mins*60;
  timerSeconds=mins*60;
  navigate('focus');
  updateTimerDOM();
  toast(`Focus timer set to ${mins}m for "${t.text}"`);
};

function taskHTML(t){
  const carried=!t.done&&t.date<today();
  const mType=normalizeMissionType(t);
  const old=carried?`<span class="tag gold">Carried over • ${dateText(t.date)}</span>`:'';
  const rem=!t.done&&t.reminderAt?`<span class="reminder-chip">⏰ ${esc(formatReminderLabel(t.reminderAt))}${t.reminded?' • Sent':''}</span>`:'';
  const remActions=!t.done&&t.reminderAt?`<button type="button" class="icon-btn" style="padding:2px 6px;font-size:10px" title="Snooze 10m" onclick="snoozeTaskReminder('${t.id}',10)">+10m</button><button type="button" class="icon-btn" style="padding:2px 6px;font-size:10px" title="Clear reminder" onclick="clearTaskReminder('${t.id}')">🔕</button>`:'';
  const fBtn=!t.done&&t.focusMins?`<button type="button" class="btn secondary" style="padding:2px 7px;font-size:10px" onclick="startMissionFocus('${t.id}')">▶ ${t.focusMins}m</button>`:'';
  return `<label class="task ${t.done?'done':''}"><input type="checkbox" ${t.done?'checked':''} onchange="toggleTask('${t.id}',this.checked)"><span class="task-text">${esc(t.text)}</span><span class="${missionTypeTagClass(mType)}">${esc(mType)}</span>${old}${rem}${remActions}${fBtn}<small>${esc(t.subject||'GENERAL')}</small><button type="button" class="icon-btn" style="padding:2px 6px" onclick="deleteTask('${t.id}')">×</button></label>`;
}

function groupedMissionsHTML(list,emptyMsg){
  if(!list.length)return `<div class="empty">${emptyMsg}</div>`;
  const subs=['Physics','Chemistry','Biology'];
  const subIcons={Physics:'⚛',Chemistry:'⚗',Biology:'🧬'};
  const blocks=subs.map(sub=>{
    const subItems=list.filter(t=>(t.subject||'General').toLowerCase()===sub.toLowerCase());
    if(!subItems.length)return '';
    return `<div class="sub-group-box">
      <div class="sub-group-head">
        <span class="sub-group-title">${subIcons[sub]} ${sub}</span>
        <span class="tag blue">${subItems.length}</span>
      </div>
      <div class="type-blocks-grid">
        ${MISSION_TYPES.map(mType=>{
          const items=subItems.filter(t=>normalizeMissionType(t)===mType);
          return `<div class="type-block">
            <div class="type-block-head"><span>${mType}</span><span>${items.length}</span></div>
            ${items.length?items.map(taskHTML).join(''):`<div class="muted" style="font-size:10px;padding:6px 0">No ${mType.toLowerCase()}</div>`}
          </div>`;
        }).join('')}
      </div>
    </div>`;
  }).join('');

  const otherItems=list.filter(t=>!subs.some(s=>(t.subject||'General').toLowerCase()===s.toLowerCase()));
  const otherHtml=otherItems.length?`<div class="sub-group-box">
    <div class="sub-group-head">
      <span class="sub-group-title">📌 General / Other</span>
      <span class="tag gold">${otherItems.length}</span>
    </div>
    <div class="type-blocks-grid">
      ${MISSION_TYPES.map(mType=>{
        const items=otherItems.filter(t=>normalizeMissionType(t)===mType);
        return `<div class="type-block">
          <div class="type-block-head"><span>${mType}</span><span>${items.length}</span></div>
          ${items.length?items.map(taskHTML).join(''):`<div class="muted" style="font-size:10px;padding:6px 0">No ${mType.toLowerCase()}</div>`}
        </div>`;
      }).join('')}
    </div>
  </div>`:'';

  return blocks+otherHtml;
}

function recentActivity(){let items=[...db.sessions.map(x=>({date:x.date,text:`Focus session • ${x.subject} • ${x.minutes} min${x.questions?' • '+x.questions+' Qs':''}`})),...db.tests.map(x=>({date:x.date,text:`Mock test • ${x.name} • ${x.score} marks`})),...(db.quizAttempts||[]).map(x=>({date:x.date,text:`Quiz (${x.chapter}) • ${x.score}/${x.total} (${x.accuracy}%)`})),...allRevs().slice(0,5).map(x=>({date:x.date,text:`Revision • ${x.chapter} (${x.kind})`}))].sort((a,b)=>b.date.localeCompare(a.date)).slice(0,5);return items.length?items.map(x=>`<div class="list-item"><span>${esc(x.text)}</span><span class="muted">${x.date}</span></div>`).join(''):'<div class="empty">Your activity will appear here as you use IGRIS.</div>'}
function tabsHTML(cur,open,closed,fn){return `<div class="tabs"><button type="button" class="tab ${cur==='open'?'active':''}" onclick="${fn}('open')">Open <b>${open}</b></button><button type="button" class="tab ${cur==='closed'?'active':''}" onclick="${fn}('closed')">Closed <b>${closed}</b></button></div>`}

function suggestMissionSubject(txt=''){
  const s=txt.toLowerCase();
  if(/\b(nlm|kinematics|projectile|friction|work power|rotational|gravitation|shm|wave|electrostatics|capacitor|current electricity|magnetism|emi|ac|optics|ray optics|wave optics|modern physics|atom|nuclei|semiconductor|vector|unit)\b/.test(s))return 'Physics';
  if(/\b(mole|stoichiometry|atomic|thermodynamics|equilibrium|electrochemistry|kinetics|chemical bonding|coordination|organic|aldehyde|ketone|carboxylic|amine|haloalkane|alcohol|phenol|biomolecule|iupac|isomerism|periodic|solution|physical chem|inorganic)\b/.test(s))return 'Chemistry';
  if(/\b(cell|mitosis|meiosis|genetics|dna|rna|transcription|translation|evolution|reproduction|human physiology|digestion|breathing|circulation|excretion|neural|endocrine|plant physiology|photosynthesis|respiration|ecology|biodiversity|biotech|microbe|human health|ncert|morphology|anatomy)\b/.test(s))return 'Biology';
  return null;
}

function suggestMissionType(txt=''){
  const s=txt.toLowerCase();
  if(/\b(lec|lecture|class|video|oneshot|one shot)\b/.test(s))return 'Lecture';
  if(/\b(rev|revision|revise|notes|ncert|recall)\b/.test(s))return 'Revision';
  if(/\b(mcq|pyq|dpp|practice|question|numerical|test)\b/.test(s))return 'Practice';
  return null;
}

window.onMissionDescInput=el=>{
  const sel=document.querySelector('#taskForm select[name="subject"]');
  if(sel && sel.dataset.manual!=='1'){
    const hit=suggestMissionSubject(el.value);
    if(hit)sel.value=hit;
  }
  const typeSel=document.querySelector('#taskForm select[name="missionType"]');
  if(typeSel && typeSel.dataset.manual!=='1'){
    const tHit=suggestMissionType(el.value);
    if(tHit)typeSel.value=tHit;
  }
};

function missions(){
  title('Daily Missions');
  const open=db.tasks.filter(t=>!t.done).sort((a,b)=>a.date.localeCompare(b.date)),closed=db.tasks.filter(t=>t.done).sort((a,b)=>(b.doneAt||b.date).localeCompare(a.doneAt||a.date)),tt=todayTasks(),td=tt.filter(t=>t.done).length,list=missionTab==='open'?open:closed;
  const empty=missionTab==='open'?'No open missions. Add a task to begin.':'No closed missions yet. Complete one to see it here.';
  const notifyStatus=('Notification' in window)?Notification.permission:'unsupported';
  return `<div class="section-head"><h2>Mission Board</h2><small>${dateText()} • ${td}/${tt.length} TODAY</small></div>
  <div class="two-col">
    <div class="card"><h3>Your missions (${missionTab==='open'?'Open':'Closed'})</h3>${tabsHTML(missionTab,open.length,closed.length,'setMissionTab')}${groupedMissionsHTML(list,empty)}<div style="margin-top:12px">${progress('TODAY’S COMPLETION',td,tt.length)}</div></div>
    <div class="card"><h3>Deploy a new mission</h3>
      <form id="taskForm">
        <div class="field"><label>TASK DESCRIPTION</label><input name="text" placeholder="e.g. Redox rxn lec -02 / 30 MCQs" oninput="onMissionDescInput(this)" required></div>
        <div class="form-grid">
          <div class="field"><label>SUBJECT</label><select name="subject" onchange="this.dataset.manual='1'"><option>General</option><option>Physics</option><option>Chemistry</option><option>Biology</option></select></div>
          <div class="field"><label>MISSION TYPE TAG</label><select name="missionType" onchange="this.dataset.manual='1'"><option value="Practice">Practice</option><option value="Revision">Revision</option><option value="Lecture">Lecture</option></select></div>
        </div>
        <div class="form-grid">
          <div class="field"><label>FOCUS DURATION (MINS)</label><input name="focusMins" type="number" min="1" max="720" placeholder="e.g. 45"></div>
          <div class="field"><label>REMINDER TIME (OPTIONAL)</label><input type="datetime-local" name="reminderAt"></div>
        </div>
        <div class="actions" style="margin-top:4px">
          <button type="submit" class="btn">＋ Add mission</button>
          <button type="button" class="btn secondary" onclick="requestBrowserNotifyPermission()">🔔 Top Alert (${esc(notifyStatus)})</button>
        </div>
      </form>
      <div class="actions" style="margin-top:12px">
        <button type="button" class="btn secondary" onclick="generateWeakMissions()">⚔ Weak chapters → missions</button>
      </div>
      <p class="muted" style="font-size:10px;line-height:1.6;margin-top:16px">Open aur Closed dono lists Physics, Chemistry aur Biology mein banti hain — har subject ke andar Practice, Revision aur Lecture ke alag blocks hain. Weak-auto missions Subject HQ ke soft chapters se aate hain.</p>
    </div>
  </div>`;
}

window.generateWeakMissions=()=>{
  const weak=backlogDueList().slice(0,5);
  if(!weak.length){toast('No weak / rev-pending chapters found.');return}
  let n=0;
  for(const w of weak){
    const text=`${w.name}: 20 MCQ + short revision`;
    const exists=db.tasks.some(t=>!t.done&&t.date===today()&&String(t.text||'').includes(w.name));
    if(exists)continue;
    db.tasks.push({
      id:crypto.randomUUID(),
      date:today(),
      text,
      subject:w.sub||'General',
      missionType:'Practice',
      done:false,
      focusMins:45,
      source:'weak-auto'
    });
    n++;
  }
  save();
  missionTab='open';
  render();
  toast(n?`${n} weak-chapter mission(s) added for today.`:'Already queued for today.');
  if(n)navigate('missions');
};

function branchOf(c){if(c.branch)return c.branch;const i=NEET_SYLLABUS.Chemistry.findIndex(n=>n.toLowerCase()===c.name.trim().toLowerCase());return i<0||i<8?'Physical':i<12?'Inorganic':'Organic'}
function tabChapters(cfg){return(db.chapters[cfg.sub]||[]).filter(c=>!cfg.branch||branchOf(c)===cfg.branch)}
function shortDate(d){return new Date(d+'T12:00:00').toLocaleDateString('en-IN',{day:'numeric',month:'short'})}
function trackCols(cfg){return[['done','Done',0],...cfg.cols.map(([k,l])=>[k,l,0]),...Array.from({length:cfg.rev},(_,i)=>['r'+(i+1),'Rev '+(i+1),1])]}

window.setStrengthFilter=f=>{strengthFilter=f;render()};
window.updateChapterStrength=(sub,id,val)=>{
  const c=(db.chapters[sub]||[]).find(x=>x.id===id);
  if(c){c.strength=val;save();keepScroll(render);toast(`Marked as ${val}`)}
};
window.updateWeakTopics=(sub,id,val)=>{
  const c=(db.chapters[sub]||[]).find(x=>x.id===id);
  if(c){c.weakTopics=val.trim();save()}
};

function subjects(){
  title('Subject HQ');
  const cfg=TRACK[trackTab],cols=trackCols(cfg),allList=tabChapters(cfg),doneN=allList.filter(c=>c.done).length;
  const list=strengthFilter==='All'?allList:allList.filter(c=>(c.strength||'Medium')===strengthFilter);
  const cnt=k=>allList.filter(c=>k==='done'?c.done:(c.t||{})[k]).length;
  const weakCount=allList.filter(c=>c.strength==='Weak').length;

  return `<div class="section-head">
    <h2>Chapter Tracker & Strength Matrix</h2>
    <div class="actions" style="align-items:center">
      <select class="str-select" style="background:#14141d;color:#ddd" onchange="setStrengthFilter(this.value)">
        <option value="All" ${strengthFilter==='All'?'selected':''}>All Levels (${allList.length})</option>
        <option value="Weak" ${strengthFilter==='Weak'?'selected':''}>🔴 Weak (${weakCount})</option>
        <option value="Medium" ${strengthFilter==='Medium'?'selected':''}>🟡 Medium (${allList.filter(c=>(c.strength||'Medium')==='Medium').length})</option>
        <option value="Strong" ${strengthFilter==='Strong'?'selected':''}>🟢 Strong (${allList.filter(c=>(c.strength||'Medium')==='Strong').length})</option>
      </select>
      <button type="button" class="btn secondary" style="padding:7px 10px" onclick="loadSyllabus()">⬇ Load NEET chapters</button>
    </div>
  </div>
  <div class="tabs tracktabs">${TRACK_ORDER.map(k=>`<button type="button" class="tab ${trackTab===k?'active':''}" onclick="setTrackTab('${k}')">${TRACK[k].label}<b>${tabChapters(TRACK[k]).length}</b></button>`).join('')}</div>
  <div class="card" style="padding:12px 17px;margin-bottom:12px">${progress(cfg.label.toUpperCase()+' • CHAPTERS DONE',doneN,allList.length)}</div>
  ${list.length?`<div class="track-wrap"><table class="track">
    <thead>
      <tr>
        <th>CHAPTER</th>
        <th>LEVEL</th>
        <th>WEAK TOPICS</th>
        ${cols.map(([k,l,r])=>`<th class="${r?'rv':''}">${l}<small>${cnt(k)}/${allList.length}</small></th>`).join('')}
        <th></th>
      </tr>
    </thead>
    <tbody>
      ${list.map(c=>{
        const st=c.strength||'Medium';
        return `<tr>
          <td><div class="cn-wrap"><span class="cn">${esc(c.name)}</span></div></td>
          <td>
            <select class="str-select str-${st}" onchange="updateChapterStrength('${cfg.sub}','${c.id}',this.value)">
              <option value="Weak" ${st==='Weak'?'selected':''}>Weak</option>
              <option value="Medium" ${st==='Medium'?'selected':''}>Medium</option>
              <option value="Strong" ${st==='Strong'?'selected':''}>Strong</option>
            </select>
          </td>
          <td>
            <input type="text" class="weak-input" value="${esc(c.weakTopics||'')}" placeholder="Write weak subtopics..." onchange="updateWeakTopics('${cfg.sub}','${c.id}',this.value)">
          </td>
          ${cols.map(([k,l,r])=>{
            const v=k==='done'?(c.done?'1':''):((c.t||{})[k]||'');
            return`<td><button type="button" class="cell ${r?'rev':''} ${v?'on':''}" aria-label="${esc(l)}" onclick="toggleCell('${c.id}','${k}')">✓</button>${r?`<span class="cdate">${v&&v!=='1'?shortDate(v):''}</span>`:''}</td>`
          }).join('')}
          <td><button type="button" class="icon-btn" style="padding:2px 6px" title="Delete chapter" onclick="deleteChapter('${cfg.sub}','${c.id}')">×</button></td>
        </tr>`;
      }).join('')}
    </tbody>
  </table></div>`:'<div class="card"><div class="empty">No chapters match this filter.</div></div>'}
  <form class="card addrow" onsubmit="addTrackChapter(event)" style="margin-top:12px">
    <div class="field" style="flex:1;margin:0"><label>ADD CHAPTER TO ${cfg.label.toUpperCase()}</label><input name="chapter" placeholder="Chapter name" required></div>
    <button class="btn secondary">＋ Add</button>
  </form>`;
}

function timerParts(sec){
  const h=Math.floor(sec/3600);
  const m=Math.floor((sec%3600)/60);
  const s=sec%60;
  return {h:pad2(h),m:pad2(m),s:pad2(s)};
}

function missionRunSecs(){return missionRanSecs+(runStartedAt?(Date.now()-runStartedAt)/1000:0)}
function pauseMissionClock(){if(timerMissionId&&runStartedAt){missionRanSecs+=(Date.now()-runStartedAt)/1000;runStartedAt=0}}
function resetMissionRun(){missionRanSecs=0;runStartedAt=0;missionSessionId=null}
function flushMissionSession(){
  if(!timerMissionId)return;
  const secs=Math.round(missionRunSecs());
  const mins=Math.round(secs/60);
  const t=db.tasks.find(x=>x.id===timerMissionId);
  let rec=missionSessionId?db.sessions.find(x=>x.id===missionSessionId):null;
  if(!rec){
    if(mins<1)return;
    rec={id:crypto.randomUUID(),date:today(),subject:(t&&t.subject)||'Focus Timer',minutes:mins,questions:0,topic:(t&&t.text)||timerLabel||'Mission focus',missionId:timerMissionId,plannedMins:Math.round(timerInitial/60),seconds:secs};
    db.sessions.push(rec);
    missionSessionId=rec.id;
  }else{rec.minutes=mins;rec.seconds=secs}
  save();
  refreshFocusPanels();
}
window.missionRowAction=id=>{
  if(timerMissionId===id){
    if(timerRunning){startTimer();return}
    if(timerSeconds>0&&timerSeconds<timerInitial){startTimer();return}
  }
  startMissionFocus(id);
  startTimer();
};
function missionFocusRowsHTML(){
  const list=db.tasks.filter(t=>t.focusMins>0&&(!t.done||t.date===today())).sort((a,b)=>(a.done?1:0)-(b.done?1:0)||String(a.date).localeCompare(String(b.date)));
  if(!list.length)return '<div class="empty">Koi mission focus time ke saath nahi hai. Daily Missions me mission add karte waqt focus minutes daalo.</div>';
  return list.map(t=>{
    const used=db.sessions.filter(x=>x.date===today()&&x.missionId===t.id).reduce((a,x)=>a+x.minutes,0);
    const active=timerMissionId===t.id;
    const running=active&&timerRunning;
    const label=running?'⏸ Pause':(active&&timerSeconds>0&&timerSeconds<timerInitial?'▶ Resume':'▶ Start');
    return `<div class="list-item" style="gap:8px;${active?'border-left:3px solid #e33b4b;padding-left:8px':''}">
      <div style="min-width:0;flex:1"><b style="font-size:12px;${t.done?'text-decoration:line-through;opacity:.6':''}">${esc(t.text)}</b>
        <div class="muted" style="font-size:10px;margin-top:3px">${esc(t.subject||'GENERAL')} • planned ${t.focusMins}m${used?` • <span style="color:${used>=t.focusMins?'#86efac':'#e7c477'}">${used}m of ${t.focusMins}m used today</span>`:''}${running?' • <b style="color:#fca5a5">RUNNING</b>':''}</div></div>
      <button type="button" class="btn ${running?'':'secondary'}" style="padding:6px 12px;white-space:nowrap" onclick="missionRowAction('${t.id}')">${label}</button>
    </div>`;
  }).join('');
}
function todaySessionsHTML(){
  return db.sessions.filter(s=>s.date===today()).map(s=>`<div class="list-item"><span>${esc(s.subject)} — ${esc(s.topic||'Study session')}${s.questions?` <span class="tag blue">${s.questions} Qs</span>`:''}${s.plannedMins&&s.minutes<s.plannedMins?` <span class="tag gold">${s.minutes}m of ${s.plannedMins}m planned</span>`:''}</span><div style="display:flex;align-items:center;gap:8px"><b>${s.minutes}m</b><button type="button" class="icon-btn" style="padding:2px 6px" onclick="removeItem('sessions','${s.id}')">×</button></div></div>`).join('')||'<div class="empty">No sessions logged today.</div>';
}
function refreshFocusPanels(){
  const a=document.getElementById('missionFocusList');if(a)a.innerHTML=missionFocusRowsHTML();
  const b=document.getElementById('todaySessionsList');if(b)b.innerHTML=todaySessionsHTML();
  const c=document.getElementById('todaySessionsMeta');if(c)c.textContent=`${totalMinutes()} MIN • ${totalQuestionsToday()} Qs`;
}

function focus(){
  title('Focus Room');
  const {h,m,s}=timerParts(timerSeconds);
  const notifyStatus=('Notification' in window)?Notification.permission:'unsupported';
  return `<div class="section-head"><h2>Deep Work Protocol</h2><small>FLIP TIMER • TOP ALERT</small></div>
  <div class="two-col">
    <div class="card timer-wrap">
      <div class="timer-top-bar">
        <div class="timer-state" id="timerState">${timerRunning?'FOCUS SESSION ACTIVE':'READY WHEN YOU ARE'}</div>
        <div style="display:flex;gap:6px;flex-wrap:wrap"><button type="button" class="btn ghost" style="padding:4px 9px;font-size:10px" onclick="toggleTimerFullscreen(true)">⛶ Full screen</button><button type="button" class="btn ghost" style="padding:4px 9px;font-size:10px" onclick="requestBrowserNotifyPermission()">🔔 Alert: ${esc(notifyStatus)}</button></div>
      </div>
      <div class="timer-flip-wrap">
        <div class="timer-flip-unit"><div class="timer-flip-val" id="tHrs">${h}</div><div class="timer-flip-lbl">HRS</div></div>
        <div class="timer-colon">:</div>
        <div class="timer-flip-unit"><div class="timer-flip-val" id="tMins">${m}</div><div class="timer-flip-lbl">MINS</div></div>
        <div class="timer-colon">:</div>
        <div class="timer-flip-unit"><div class="timer-flip-val" id="tSecs">${s}</div><div class="timer-flip-lbl">SECS</div></div>
      </div>
      <div class="actions" style="justify-content:center;margin-bottom:14px">
        <button type="button" class="btn" id="startTimer">${timerRunning?'Pause':'Start focus'}</button>
        <button type="button" class="btn secondary" id="resetTimer">Reset</button>
      </div>
      <div class="actions" style="justify-content:center">
        <button type="button" class="btn ghost" onclick="setTimer(25)">25 min</button>
        <button type="button" class="btn ghost" onclick="setTimer(50)">50 min</button>
        <button type="button" class="btn ghost" onclick="setTimer(90)">90 min</button>
        <button type="button" class="btn ghost" onclick="setTimer(180)">3 hrs (Mock)</button>
      </div>
      <div class="custom-time-row">
        <input type="number" id="customMinInput" min="1" max="720" placeholder="Custom mins">
        <button type="button" class="btn secondary" onclick="applyCustomTimer()">Set</button>
      </div>
      <p class="muted" style="font-size:10px;margin-top:10px">Timer chalte waqt notification bar me live countdown dikhega aur session finish hone par alert aayega. Pehle 🔔 button se notification Allow kar do.</p>
      <p class="muted" id="tickDebug" style="font-size:9px;margin-top:6px"></p>
    </div>
    <div class="card">
      <h3>Mission focus timers</h3>
      <p class="muted" style="font-size:10px;line-height:1.6;margin-bottom:8px">Mission pe ▶ dabao, timer start ho jayega. Jitna timer chalega utna hi save hoga (planned se kam chala to wo bhi dikhega).</p>
      <div id="missionFocusList">${missionFocusRowsHTML()}</div>
      <details class="group" style="margin-top:14px"><summary><b>＋ Manual log (optional)</b></summary><div class="group-body" style="padding-bottom:12px">
        <form id="sessionForm">
          <div class="form-grid">
            <div class="field"><label>SUBJECT</label><select name="subject"><option>Physics</option><option>Chemistry</option><option>Biology</option><option>Revision</option></select></div>
            <div class="field"><label>DURATION (MINUTES)</label><input name="minutes" type="number" min="1" max="1440" placeholder="e.g. 50" required></div>
          </div>
          <div class="form-grid">
            <div class="field"><label>WHAT DID YOU WORK ON?</label><input name="topic" placeholder="e.g. Electrostatics numericals"></div>
            <div class="field"><label>MCQS SOLVED (OPTIONAL)</label><input name="questions" type="number" min="0" max="500" placeholder="e.g. 45"></div>
          </div>
          <button type="submit" class="btn">Save session</button>
        </form>
      </div></details>
      <div class="section-head"><h2>Today's sessions</h2><small id="todaySessionsMeta">${totalMinutes()} MIN • ${totalQuestionsToday()} Qs</small></div>
      <div id="todaySessionsList">${todaySessionsHTML()}</div>
    </div>
  </div>`;
}

function updateTimerDOM(){
  const {h,m,s}=timerParts(timerSeconds);
  const eh=document.getElementById('tHrs'),em=document.getElementById('tMins'),es=document.getElementById('tSecs');
  if(eh)eh.textContent=h;
  if(em)em.textContent=m;
  if(es)es.textContent=s;
  const b1=document.getElementById('startTimer');
  if(b1)b1.textContent=timerRunning?'Pause':'Start focus';
  updateTopFocusUI();
  updateTimerFullDOM();
}
window.setTimer=m=>{
  pauseMissionClock();
  flushMissionSession();
  timerMissionId=null;
  resetMissionRun();
  clearInterval(timerInterval);
  timerRunning=false;
  timerEndAt=0;
  timerLabel='';
  closeOngoingNotif();
  topBannerHidden=false;
  timerInitial=m*60;
  timerSeconds=m*60;
  render();
  updateTimerDOM();
};
window.applyCustomTimer=()=>{
  const v=parseInt(document.getElementById('customMinInput').value,10);
  if(!v||v<1||v>720){toast('Enter valid minutes (1–720).');return}
  setTimer(v);
  toast(`Timer set to ${v} mins.`);
};
function fmtClock(sec){const {h,m,s}=timerParts(sec);return (+h>0?h+':':'')+m+':'+s}
function updateOngoingNotif(force){
  if(!timerRunning)return;
  const now=Date.now();
  if(!force && now-ongoingNotifLast<15000)return;
  ongoingNotifLast=now;
  const endTxt=new Date(timerEndAt).toLocaleTimeString('en-IN',{hour:'2-digit',minute:'2-digit'});
  sendTopNotification(`⏳ Focus • ${fmtClock(timerSeconds)} left`,`${timerLabel?timerLabel+' • ':''}Ends at ${endTxt}`,'igris-focus-timer',{silent:true,requireInteraction:true});
}
function setTimerStateText(txt){const st=document.getElementById('timerState');if(st)st.textContent=txt}
function finishTimer(){
  clearInterval(timerInterval);
  timerRunning=false;
  timerEndAt=0;
  const completedMins=Math.max(1,Math.round(timerInitial/60));
  if(timerMissionId){
    runStartedAt=0;
    missionRanSecs=timerInitial;
    flushMissionSession();
    resetMissionRun();
  }else{
    db.sessions.push({id:crypto.randomUUID(),date:today(),subject:'Focus Timer',minutes:completedMins,questions:0,topic:'Completed flip timer session'});
  }
  save();
  toast(`Focus complete! ${completedMins} min session logged.`);
  sendTopNotification('⏰ IGRIS Focus Complete!',`${completedMins} min focus session logged.`,'igris-focus-timer',{requireInteraction:true,renotify:true,silent:false,vibrate:[300,150,300,150,600]});
  render();
}
function timerTick(){
  if(!timerRunning)return;
  try{
    timerSeconds=Math.max(0,Math.ceil((timerEndAt-Date.now())/1000));
    updateTimerDOM();
    const dbg=document.getElementById('tickDebug');
    if(dbg)dbg.textContent='sync '+new Date().toLocaleTimeString('en-IN');
    if(timerSeconds<=0){finishTimer();return}
    updateOngoingNotif(false);
    if(timerMissionId&&Date.now()-lastMissionFlush>15000){lastMissionFlush=Date.now();flushMissionSession()}
  }catch(e){console.error('timerTick',e)}
}
function startTimer(){
  if(timerRunning){
    timerTick();
    if(!timerRunning)return;
    clearInterval(timerInterval);
    pauseMissionClock();
    timerRunning=false;
    timerEndAt=0;
    closeOngoingNotif();
    flushMissionSession();
    setTimerStateText('PAUSED');
    updateTimerDOM();
    refreshFocusPanels();
    return;
  }
  if(timerSeconds<=0)timerSeconds=timerInitial;
  timerRunning=true;
  if(timerMissionId)runStartedAt=Date.now();
  lastMissionFlush=Date.now();
  topBannerHidden=false;
  timerEndAt=Date.now()+timerSeconds*1000;
  ongoingNotifLast=0;
  clearInterval(timerInterval);
  timerInterval=setInterval(timerTick,500);
  setTimerStateText('FOCUS SESSION ACTIVE');
  updateTimerDOM();
  refreshFocusPanels();
  setTimeout(()=>{try{updateOngoingNotif(true)}catch(_){}},0);
}

const MARKS=['Imp','Easy','Medium','Hard'];
function normalizeMark(val){
  const raw=String(val||'').trim().toLowerCase();
  if(raw==='imp' || raw==='important')return 'Imp';
  if(raw==='easy')return 'Easy';
  if(raw==='hard')return 'Hard';
  if(raw==='medium' || raw==='med')return 'Medium';
  return 'Medium';
}
function markBadgeStyle(mk){
  if(mk==='Imp')return 'background:#5c3d08;color:#ffd369;border-color:#a36d10';
  if(mk==='Hard')return 'background:#5e141f;color:#ff9aa6;border-color:#b8283e';
  if(mk==='Easy')return 'background:#12442f;color:#86efac;border-color:#1f7a55';
  return 'background:#1d2742;color:#93c5fd;border-color:#2d4375';
}

window.setPracticeTab=t=>{clearInterval(quizClockInterval);practiceTab=t;render()};
window.setAttemptChapterFilter=ch=>{attemptChapterFilter=ch;render()};
window.selectQuizSubject=sub=>{selectedQuizSubject=sub;render()};
window.exitActiveQuiz=()=>{clearInterval(quizClockInterval);saveQuizProgress();activeQuiz=null;quizIdx=0;quizAnswered=false;quizPickedIdx=null;quizElapsedSec=0;skipDrill=false;render()};
window.launchChapterBtn=btn=>{
  const sub=btn?.dataset?.sub||'All';
  const chap=btn?.dataset?.chap||'All';
  startSavedQuiz(sub,chap,'All');
};

function startQuizStopwatch(){
  clearInterval(quizClockInterval);
  if(!activeQuiz || quizIdx>=activeQuiz.length)return;
  quizClockInterval=setInterval(()=>{
    quizElapsedSec++;
    const el=document.getElementById('quizLiveClock');
    if(el)el.textContent=fmtMMSS(quizElapsedSec);
  },1000);
}

function getChaptersForSubject(sub){
  const fromDb=(db.chapters[sub]||[]).map(c=>c.name);
  const fromSyl=NEET_SYLLABUS[sub]||[];
  const fromQuiz=(db.quizzes||[]).filter(q=>q.subject===sub).map(q=>q.chapter).filter(Boolean);
  return [...new Set([...fromDb,...fromSyl,...fromQuiz])];
}

window.setQuestionMark=(markVal)=>{
  const norm=normalizeMark(markVal);
  if(!activeQuiz||!activeQuiz[quizIdx])return;
  const curQ=activeQuiz[quizIdx];
  curQ.mark=norm;
  const bankItem=(db.quizzes||[]).find(x=>x.id===curQ.id || x.q===curQ.q);
  if(bankItem)bankItem.mark=norm;
  save();
  render();
  toast(`Tagged as ${norm}`);
};

window.loadSampleJson=()=>{
  const ta=document.getElementById('bulkJsonInput');
  if(ta){ta.value=SAMPLE_JSON;toast('Sample NEET JSON loaded.')}
};

function quizKey(sub,chap,mark){return [sub,chap,mark].map(x=>String(x||'All').trim().toLowerCase()).join('|')}
function saveQuizProgress(){
  if(!activeQuizKey||!activeQuiz)return;
  db.quizProgress=db.quizProgress||{};
  if(quizIdx>=activeQuiz.length){delete db.quizProgress[activeQuizKey];save();return}
  db.quizProgress[activeQuizKey]={ids:activeQuiz.map(q=>q.id),idx:quizIdx,score:quizScore,wrong:quizWrongList,elapsed:quizElapsedSec,answered:quizAnswered,picked:quizPickedIdx,meta:activeQuizMeta,updatedAt:Date.now()};
  save();
}
window.restartActiveQuiz=()=>{
  if(!activeQuizKey||!activeQuizArgs){toast('Is quiz ko restart nahi kar sakte.');return}
  if(!confirm('Quiz reset karke pehle question se shuru karein?'))return;
  const args=activeQuizArgs;
  if(db.quizProgress)delete db.quizProgress[activeQuizKey];
  save();
  clearInterval(quizClockInterval);
  activeQuiz=null;
  startSavedQuiz(args[0],args[1],args[2]);
};
window.resetChapterProgress=btn=>{
  const key=quizKey(btn.dataset.sub,btn.dataset.chap,'All');
  if(db.quizProgress&&db.quizProgress[key]){delete db.quizProgress[key];save();render();toast('Is chapter ka progress reset ho gaya.')}
};
function beginQuizSession(items,meta,key=null,args=null){
  activeQuizKey=key;
  activeQuizArgs=args;
  activeQuiz=items.map(x=>({...x,mark:normalizeMark(x.mark||x.tag)}));
  activeQuizMeta=meta||{};
  quizIdx=0;
  quizScore=0;
  quizAnswered=false;
  quizPickedIdx=null;
  quizWrongList=[];
  quizElapsedSec=0;
  skipDrill=!!(meta&&meta.skipDrill);
  drillNet=0;drillForced=0;drillRight=0;drillWrong=0;drillSkip=0;
  practiceTab='quiz';
  render();
}
window.startSkipDrill=(n=15)=>{
  const bank=db.quizzes||[];
  if(bank.length<3){toast('Pehle Practice tab se kam se kam 3 MCQ import karo.');return}
  const pool=[...bank].sort(()=>Math.random()-0.5).slice(0,Math.min(n,bank.length));
  beginQuizSession(pool,{subject:'Mixed',chapter:'NEET Skip Drill',skipDrill:true});
  toast(`Skip drill: ${pool.length} Qs • +4 / −1 / 0 skip`);
};

function parseQuizJsonText(raw){
  let t=String(raw||'').trim();
  t=t.replace(/^```json\s*/i,'').replace(/^```\s*/i,'').replace(/\s*```$/,'');
  const start=t.indexOf('['), end=t.lastIndexOf(']');
  if(start>=0&&end>start)t=t.slice(start,end+1);
  const arr=JSON.parse(t);
  if(!Array.isArray(arr)||!arr.length)throw new Error('Empty');
  const cleaned=[];
  for(const item of arr){
    if(!item.q || !Array.isArray(item.opts) || item.opts.length<2)continue;
    let ansIdx=0;
    if(typeof item.ans==='number')ansIdx=Math.max(0,Math.min(item.opts.length-1,item.ans));
    else if(typeof item.ans==='string'){
      const letterMap={'A':0,'B':1,'C':2,'D':3};
      const up=item.ans.trim().toUpperCase();
      if(up in letterMap)ansIdx=letterMap[up];
      else{
        const found=item.opts.findIndex(o=>o.trim().toLowerCase()===item.ans.trim().toLowerCase());
        ansIdx=found>=0?found:0;
      }
    }
    const mk=normalizeMark(item.mark||item.tag);
    cleaned.push({
      id:crypto.randomUUID(),
      subject:item.subject||'Biology',
      chapter:item.chapter||'General Practice',
      q:String(item.q).trim(),
      opts:item.opts.map(o=>String(o)),
      ans:ansIdx,
      mark:mk,
      exp:String(item.exp||'').trim()
    });
  }
  if(!cleaned.length)throw new Error('No valid MCQs');
  return cleaned;
}
window.importBulkJson=()=>{
  const ta=document.getElementById('bulkJsonInput');
  if(!ta||!ta.value.trim()){toast('Paste JSON first.');return}
  try{
    const cleaned=parseQuizJsonText(ta.value);
    db.quizzes=db.quizzes||[];
    db.quizzes.push(...cleaned);
    save();
    const chapters=[...new Set(cleaned.map(x=>x.chapter))];
    beginQuizSession(cleaned,{subject:cleaned[0].subject||'Mixed',chapter:chapters.length===1?chapters[0]:'Mixed Chapters'});
    toast(`${cleaned.length} MCQs imported & quiz started!`);
  }catch(err){
    toast('Invalid JSON format. Check brackets and commas.');
  }
};
async function callGemini(prompt){
  const key=(db.settings.geminiKey||'').trim();
  if(!key)throw new Error('NO_KEY');
  if(!prompt)throw new Error('NO_PROMPT');
  const url='https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key='+encodeURIComponent(key);
  const res=await fetch(url,{
    method:'POST',
    headers:{'Content-Type':'application/json'},
    body:JSON.stringify({
      contents:[{parts:[{text:prompt}]}],
      generationConfig:{temperature:0.4,maxOutputTokens:8192}
    })
  });
  const data=await res.json();
  if(data?.error?.message)throw new Error(data.error.message);
  const text=data?.candidates?.[0]?.content?.parts?.[0]?.text;
  if(!text)throw new Error('Empty Gemini response');
  return text;
}
window.vaultMakeQuiz=async(id)=>{
  const v=(db.vault||[]).find(x=>x.id===id);
  if(!v){toast('Vault item missing.');return}
  const sub=(v.subject&&v.subject!=='Formula / Notes')?v.subject:'Physics';
  const chap=(v.chapter||v.title||'General').trim();
  const topic=`Material: "${v.title}"${v.category?' ('+v.category+')':''}. NCERT + NEET PYQ style from this chapter.`;
  const prompt=`Act as a NEET exam question maker for ${sub} (Chapter: "${chap}", Topic: "${topic}"). Create 12 NCERT-aligned multiple choice questions. Return ONLY a valid JSON array in this exact schema with no extra text:\n[{"subject":"${sub}","chapter":"${chap}","q":"Question text","opts":["Option A","Option B","Option C","Option D"],"ans":0,"mark":"Imp","exp":"Short clear explanation"}]`;
  const key=(db.settings.geminiKey||'').trim();
  if(!key){
    aiOutput=prompt;
    navigate('aitutor');
    setTimeout(()=>{
      const box=document.getElementById('aiPromptBox');
      if(box)box.textContent=prompt;
      navigator.clipboard?.writeText(prompt).catch(()=>{});
      toast('Key nahi mili — prompt copy ho gaya. AI Tutor mein Gemini key save karo ya GPT mein paste karo.');
    },80);
    return;
  }
  toast('Gemini se 12 MCQ ban rahe hain…');
  try{
    const text=await callGemini(prompt);
    const cleaned=parseQuizJsonText(text);
    db.quizzes=db.quizzes||[];
    db.quizzes.push(...cleaned);
    save();
    beginQuizSession(cleaned,{subject:sub,chapter:chap});
    toast(`${cleaned.length} MCQs Vault se ban ke quiz start!`);
  }catch(e){
    const msg=String(e.message||e);
    if(msg==='NO_KEY')toast('Gemini key AI Tutor mein save karo.');
    else{
      aiOutput=prompt+'\n\n--- Gemini error ---\n'+msg;
      navigate('aitutor');
      setTimeout(()=>{
        const box=document.getElementById('aiPromptBox');
        if(box)box.textContent=prompt;
        toast('Gemini fail: '+msg.slice(0,80)+' — prompt ready, paste GPT/Claude mein.');
      },80);
    }
  }
};

window.startSavedQuiz=(subFilter='All',chapFilter='All',markFilter='All')=>{
  const targetMark=markFilter==='All'?'All':normalizeMark(markFilter);
  const normSub=String(subFilter||'All').trim().toLowerCase();
  const normChap=String(chapFilter||'All').trim().toLowerCase();
  const pool=(db.quizzes||[]).filter(q=>{
    const qMark=normalizeMark(q.mark||q.tag);
    const qSub=String(q.subject||'').trim().toLowerCase();
    const qChap=String(q.chapter||'').trim().toLowerCase();
    return (normSub==='all'||qSub===normSub) &&
           (normChap==='all'||qChap===normChap) &&
           (targetMark==='All'||qMark===targetMark);
  });
  if(!pool.length){toast(`No ${targetMark==='All'?'saved':targetMark} MCQs found for this selection.`);return}
  const labelChap=chapFilter==='All'?(targetMark==='All'?'All Chapters':`${targetMark} Marked`):chapFilter;
  const key=quizKey(subFilter,chapFilter,markFilter);
  const prog=(db.quizProgress||{})[key];
  if(prog&&prog.ids&&prog.idx<prog.ids.length){
    const bank=db.quizzes||[];
    const rebuilt=prog.ids.map(id=>bank.find(q=>q.id===id)).filter(Boolean);
    if(rebuilt.length===prog.ids.length){
      activeQuiz=rebuilt.map(x=>({...x,mark:normalizeMark(x.mark||x.tag)}));
      activeQuizMeta=prog.meta||{subject:subFilter,chapter:labelChap};
      activeQuizKey=key;
      activeQuizArgs=[subFilter,chapFilter,markFilter];
      quizIdx=prog.idx;quizScore=prog.score||0;quizWrongList=prog.wrong||[];quizElapsedSec=prog.elapsed||0;
      quizAnswered=!!prog.answered;quizPickedIdx=quizAnswered?prog.picked:null;
      practiceTab='quiz';
      render();
      toast(`Resume: Q${quizIdx+1}/${activeQuiz.length}. Naya shuru karna ho to ↺ Restart dabao.`);
      return;
    }
    delete db.quizProgress[key];
  }
  beginQuizSession(pool,{subject:subFilter,chapter:labelChap},key,[subFilter,chapFilter,markFilter]);
};

window.retryAttemptWrongQuestions=(attemptId)=>{
  const att=(db.quizAttempts||[]).find(a=>a.id===attemptId);
  const wrong=att && att.wrongItems ? att.wrongItems : [];
  if(!wrong.length){toast('No wrong questions in this attempt!');return}
  const qBank=db.quizzes||[];
  const rebuilt=wrong.map(w=>{
    const matched=qBank.find(q=>q.q===w.q);
    if(matched)return {...matched};
    const opts=[w.picked,w.correct].filter(Boolean);
    if(opts.length<2)opts.push('Option A','Option B');
    const ansIdx=Math.max(0,opts.indexOf(w.correct));
    return {
      id:crypto.randomUUID(),
      subject:att.subject||'Biology',
      chapter:att.chapter||'Wrong Q Re-Test',
      q:w.q,
      opts,
      ans:ansIdx,
      mark:normalizeMark(w.mark||'Hard'),
      exp:w.exp||`True Answer: ${w.correct}`
    };
  });
  beginQuizSession(rebuilt,{subject:att.subject||'Mixed',chapter:`${att.chapter} (Wrong Qs)`});
  toast(`Starting ${rebuilt.length} missed question(s)!`);
};

function saveQuizAttemptIfDone(){
  clearInterval(quizClockInterval);
  if(!activeQuiz||!activeQuiz.length)return;
  const total=activeQuiz.length;
  const acc=Math.round(quizScore/total*100);
  const avgSec=Math.max(1,Math.round(quizElapsedSec/total));
  db.quizAttempts=db.quizAttempts||[];
  db.quizAttempts.push({
    id:crypto.randomUUID(),
    date:today(),
    subject:activeQuizMeta.subject||'Mixed',
    chapter:activeQuizMeta.chapter||'General Practice',
    score:quizScore,
    total,
    accuracy:acc,
    elapsedSec:quizElapsedSec,
    avgSec,
    marksSummary:activeQuiz.map(q=>({q:q.q,mark:normalizeMark(q.mark||q.tag)})),
    wrongItems:quizWrongList.slice()
  });
  if(activeQuizKey&&db.quizProgress)delete db.quizProgress[activeQuizKey];
  save();
  toast(`Attempt saved (${acc}% • ${avgSec}s/Q)!`);
}

window.answerQuizOpt=(choiceIdx)=>{
  if(quizAnswered||!activeQuiz||!activeQuiz[quizIdx])return;
  quizAnswered=true;
  quizPickedIdx=choiceIdx;
  const curQ=activeQuiz[quizIdx];
  const mk=normalizeMark(curQ.mark||curQ.tag);
  const isCorrect=choiceIdx===curQ.ans;
  const trueAnsText=curQ.opts[curQ.ans];
  if(isCorrect){
    quizScore++;
    if(skipDrill){drillNet+=4;drillForced+=4;drillRight++}
  }else{
    if(skipDrill){drillNet-=1;drillForced-=1;drillWrong++}
    quizWrongList.push({q:curQ.q,picked:curQ.opts[choiceIdx],correct:trueAnsText,mark:mk,exp:curQ.exp||''});
    const exists=db.mistakes.some(m=>!m.closed && m.error.includes(curQ.q.slice(0,45)));
    if(!exists){
      db.mistakes.push({
        id:crypto.randomUUID(),
        date:today(),
        subject:curQ.subject||'Biology',
        type:skipDrill?'Time management':'Concept gap',
        chapter:curQ.chapter||'Quiz Practice',
        error:`Wrong MCQ [${mk}]: ${curQ.q} (Picked: ${curQ.opts[choiceIdx]}) | True Answer: ${trueAnsText}`,
        fix:curQ.exp?`True Answer: ${trueAnsText}. ${curQ.exp}`:`True Answer: ${trueAnsText}.`
      });
      save();
      toast('Saved to Mistake Hunter with True Answer.');
    }
  }
  if(quizIdx+1>=activeQuiz.length)saveQuizAttemptIfDone();else saveQuizProgress();
  render();
};
window.skipQuizQuestion=()=>{
  if(!skipDrill||quizAnswered||!activeQuiz||!activeQuiz[quizIdx])return;
  quizAnswered=true;
  quizPickedIdx=-1;
  drillSkip++;
  // Forced-attempt baseline assumes a wrong guess (−1) when you wisely skip
  drillForced-=1;
  if(quizIdx+1>=activeQuiz.length)saveQuizAttemptIfDone();else saveQuizProgress();
  render();
};

window.nextQuizQuestion=()=>{
  quizIdx++;
  quizAnswered=false;
  quizPickedIdx=null;
  saveQuizProgress();
  render();
};

window.deleteAttempt=(id)=>{
  db.quizAttempts=(db.quizAttempts||[]).filter(a=>a.id!==id);
  save();
  render();
  toast('Attempt deleted.');
};

window.clearSavedQuizzes=()=>{
  if(!confirm('Delete all imported MCQs?'))return;
  db.quizzes=[];
  db.quizProgress={};
  activeQuiz=null;
  save();
  render();
  toast('Saved MCQs cleared.');
};

window.retestResult=(mistakeId,passed)=>{
  const m=db.mistakes.find(x=>x.id===mistakeId);
  if(m){
    m.fixStreak=m.fixStreak||0;
    if(passed){
      m.fixStreak++;
      if(m.fixStreak>=2){
        m.closed=true;
        m.closedAt=today();
        toast('2 clears in a row — mistake closed.');
      }else{
        toast('1 more clear needed before it closes (fix loop).');
      }
    }else{
      m.fixStreak=0;
      toast('Still open. Loop resets — try again after a short revision.');
    }
    save();
  }
  retestIdx++;
  render();
};

window.focusMistakeRetest=(id)=>{
  const open=db.mistakes.filter(x=>!x.closed);
  const i=open.findIndex(x=>x.id===id);
  if(i<0){toast('Mistake already closed or missing.');return}
  retestIdx=i;
  practiceTab='retest';
  navigate('practice');
  toast('Fix loop opened for this mistake.');
};

function practiceView(){
  title('Practice & Quiz');
  const qBank=db.quizzes||[];
  const attempts=db.quizAttempts||[];
  const openMistakes=db.mistakes.filter(m=>!m.closed);
  const safeRetestIdx=openMistakes.length?(retestIdx%openMistakes.length):0;
  const curRetest=openMistakes[safeRetestIdx];
  const attemptChapters=['All',...new Set(attempts.map(a=>a.chapter).filter(Boolean))];
  const filteredAttempts=(attemptChapterFilter==='All'?attempts:attempts.filter(a=>a.chapter===attemptChapterFilter)).slice().reverse();

  const countByMark=(arr,mk)=>arr.filter(q=>normalizeMark(q.mark||q.tag)===mk).length;
  const impCount=countByMark(qBank,'Imp');
  const easyCount=countByMark(qBank,'Easy');
  const medCount=countByMark(qBank,'Medium');
  const hardCount=countByMark(qBank,'Hard');

  const chapStatsMap={};
  attempts.forEach(a=>{
    const ch=a.chapter||'General Practice';
    if(!chapStatsMap[ch])chapStatsMap[ch]={chapter:ch,subject:a.subject||'Mixed',count:0,totalQ:0,rightQ:0};
    chapStatsMap[ch].count++;
    chapStatsMap[ch].totalQ+=(a.total||0);
    chapStatsMap[ch].rightQ+=(a.score||0);
  });
  const chapSummaryList=Object.values(chapStatsMap).map(x=>({...x,acc:x.totalQ?Math.round(x.rightQ/x.totalQ*100):0}));

  return `<div class="tabs">
    <button type="button" class="tab ${practiceTab==='quiz'?'active':''}" onclick="setPracticeTab('quiz')">Bulk JSON MCQ Quiz <b>${qBank.length}</b></button>
    <button type="button" class="tab ${practiceTab==='attempts'?'active':''}" onclick="setPracticeTab('attempts')">Saved Attempts <b>${attempts.length}</b></button>
    <button type="button" class="tab ${practiceTab==='retest'?'active':''}" onclick="setPracticeTab('retest')">Mistake Re-Test <b>${openMistakes.length}</b></button>
    <button type="button" class="tab ${practiceTab==='skipdrill'?'active':''}" onclick="setPracticeTab('skipdrill')">Skip Drill</button>
  </div>

  ${practiceTab==='skipdrill'&&!(activeQuiz&&skipDrill)?`
    <div class="card">
      <h3>Negative marking drill (+4 / −1 / skip 0)</h3>
      <p class="muted" style="font-size:12px;line-height:1.7;max-width:560px">NEET mein har galat jawab −1. 50/50 pe attempt karna aksar score khaata hai. Is drill mein Skip allowed hai — end pe net marks vs “forced attempt” baseline dikhega.</p>
      <div class="actions" style="margin-top:12px">
        <button type="button" class="btn" onclick="startSkipDrill(10)">Start 10 Q</button>
        <button type="button" class="btn secondary" onclick="startSkipDrill(15)">15 Q</button>
        <button type="button" class="btn secondary" onclick="startSkipDrill(20)">20 Q</button>
      </div>
      <p class="muted" style="font-size:11px;margin-top:10px">Uses your imported MCQ bank (${qBank.length} available).</p>
    </div>
  `:''}

  ${practiceTab==='quiz'||(practiceTab==='skipdrill'&&activeQuiz&&skipDrill)?`
    <div class="two-col">
      <div class="card">
        <div class="section-head" style="margin-top:0">
          <h3>${activeQuiz && quizIdx < activeQuiz.length ? (skipDrill?`Skip Drill (${quizIdx+1}/${activeQuiz.length}) • Net ${drillNet}`:`Active Quiz (${quizIdx+1}/${activeQuiz.length}) • Score: ${quizScore}`) : activeQuiz && quizIdx >= activeQuiz.length ? (skipDrill?`Drill done • Net ${drillNet} (R${drillRight}/W${drillWrong}/S${drillSkip})`:'Quiz Completed & Saved!') : selectedQuizSubject ? `${selectedQuizSubject} — Select Chapter` : 'MCQ Quiz Runner'}</h3>
          <div class="actions" style="align-items:center">
            ${activeQuiz && quizIdx < activeQuiz.length ? `<span class="tag gold" style="font-size:11px">⏱ <b id="quizLiveClock">${fmtMMSS(quizElapsedSec)}</b> • ${Math.max(1,Math.round(quizElapsedSec/Math.max(quizIdx+(quizAnswered?1:0),1)))}s/Q</span>` : ''}
            ${activeQuiz && quizIdx<activeQuiz.length && activeQuizKey ? `<button type="button" class="btn ghost" style="padding:5px 10px" onclick="restartActiveQuiz()">↺ Restart</button>` : ''}
            ${activeQuiz ? `<button type="button" class="btn secondary" style="padding:5px 10px" onclick="exitActiveQuiz()">← Back</button>` : selectedQuizSubject ? `<button type="button" class="btn ghost" style="padding:5px 10px" onclick="selectQuizSubject(null)">← Back</button>` : ''}
          </div>
        </div>

        ${activeQuiz && quizIdx < activeQuiz.length ? (()=>{
          const q=activeQuiz[quizIdx];
          const curMark=normalizeMark(q.mark||q.tag);
          return `<div>
            <div class="actions" style="justify-content:space-between;align-items:center;margin-bottom:8px;flex-wrap:wrap">
              <div class="actions" style="gap:6px;align-items:center">
                <span class="tag blue">${esc(q.subject)} • ${esc(q.chapter)}</span>
                <span class="tag" style="${markBadgeStyle(curMark)}">TAG: ${esc(curMark)}</span>
              </div>
              <div class="actions" style="gap:5px">
                ${MARKS.map(mk=>`<button type="button" class="btn ${curMark===mk?'':'ghost'}" style="padding:4px 8px;font-size:10px;${curMark===mk?markBadgeStyle(mk):''}" onclick="setQuestionMark('${mk}')">${mk}</button>`).join('')}
              </div>
            </div>
            <p style="font-size:13px;font-weight:700;line-height:1.6;margin:10px 0 12px">${esc(q.q)}</p>
            ${q.opts.map((opt,i)=>{
              let cls='opt-btn';
              let extraStyle='';
              if(quizAnswered && i===q.ans){
                cls+=' correct';
                extraStyle='background:#103827;border-color:#22c55e;color:#dcfce7';
              }else if(quizAnswered && i===quizPickedIdx && i!==q.ans){
                extraStyle='background:#4a1019;border-color:#ef4444;color:#fecaca';
              }
              return `<button type="button" class="${cls}" style="${extraStyle}" ${quizAnswered?'disabled':''} onclick="answerQuizOpt(${i})"><b>${String.fromCharCode(65+i)}.</b> <span>${esc(opt)}</span>${quizAnswered && i===q.ans ? ' <span style="margin-left:auto;font-size:10px;font-weight:800;color:#86efac">✓ TRUE ANSWER</span>' : quizAnswered && i===quizPickedIdx && i!==q.ans ? ' <span style="margin-left:auto;font-size:10px;font-weight:800;color:#fca5a5">✕ WRONG</span>' : ''}</button>`;
            }).join('')}
            ${!quizAnswered&&skipDrill?`<div class="actions" style="margin-top:10px"><button type="button" class="btn secondary" onclick="skipQuizQuestion()">⏭ Skip (0 marks)</button><span class="muted" style="font-size:11px">Net ${drillNet} • R${drillRight} W${drillWrong} S${drillSkip}</span></div>`:''}
            ${quizAnswered?`<div class="ai-reply-box" style="${quizPickedIdx!==q.ans&&quizPickedIdx!==-1?'border-color:#ef4444':''}"><b>${quizPickedIdx===-1?'Skipped. ':''}True Answer: ${String.fromCharCode(65+q.ans)}. ${esc(q.opts[q.ans])}</b> • Tag: <b>${esc(curMark)}</b>\n${esc(q.exp||'Review this concept before moving on.')}</div>
            <div class="actions" style="margin-top:12px"><button type="button" class="btn" onclick="nextQuizQuestion()">${quizIdx+1<activeQuiz.length?'Next Question →':'Finish & Save Attempt'}</button></div>`:''}
          </div>`;
        })() : activeQuiz && quizIdx >= activeQuiz.length ? (()=>{
          const lastAtt=attempts[attempts.length-1];
          const avgS=lastAtt&&lastAtt.avgSec?lastAtt.avgSec:Math.max(1,Math.round(quizElapsedSec/Math.max(activeQuiz.length,1)));
          return `<div class="card" style="background:#0c0c12;text-align:center;padding:24px">
            <div class="eyebrow">SAVED UNDER • ${esc(activeQuizMeta.chapter)}</div>
            <div style="font-size:36px;font-weight:800;margin:8px 0">${skipDrill?`${drillNet} net`:`${quizScore} / ${activeQuiz.length}`}</div>
            <div class="muted" style="font-size:12px">${skipDrill?`Right ${drillRight} • Wrong ${drillWrong} • Skip ${drillSkip} • Forced-guess baseline ~${drillForced} (assumes skips would be wrong)`:`Accuracy: ${Math.round(quizScore/activeQuiz.length*100)}%`} • Time: ${fmtMMSS(quizElapsedSec)} (${avgS}s/Q)</div>
            ${skipDrill&&drillNet>drillForced?`<p style="color:#86efac;font-size:12px;margin-top:8px">Skipping saved ~${drillNet-drillForced} marks vs guessing every doubtful Q wrong.</p>`:''}
            <div class="actions" style="justify-content:center;margin-top:16px">
              ${quizWrongList.length && lastAtt ? `<button type="button" class="btn" style="background:#8f1e2b" onclick="retryAttemptWrongQuestions('${lastAtt.id}')">↺ Retry ${quizWrongList.length} Wrong Qs</button>` : ''}
              <button type="button" class="btn secondary" onclick="exitActiveQuiz()">← Back to Quiz Runner</button>
              <button type="button" class="btn ghost" onclick="setPracticeTab('attempts')">Saved Attempts</button>
            </div>
          </div>`;
        })() : selectedQuizSubject ? (()=>{
          const chList=getChaptersForSubject(selectedQuizSubject);
          const subBank=qBank.filter(x=>String(x.subject||'').trim().toLowerCase()===selectedQuizSubject.toLowerCase());
          return `<div>
            <div class="muted" style="margin-bottom:10px">Chapter select karke quiz start karein ya ${esc(selectedQuizSubject)} ke Imp / Hard / Easy / Medium questions filter karein.</div>
            <div class="actions" style="margin-bottom:14px;flex-wrap:wrap">
              <button type="button" class="btn" onclick="startSavedQuiz('${selectedQuizSubject}','All','All')">All ${esc(selectedQuizSubject)} (${subBank.length})</button>
              <button type="button" class="btn secondary" style="${markBadgeStyle('Imp')}" onclick="startSavedQuiz('${selectedQuizSubject}','All','Imp')">Imp (${countByMark(subBank,'Imp')})</button>
              <button type="button" class="btn secondary" style="${markBadgeStyle('Hard')}" onclick="startSavedQuiz('${selectedQuizSubject}','All','Hard')">Hard (${countByMark(subBank,'Hard')})</button>
              <button type="button" class="btn secondary" style="${markBadgeStyle('Medium')}" onclick="startSavedQuiz('${selectedQuizSubject}','All','Medium')">Medium (${countByMark(subBank,'Medium')})</button>
              <button type="button" class="btn secondary" style="${markBadgeStyle('Easy')}" onclick="startSavedQuiz('${selectedQuizSubject}','All','Easy')">Easy (${countByMark(subBank,'Easy')})</button>
            </div>
            <div style="max-height:320px;overflow:auto">
              ${chList.map(ch=>{
                const cnt=subBank.filter(x=>String(x.chapter||'').trim().toLowerCase()===ch.trim().toLowerCase()).length;
                const pg=(db.quizProgress||{})[quizKey(selectedQuizSubject,ch,'All')];
                const hasPg=pg&&pg.ids&&pg.idx<pg.ids.length;
                return `<div class="list-item">
                  <span><b>${esc(ch)}</b> <span class="tag ${cnt?'green':''}">${cnt} Qs ready</span>${hasPg?` <span class="tag gold">Q${pg.idx+1}/${pg.ids.length} saved</span>`:''}</span>
                  <span style="display:flex;gap:6px;align-items:center">${hasPg?`<button type="button" class="btn ghost" style="padding:5px 9px" title="Reset progress" data-sub="${esc(selectedQuizSubject)}" data-chap="${esc(ch)}" onclick="resetChapterProgress(this)">↺</button>`:''}<button type="button" class="btn ${cnt?'secondary':'ghost'}" style="padding:5px 10px" data-sub="${esc(selectedQuizSubject)}" data-chap="${esc(ch)}" onclick="launchChapterBtn(this)">${hasPg?'▶ Resume':'Start Chapter'}</button></span>
                </div>`;
              }).join('')}
            </div>
          </div>`;
        })() : (()=>{
          const bankChapters=[...new Set(qBank.map(q=>q.chapter).filter(Boolean))];
          return `
            <div class="empty">Filter by Imp / Hard / Medium / Easy tags, select a subject first to open chapters, or start directly from saved chapters (${qBank.length} MCQs in bank).</div>
            <div class="actions" style="margin-top:8px;flex-wrap:wrap">
              <button type="button" class="btn secondary" onclick="startSavedQuiz('All','All','All')">Start All Saved (${qBank.length})</button>
              <button type="button" class="btn" style="${markBadgeStyle('Imp')}" onclick="startSavedQuiz('All','All','Imp')">Imp (${impCount})</button>
              <button type="button" class="btn" style="${markBadgeStyle('Hard')}" onclick="startSavedQuiz('All','All','Hard')">Hard (${hardCount})</button>
              <button type="button" class="btn" style="${markBadgeStyle('Medium')}" onclick="startSavedQuiz('All','All','Medium')">Medium (${medCount})</button>
              <button type="button" class="btn" style="${markBadgeStyle('Easy')}" onclick="startSavedQuiz('All','All','Easy')">Easy (${easyCount})</button>
            </div>
            <div class="actions" style="margin-top:10px;flex-wrap:wrap">
              <button type="button" class="btn" onclick="selectQuizSubject('Physics')">Physics Chapters →</button>
              <button type="button" class="btn" onclick="selectQuizSubject('Chemistry')">Chemistry Chapters →</button>
              <button type="button" class="btn" onclick="selectQuizSubject('Biology')">Biology Chapters →</button>
              ${qBank.length?`<button type="button" class="btn danger" onclick="clearSavedQuizzes()">Clear Bank</button>`:''}
            </div>
            ${bankChapters.length?`<div style="margin-top:14px"><div class="eyebrow" style="margin-bottom:6px">DIRECT CHAPTER SHORTCUTS</div><div class="actions" style="flex-wrap:wrap">${bankChapters.map(ch=>{
              const cQs=qBank.filter(x=>x.chapter===ch);
              const cSub=cQs[0]?.subject||'All';
              return `<button type="button" class="btn ghost" style="padding:5px 9px;font-size:10px" data-sub="${esc(cSub)}" data-chap="${esc(ch)}" onclick="launchChapterBtn(this)">${esc(ch)} (${cQs.length})</button>`;
            }).join('')}</div></div>`:''}
          `;
        })()}
      </div>

      <div class="card">
        <h3>Bulk JSON Import (No Manual Typing)</h3>
        <div class="field">
          <label>PASTE MCQ JSON ARRAY (TAG: "Imp" | "Easy" | "Medium" | "Hard")</label>
          <textarea id="bulkJsonInput" class="code-box" placeholder='[{"subject":"Biology","chapter":"Genetics","q":"...","opts":["A","B","C","D"],"ans":0,"mark":"Imp","exp":"..."}]'></textarea>
        </div>
        <div class="actions">
          <button type="button" class="btn" onclick="importBulkJson()">⚡ Import & Start Quiz</button>
          <button type="button" class="btn secondary" onclick="loadSampleJson()">Load Sample JSON</button>
        </div>
        <p class="muted" style="font-size:10px;line-height:1.6;margin-top:12px">Tip: PDF ya coaching sheet ko AI Tutor / Gemini / GPT mein dekar JSON banwayein, phir yahan paste karein.</p>
      </div>
    </div>
  ` : practiceTab==='attempts' ? `
    <div class="two-col">
      <div class="card">
        <div class="section-head" style="margin-top:0">
          <h3>Chapter-Wise Saved Attempts (${filteredAttempts.length})</h3>
          <select class="str-select" style="background:#14141d;color:#ddd" onchange="setAttemptChapterFilter(this.value)">
            ${attemptChapters.map(ch=>`<option value="${esc(ch)}" ${attemptChapterFilter===ch?'selected':''}>${ch==='All'?'All Chapters':ch}</option>`).join('')}
          </select>
        </div>
        ${filteredAttempts.length ? filteredAttempts.map(a=>{
          const markCounts={Imp:0,Easy:0,Medium:0,Hard:0};
          (a.marksSummary||[]).forEach(m=>{const nm=normalizeMark(m.mark||m.tag);if(markCounts[nm]!==undefined)markCounts[nm]++});
          const markBadges=MARKS.filter(k=>markCounts[k]>0).map(k=>`<span class="tag" style="${markBadgeStyle(k)}">${k}: ${markCounts[k]}</span>`).join(' ');
          const speedTag=a.avgSec?`<span class="tag blue">⏱ ${a.avgSec}s/Q</span>`:'';
          return `<div class="list-item" style="align-items:flex-start">
            <div>
              <b>${esc(a.chapter)}</b> <span class="tag blue">${esc(a.subject)}</span> <span class="tag green">${a.score}/${a.total} (${a.accuracy}%)</span> ${speedTag} ${markBadges}
              <div class="muted" style="margin-top:4px">Attempted on ${esc(a.date)}${a.elapsedSec?' • Total '+fmtMMSS(a.elapsedSec):''}</div>
              ${a.wrongItems && a.wrongItems.length ? `<div style="margin-top:6px;font-size:11px;color:#fca5a5">Missed ${a.wrongItems.length} Q(s): ${a.wrongItems.map(w=>`${esc(w.q.slice(0,48))} → <b>True: ${esc(w.correct)}</b>`).join(' • ')}</div>`:`<div style="margin-top:5px;font-size:11px;color:#86efac">100% accuracy on attempted questions!</div>`}
            </div>
            <div class="actions">
              ${a.wrongItems && a.wrongItems.length ? `<button type="button" class="btn danger" style="padding:5px 9px" onclick="retryAttemptWrongQuestions('${a.id}')">Retry Wrong (${a.wrongItems.length})</button>` : ''}
              ${a.chapter && a.chapter!=='Mixed Chapters' && qBank.some(q=>q.chapter===a.chapter)?`<button type="button" class="btn secondary" style="padding:5px 9px" data-sub="${esc(a.subject)}" data-chap="${esc(a.chapter)}" onclick="launchChapterBtn(this)">Re-Attempt</button>`:''}
              <button type="button" class="icon-btn" onclick="deleteAttempt('${a.id}')">×</button>
            </div>
          </div>`;
        }).join('') : '<div class="empty">Complete a quiz to save chapter-wise attempt history here.</div>'}
      </div>

      <div class="card">
        <h3>Chapter-Wise Accuracy Summary</h3>
        ${chapSummaryList.length ? chapSummaryList.map(cs=>`
          <div class="list-item" style="flex-direction:column;align-items:stretch;gap:6px">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>${esc(cs.chapter)}</b>
              <span class="tag ${cs.acc>=75?'green':'gold'}">${cs.acc}% • ${cs.count} attempt(s)</span>
            </div>
            <div class="bar"><span style="width:${cs.acc}%"></span></div>
          </div>
        `).join('') : '<div class="empty">Chapter accuracy stats appear once you complete quizzes.</div>'}
      </div>
    </div>
  ` : `
    <div class="two-col">
      <div class="card">
        <h3>Mistake Re-Test Queue (${openMistakes.length} Open)</h3>
        ${curRetest ? `
          <span class="tag">${esc(curRetest.subject)} • ${esc(curRetest.chapter)} • ${curRetest.date}</span>
          <div style="margin-top:10px;font-size:12px;color:#777682">ERROR LOGGED:</div>
          <p style="font-size:13px;font-weight:700;line-height:1.6;margin:4px 0 12px">${esc(curRetest.error)}</p>
          ${curRetest.fix?`<div class="ai-reply-box"><b>True Answer & Fix:</b>\n${esc(curRetest.fix)}</div>`:''}
          <p class="muted" style="font-size:11px;margin-top:8px">Fix loop: 2× “Solved” in a row closes it (streak now ${curRetest.fixStreak||0}/2). Still Tricky resets the streak.</p>
          <div class="actions" style="margin-top:14px">
            <button type="button" class="btn" style="background:#1b6e4d" onclick="retestResult('${curRetest.id}',true)">✓ Solved (${(curRetest.fixStreak||0)+1}/2)</button>
            <button type="button" class="btn secondary" onclick="retestResult('${curRetest.id}',false)">↺ Still Tricky — Next</button>
          </div>
        ` : '<div class="empty">No open mistakes in Re-Test queue. Great job!</div>'}
      </div>
      <div class="card">
        <h3>Open Mistakes List</h3>
        ${openMistakes.length?openMistakes.slice().reverse().map(m=>`<div class="list-item"><div><b>${esc(m.chapter)}</b><div class="muted">${esc(m.subject)} • ${esc(m.error.slice(0,75))}</div></div><div class="actions"><span class="tag">${esc(m.type)}</span><button type="button" class="btn secondary" style="padding:4px 8px;font-size:10px" onclick="focusMistakeRetest('${m.id}')">Fix</button></div></div>`).join(''):'<div class="empty">All logged mistakes are fixed.</div>'}
      </div>
    </div>
  `}
  `;
}

window.buildTutorPrompt=(mode)=>{
  const sub=document.getElementById('aiSub')?.value||'Biology';
  const chap=document.getElementById('aiChap')?.value.trim()||'NCERT Chapter';
  const topic=document.getElementById('aiTopic')?.value.trim()||'High-yield NEET concepts & PYQs';
  const count=document.getElementById('aiCount')?.value||'10';
  let prompt='';
  if(mode==='json_quiz'){
    prompt=`Act as a NEET exam question maker for ${sub} (Chapter: "${chap}", Topic: "${topic}"). Create ${count} NCERT-aligned multiple choice questions. Return ONLY a valid JSON array in this exact schema with no extra text:\n[{"subject":"${sub}","chapter":"${chap}","q":"Question text","opts":["Option A","Option B","Option C","Option D"],"ans":0,"mark":"Imp","exp":"Short clear explanation"}]`;
  }else if(mode==='doubt'){
    prompt=`Explain "${topic}" from ${sub} (${chap}) for a NEET aspirant. Keep it crisp, highlight NCERT traps or common formula mistakes, and give 2 quick check questions at the end.`;
  }else{
    const openErrs=db.mistakes.filter(m=>!m.closed).slice(-5).map(m=>`- [${m.subject} - ${m.chapter}] ${m.error}`).join('\n') || '- Focus on NCERT exceptions and numerical speed.';
    prompt=`Here are my recent NEET mistakes:\n${openErrs}\n\nCreate a focused practice plan and 8 MCQs targeting these exact weak points in JSON format:\n[{"subject":"${sub}","chapter":"${chap}","q":"Question","opts":["A","B","C","D"],"ans":0,"mark":"Hard","exp":"Why this is right"}]`;
  }
  const box=document.getElementById('aiPromptBox');
  if(box)box.textContent=prompt;
  return prompt;
};

window.copyTutorPrompt=()=>{
  const txt=document.getElementById('aiPromptBox')?.textContent||'';
  if(!txt)return;
  navigator.clipboard?.writeText(txt).then(()=>toast('Prompt copied! Paste into Gemini, ChatGPT, or Claude.')).catch(()=>toast('Select and copy prompt from box.'));
};

window.saveGeminiKey=()=>{
  const inp=document.getElementById('geminiKeyInput');
  if(!inp)return;
  db.settings.geminiKey=inp.value.trim();
  save();
  toast(db.settings.geminiKey?'Gemini API key saved locally.':'API key cleared.');
};

window.runGeminiDirect=async()=>{
  const prompt=document.getElementById('aiPromptBox')?.textContent||'';
  if(!(db.settings.geminiKey||'').trim()){toast('Add a Gemini API key first or use Copy Prompt.');return}
  if(!prompt)return;
  aiOutput='Thinking...';
  render();
  try{
    aiOutput=await callGemini(prompt);
  }catch(e){
    const msg=String(e.message||e);
    aiOutput=msg==='NO_KEY'
      ?'Add a Gemini API key (Google AI Studio → free key).'
      :('Gemini error: '+msg+'\n\nYou can still Copy Prompt into ChatGPT / Claude.');
  }
  render();
};
window.importAiOutputAsQuiz=()=>{
  try{
    const cleaned=parseQuizJsonText(aiOutput||'');
    db.quizzes=db.quizzes||[];
    db.quizzes.push(...cleaned);
    save();
    const chapters=[...new Set(cleaned.map(x=>x.chapter))];
    beginQuizSession(cleaned,{subject:cleaned[0].subject||'Mixed',chapter:chapters.length===1?chapters[0]:'Mixed Chapters'});
    toast(`${cleaned.length} MCQs from AI → quiz started`);
  }catch(err){
    toast('AI output mein valid JSON array nahi mila.');
  }
};

function aiTutorView(){
  title('AI Tutor');
  const defaultPrompt=`Act as a NEET exam question maker for Biology (Chapter: "Cell Cycle", Topic: "Meiosis"). Create 8 NCERT-aligned MCQs. Return ONLY a valid JSON array:\n[{"subject":"Biology","chapter":"Cell Cycle","q":"Question","opts":["A","B","C","D"],"ans":0,"mark":"Imp","exp":"Explanation"}]`;
  return `<div class="two-col">
    <div class="card">
      <h3>NEET Prompt & Quiz Builder</h3>
      <div class="form-grid">
        <div class="field"><label>SUBJECT</label><select id="aiSub"><option>Biology</option><option>Chemistry</option><option>Physics</option></select></div>
        <div class="field"><label>NO. OF MCQS</label><input id="aiCount" type="number" min="3" max="30" value="10"></div>
      </div>
      <div class="field"><label>CHAPTER</label><input id="aiChap" placeholder="e.g. Thermodynamics / Biomolecules"></div>
      <div class="field"><label>TOPIC / DOUBT / PDF CONTEXT</label><input id="aiTopic" placeholder="e.g. Exceptions, formulas, or paste topic name"></div>
      <div class="actions" style="margin-top:10px">
        <button type="button" class="btn" onclick="buildTutorPrompt('json_quiz')">Build Quiz JSON Prompt</button>
        <button type="button" class="btn secondary" onclick="buildTutorPrompt('doubt')">Build Doubt Prompt</button>
        <button type="button" class="btn ghost" onclick="buildTutorPrompt('mistake_fix')">Mistake-Based Quiz</button>
      </div>
      <div class="field" style="margin-top:14px">
        <label>GENERATED PROMPT (COPY TO GEMINI / GPT / CLAUDE OR RUN DIRECTLY)</label>
        <div id="aiPromptBox" class="prompt-preview">${esc(defaultPrompt)}</div>
      </div>
      <div class="actions">
        <button type="button" class="btn secondary" onclick="copyTutorPrompt()">📋 Copy Prompt</button>
        <button type="button" class="btn" onclick="runGeminiDirect()">✦ Ask Gemini</button>
        ${aiOutput?`<button type="button" class="btn" style="background:#1b6e4d" onclick="importAiOutputAsQuiz()">📥 Import AI → Quiz</button>`:''}
      </div>
      ${aiOutput?`<div class="ai-reply-box">${esc(aiOutput)}</div>`:''}
    </div>

    <div class="card">
      <h3>Gemini key (sirf is device pe)</h3>
      <div class="field">
        <label>GEMINI API KEY</label>
        <input type="password" id="geminiKeyInput" value="${esc(db.settings.geminiKey||'')}" placeholder="AIza... from Google AI Studio">
      </div>
      <button type="button" class="btn secondary" onclick="saveGeminiKey()">Save Key</button>
      <p class="muted" style="font-size:10px;line-height:1.65;margin-top:14px">
        Free key: <b>aistudio.google.com</b> → Get API key → yahan paste → Save.<br>
        Key kabhi code/GitHub mein share mat karna. Vault card pe <b>⚡ Quiz</b> ab isi key se MCQ bana sakta hai.<br>
        Bina key ke: Copy Prompt → ChatGPT/Gemini web → JSON → Practice paste, ya yahan Import AI → Quiz.
      </p>
    </div>
  </div>`;
}

window.refreshMistakeChapterOptions=(subVal)=>{
  const sel=document.getElementById('mistakeChapterSelect');
  const inp=document.getElementById('mistakeChapterInput');
  if(!sel)return;
  const list=getChaptersForSubject(subVal||'Physics');
  sel.innerHTML=list.map(ch=>`<option value="${esc(ch)}">${esc(ch)}</option>`).join('');
  if(inp && !inp.value.trim() && list.length)inp.value=list[0];
};

window.onMistakeChapterPick=(val)=>{
  const inp=document.getElementById('mistakeChapterInput');
  if(inp)inp.value=val;
};

window.toggleMistakeGroup=el=>{const k=el.dataset.key;if(el.open)mistakeOpenGroups.add(k);else mistakeOpenGroups.delete(k)};
function mistakeItemHTML(m){
  const isWrongMcq=!m.closed && String(m.error||'').includes('Wrong MCQ');
  return `<div class="list-item" style="align-items:flex-start;${isWrongMcq?'background:#2a0d14;border:1px solid #8f1e2b':''}">
          <div>
            <b>${esc(m.chapter)}</b> ${isWrongMcq?'<span class="tag" style="background:#5e141f;color:#fca5a5;border-color:#ef4444">WRONG MCQ</span>':''}
            <div class="muted" style="margin:4px 0">${esc(m.subject)} • ${esc(m.type)} • ${m.date}</div>
            <div style="${isWrongMcq?'color:#fecaca;font-weight:600':''}">${esc(m.error)}</div>
            ${m.fix?`<div style="color:#86efac;margin-top:5px"><b>True Answer / Fix:</b> ${esc(m.fix)}</div>`:''}
            <div class="actions" style="margin-top:8px">
              ${!m.closed?`<button type="button" class="btn" style="padding:6px 10px" onclick="focusMistakeRetest('${m.id}')">↺ Fix loop</button>`:''}
              <button type="button" class="btn ${m.closed?'ghost':'secondary'}" style="padding:6px 10px" onclick="toggleMistake('${m.id}')">${m.closed?'↺ Reopen':'✓ Mark fixed'}</button>
              ${m.fileId?`<button type="button" class="btn ghost" style="padding:6px 10px" onclick="previewFile('${m.fileId}')">📎 View ${m.fileType==='application/pdf'?'PDF':'Screenshot'}</button>`:''}
            </div>
          </div>
          <button type="button" class="icon-btn" onclick="deleteMistake('${m.id}')">×</button>
        </div>`;
}
function mistakeGroupsHTML(ml){
  const g={};
  ml.slice().reverse().forEach(m=>{
    const ch=String(m.chapter||'General').trim()||'General';
    const k=mistakeTab+'|'+(m.subject||'')+'|'+ch.toLowerCase();
    (g[k]=g[k]||{key:k,subject:m.subject||'',chapter:ch,list:[]}).list.push(m);
  });
  const arr=Object.values(g).sort((a,b)=>b.list.length-a.list.length||a.chapter.localeCompare(b.chapter));
  return `<div class="muted" style="font-size:10px;margin-bottom:10px">${ml.length} ${mistakeTab==='open'?'wrong':'fixed'} • ${arr.length} chapters (chapter pe tap karke kholo)</div>`
    +arr.map(x=>`<details class="group" data-key="${esc(x.key)}" ${mistakeOpenGroups.has(x.key)?'open':''} ontoggle="toggleMistakeGroup(this)"><summary><span><b>📂 ${esc(x.chapter)}</b> <span class="muted" style="font-size:10px">${esc(x.subject)}</span></span><span class="tag ${mistakeTab==='open'?'':'green'}">${x.list.length} ${mistakeTab==='open'?'wrong':'fixed'}</span></summary><div class="group-body">${x.list.map(mistakeItemHTML).join('')}</div></details>`).join('');
}

function mistakes(){
  title('Mistake Hunter');
  const mo=db.mistakes.filter(x=>!x.closed),mc=db.mistakes.filter(x=>x.closed),ml=mistakeTab==='open'?mo:mc;
  const initChapters=getChaptersForSubject('Physics');
  return `<div class="section-head"><h2>Turn errors into insight</h2><small>${db.mistakes.length} RECORDS</small></div>
  <div class="two-col">
    <div class="card">
      <h3>Record a mistake</h3>
      <form id="mistakeForm">
        <div class="form-grid">
          <div class="field"><label>SUBJECT</label><select name="subject" id="mistakeSubSelect" onchange="refreshMistakeChapterOptions(this.value)"><option>Physics</option><option>Chemistry</option><option>Biology</option></select></div>
          <div class="field"><label>ERROR TYPE</label><select name="type"><option>Concept gap</option><option>Calculation</option><option>Formula recall</option><option>Misread question</option><option>Memory / NCERT</option><option>Time management</option></select></div>
        </div>
        <div class="field">
          <label>SELECT CHAPTER (ALL SUBJECTS SUPPORTED)</label>
          <select id="mistakeChapterSelect" onchange="onMistakeChapterPick(this.value)">
            ${initChapters.map(ch=>`<option value="${esc(ch)}">${esc(ch)}</option>`).join('')}
          </select>
        </div>
        <div class="field"><label>CHAPTER / TOPIC (SELECTED OR CUSTOM)</label><input name="chapter" id="mistakeChapterInput" value="${esc(initChapters[0]||'')}" required placeholder="Chapter name"></div>
        <div class="field"><label>WHAT WENT WRONG?</label><textarea name="error" required placeholder="Describe the error briefly..."></textarea></div>
        <div class="field"><label>CORRECT APPROACH / TAKEAWAY</label><textarea name="fix" placeholder="What will you remember next time?"></textarea></div>
        <div class="field"><label>ATTACH QUESTION SCREENSHOT / PDF (OPTIONAL)</label><input type="file" name="attachment" accept="image/*,application/pdf"></div>
        <button type="submit" class="btn">＋ Save mistake</button>
      </form>
    </div>
    <div class="card">
      <h3>Learning log</h3>
      ${tabsHTML(mistakeTab,mo.length,mc.length,'setMistakeTab')}
      ${ml.length?mistakeGroupsHTML(ml):`<div class="empty">${mistakeTab==='open'?'No open mistakes. Add one after your next practice session.':'Nothing closed yet.'}</div>`}
    </div>
  </div>`;
}

window.deleteMistake=async(id)=>{
  const m=db.mistakes.find(x=>x.id===id);
  if(m&&m.fileId)await deleteStoredFile(m.fileId);
  db.mistakes=db.mistakes.filter(x=>x.id!==id);
  save();render();toast('Mistake removed.');
};

window.setVaultFilter=f=>{vaultFilter=f;vaultChapterFilter='All';render()};
window.setVaultChapterFilter=ch=>{vaultChapterFilter=ch;render()};
function chapterOptionsHTML(sub,selected=''){
  const list=(sub&&sub!=='Formula / Notes')?getChaptersForSubject(sub):[];
  return '<option value="">— General (no chapter) —</option>'+list.map(ch=>`<option value="${esc(ch)}" ${ch===selected?'selected':''}>${esc(ch)}</option>`).join('');
}
window.refreshVaultChapters=sub=>{const sel=document.getElementById('vaultChapterSelect');if(sel)sel.innerHTML=chapterOptionsHTML(sub)};
window.openVaultLink=id=>{
  const v=(db.vault||[]).find(x=>x.id===id);
  if(v&&/^https?:\/\//i.test(v.link||''))window.open(v.link,'_blank','noopener');
};
window.openVaultItem=id=>{
  const v=(db.vault||[]).find(x=>x.id===id);
  if(!v)return;
  if(v.fileId)previewFile(v.fileId);else openVaultLink(id);
};
window.deleteVaultItem=async(id)=>{
  const item=(db.vault||[]).find(x=>x.id===id);
  if(item&&item.fileId)await deleteStoredFile(item.fileId);
  db.vault=(db.vault||[]).filter(x=>x.id!==id);
  save();render();toast('File removed from Study Vault.');
};

function vaultCardHTML(v){
  return `
          <div class="vault-card">
            <div class="vault-thumb" onclick="openVaultItem('${v.id}')">
              <div class="pdf-badge">${!v.fileId?'🔗 ONLINE LINK (DRIVE)':(v.fileType==='application/pdf'?'📄 PDF DOCUMENT':'🖼 SCREENSHOT / IMAGE')}<div style="font-size:9px;color:#8f8d99;margin-top:4px;text-align:center">Tap to open</div></div>
            </div>
            <div>
              <b style="font-size:12px;display:block">${esc(v.title)}</b>
              <div class="muted" style="font-size:10px;margin-top:3px">${esc(v.subject)}${v.chapter?' • '+esc(v.chapter):''} • ${esc(v.category)}</div>
            </div>
            <div class="actions" style="margin-top:auto;flex-wrap:wrap">
              <button type="button" class="btn secondary" style="flex:1;padding:6px" onclick="openVaultItem('${v.id}')">Open</button>
              <button type="button" class="btn" style="flex:1;padding:6px" onclick="vaultMakeQuiz('${v.id}')">⚡ Quiz</button>
              ${v.fileId&&v.link?`<button type="button" class="icon-btn" style="padding:5px 9px" title="Open Drive link" onclick="openVaultLink('${v.id}')">🔗</button>`:''}
              <button type="button" class="icon-btn" style="padding:5px 9px" onclick="deleteVaultItem('${v.id}')">×</button>
            </div>
          </div>
        `;
}

function vault(){
  title('Study Vault');
  const all=db.vault||[];
  const subjects4=['Physics','Chemistry','Biology','Formula / Notes'];
  const hasChapters=vaultFilter!=='All'&&vaultFilter!=='Formula / Notes';
  const chapList=hasChapters?getChaptersForSubject(vaultFilter):[];
  const cnt=ch=>all.filter(v=>v.subject===vaultFilter&&(v.chapter||'')===ch).length;
  const items=all.filter(v=>(vaultFilter==='All'||v.subject===vaultFilter)&&(vaultChapterFilter==='All'||(v.chapter||'')===vaultChapterFilter));
  const formSubject=subjects4.includes(vaultFilter)?vaultFilter:'Physics';
  const formChapter=hasChapters&&vaultChapterFilter!=='All'?vaultChapterFilter:'';
  const groups={};
  items.slice().reverse().forEach(v=>{const k=(v.subject||'')+'||'+(v.chapter||'');(groups[k]=groups[k]||{subject:v.subject||'',chapter:v.chapter||'',list:[]}).list.push(v)});
  const gl=Object.values(groups).sort((a,b)=>subjects4.indexOf(a.subject)-subjects4.indexOf(b.subject)||a.chapter.localeCompare(b.chapter));
  return `<div class="section-head">
    <h2>NEET Material, Screenshots & PDFs</h2>
    <div class="actions">
      ${['All','Physics','Chemistry','Biology','Formula / Notes'].map(f=>`<button type="button" class="btn ${vaultFilter===f?'':'secondary'}" style="padding:6px 10px" onclick="setVaultFilter('${f}')">${f}</button>`).join('')}
    </div>
  </div>
  ${hasChapters?`<div class="chip-wrap">
    <button type="button" class="btn ${vaultChapterFilter==='All'?'':'secondary'}" style="padding:5px 9px;font-size:10px" data-ch="All" onclick="setVaultChapterFilter(this.dataset.ch)">All chapters</button>
    ${chapList.map(ch=>`<button type="button" class="btn ${vaultChapterFilter===ch?'':'secondary'}" style="padding:5px 9px;font-size:10px" data-ch="${esc(ch)}" onclick="setVaultChapterFilter(this.dataset.ch)">${esc(ch)}${cnt(ch)?` (${cnt(ch)})`:''}</button>`).join('')}
  </div>`:''}
  <div class="two-col">
    <div class="card">
      <h3>Upload Study Material</h3>
      <form id="vaultForm">
        <div class="field"><label>TITLE / TOPIC NAME</label><input name="title" required placeholder="e.g. Organic Named Reactions Mindmap"></div>
        <div class="form-grid">
          <div class="field"><label>SUBJECT</label><select name="subject" onchange="refreshVaultChapters(this.value)">${subjects4.map(x=>`<option ${x===formSubject?'selected':''}>${x}</option>`).join('')}</select></div>
          <div class="field"><label>CATEGORY</label><select name="category"><option>Screenshot / Diagram</option><option>Short Notes PDF</option><option>DPP / Question Bank</option><option>NCERT Highlight</option></select></div>
        </div>
        <div class="field"><label>CHAPTER</label><select name="chapter" id="vaultChapterSelect">${chapterOptionsHTML(formSubject,formChapter)}</select></div>
        <div class="field"><label>SELECT IMAGE OR PDF</label><input type="file" name="file" accept="image/*,application/pdf"></div>
        <div class="field"><label>OR DRIVE / ONLINE LINK (OPTIONAL)</label><input name="link" type="url" placeholder="https://drive.google.com/..."></div>
        <button type="submit" class="btn">⬆ Save to Study Vault</button>
      </form>
      <p class="muted" style="font-size:10px;line-height:1.6;margin-top:14px">Files are stored offline inside your browser's IndexedDB so large PDFs and screenshots load instantly without slowing down the app.</p>
    </div>
    <div class="card">
      <h3>Saved Files (${items.length})</h3>
      ${items.length?gl.map(g=>`<div class="vault-group-title">📘 ${vaultFilter==='All'?esc(g.subject)+' • ':''}${esc(g.chapter||'General')} (${g.list.length})</div><div class="vault-grid">${g.list.map(vaultCardHTML).join('')}</div>`).join(''):'<div class="empty">No screenshots or PDFs uploaded yet. Add your first formula sheet or question screenshot.</div>'}
    </div>
  </div>`;
}

function revisionScheduleDates(r){
  const base=r.date||today();
  const d3=r.r3||addDays(base,3);
  const d7=r.r7||addDays(base,7);
  const d21=r.r21||addDays(base,21);
  return {d3,d7,d21};
}

function dueRevisionsList(){
  const t=today();
  return db.revisions
    .map(r=>{
      const {d3,d7,d21}=revisionScheduleDates(r);
      const ms=r.milestones||{};
      const dueDates=[];
      if(d3<=t&&!ms.r3)dueDates.push({key:'r3',date:d3,label:'+3d'});
      if(d7<=t&&!ms.r7)dueDates.push({key:'r7',date:d7,label:'+7d'});
      if(d21<=t&&!ms.r21)dueDates.push({key:'r21',date:d21,label:'+21d'});
      return {...r,d3,d7,d21,dueDates};
    })
    .filter(r=>r.dueDates.length>0)
    .sort((a,b)=>a.dueDates[0].date.localeCompare(b.dueDates[0].date));
}

window.markRevisionDone=id=>{
  const r=db.revisions.find(x=>x.id===id);
  if(!r)return;
  const t=today();
  const {d3,d7,d21}=revisionScheduleDates(r);
  r.milestones=r.milestones||{};
  if(d3<=t&&!r.milestones.r3)r.milestones.r3=t;
  else if(d7<=t&&!r.milestones.r7)r.milestones.r7=t;
  else if(d21<=t&&!r.milestones.r21)r.milestones.r21=t;
  else{toast('No pending milestone for this entry.');return}
  r.lastRevised=t;
  save();
  render();
  toast('Revision milestone marked. Next spaced due will show when ready.');
};

function revision(){
  title('Revision Log');
  const dueList=dueRevisionsList();
  return `<div class="section-head"><h2>Revision Tracker</h2><small>SPACED REVISION RECORD (+3 / +7 / +21 DAYS)</small></div>
  ${dueList.length?`<div class="card" style="margin-bottom:14px;border-color:#a36d10">
    <div class="section-head" style="margin-top:0"><h3>Due for Revision (${dueList.length})</h3><span class="tag gold">SPACED DUE</span></div>
    <p class="muted" style="font-size:11px;margin-bottom:8px">Daily alert bhi aayega jab spaced revision due ho. Tap “Mark revised” after you finish that slot.</p>
    ${dueList.map(r=>`<div class="list-item" style="align-items:flex-start"><div><b>${esc(r.chapter)}</b> <span class="tag blue">${esc(r.subject)}</span><div class="muted" style="margin-top:3px">Logged ${shortDate(r.date)} • Pending: ${r.dueDates.map(d=>d.label+' ('+shortDate(d.date)+')').join(', ')}</div></div><button type="button" class="btn" style="padding:6px 10px" onclick="markRevisionDone('${r.id}')">✓ Mark revised</button></div>`).join('')}
  </div>`:''}
  <div class="two-col"><div class="card"><h3>Log a revision session</h3><form id="revisionForm"><div class="field"><label>CHAPTER / TOPIC</label><input name="chapter" required placeholder="e.g. Human Reproduction"></div><div class="form-grid"><div class="field"><label>SUBJECT</label><select name="subject"><option>Biology</option><option>Chemistry</option><option>Physics</option></select></div><div class="field"><label>REVISION TYPE</label><select name="kind"><option>Short notes</option><option>NCERT</option><option>Active recall</option><option>Questions / PYQs</option><option>Full chapter</option></select></div></div><button class="btn">Save revision</button></form></div><div class="card"><h3>Revision history</h3>${allRevs().length?allRevs().slice(0,60).map(r=>{const {d3,d7,d21}=revisionScheduleDates(r);const ms=(db.revisions.find(x=>x.chapter===r.chapter&&x.date===r.date)||{}).milestones||{};const isDue=((d3<=today()&&!ms.r3)||(d7<=today()&&!ms.r7)||(d21<=today()&&!ms.r21));return `<div class="list-item"><div><b>${esc(r.chapter)}</b> ${isDue?'<span class="tag gold">DUE</span>':''}<div class="muted">${esc(r.subject)} • ${esc(r.kind)} • Next: +3d (${shortDate(d3)}${ms.r3?' ✓':''}), +7d (${shortDate(d7)}${ms.r7?' ✓':''}), +21d (${shortDate(d21)}${ms.r21?' ✓':''})</div></div><span>${shortDate(r.date)}</span></div>`}).join(''):'<div class="empty">Your revision history will appear here.</div>'}</div></div></div>`;
}

window.autoCalcMockScore=()=>{
  const f=document.getElementById('testForm');
  if(!f)return;
  const p=Number(f.physics?.value||0);
  const c=Number(f.chemistry?.value||0);
  const b=Number(f.biology?.value||0);
  if(p>0 || c>0 || b>0){
    f.score.value=Math.min(720,p+c+b);
  }
};

function daysUntil(iso){
  const [y,m,d]=String(iso).split('-').map(Number);
  const t=new Date(y,(m||1)-1,d||1).getTime();
  const n=new Date();
  const t0=new Date(n.getFullYear(),n.getMonth(),n.getDate()).getTime();
  return Math.round((t-t0)/86400000);
}
function upcomingTestsHTML(){
  const list=(db.upcomingTests||[]).slice().sort((a,b)=>(a.date+(a.time||'')).localeCompare(b.date+(b.time||'')));
  const chip=n=>n<0?`<span class="tag">Passed ${-n}d ago</span>`:n===0?'<span class="tag">TODAY</span>':n===1?'<span class="tag gold">Tomorrow</span>':`<span class="tag blue">in ${n} days</span>`;
  return `<div class="section-head" style="margin-top:6px"><h2>Upcoming tests</h2><small>DATE • SYLLABUS</small></div>
  <div class="two-col" style="margin-bottom:22px">
    <div class="card">
      <h3>Add upcoming test</h3>
      <form id="upcomingForm">
        <div class="field"><label>TEST NAME</label><input name="name" required placeholder="e.g. Allen Part Test 03"></div>
        <div class="form-grid">
          <div class="field"><label>TEST DATE</label><input name="date" type="date" required></div>
          <div class="field"><label>TIME (OPTIONAL)</label><input name="time" type="time"></div>
        </div>
        <div class="field"><label>SYLLABUS / TOPICS (TEXT)</label><textarea name="syllabus" placeholder="e.g. Physics: Kinematics, Laws of Motion | Chem: Mole concept | Bio: Cell cycle"></textarea></div>
        <div class="field"><label>SYLLABUS FILE (IMAGE OR PDF, OPTIONAL)</label><input type="file" name="file" accept="image/*,application/pdf"></div>
        <button type="submit" class="btn">＋ Add upcoming test</button>
      </form>
    </div>
    <div class="card">
      <h3>Scheduled (${list.length})</h3>
      ${list.length?list.map(t=>`<div class="list-item" style="flex-direction:column;align-items:stretch;gap:8px">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:8px">
          <div><b style="font-size:12px">${esc(t.name)}</b><div class="muted" style="font-size:10px;margin-top:3px">${dateText(t.date)}${t.time?' • '+esc(t.time):''}</div></div>
          ${chip(daysUntil(t.date))}
        </div>
        ${t.syllabus?`<div class="muted" style="font-size:11px;line-height:1.6;white-space:pre-wrap">${esc(t.syllabus)}</div>`:''}
        <div class="actions">
          ${t.fileId?`<button type="button" class="btn secondary" style="padding:6px 10px" onclick="previewFile('${t.fileId}')">📎 Open syllabus file</button>`:''}
          <button type="button" class="icon-btn" style="padding:5px 9px" onclick="deleteUpcomingTest('${t.id}')">×</button>
        </div>
      </div>`).join(''):'<div class="empty">No upcoming test added yet. Date aur syllabus daal do.</div>'}
    </div>
  </div>`;
}
window.deleteUpcomingTest=async id=>{
  const t=(db.upcomingTests||[]).find(x=>x.id===id);
  if(t&&t.fileId)await deleteStoredFile(t.fileId);
  db.upcomingTests=(db.upcomingTests||[]).filter(x=>x.id!==id);
  save();render();toast('Upcoming test removed.');
};

window.toggleTestAnalysis=id=>{const t=db.tests.find(x=>x.id===id);if(t){t.analysisDone=!t.analysisDone;save();render()}};
window.removeTest=async id=>{
  const t=db.tests.find(x=>x.id===id);
  if(t&&t.fileId)await deleteStoredFile(t.fileId);
  db.tests=db.tests.filter(x=>x.id!==id);
  save();render();toast('Record removed.');
};

function tests(){
  title('Mock Tests');
  let avg=db.tests.length?Math.round(db.tests.reduce((a,t)=>a+t.score,0)/db.tests.length):0;
  return `<div class="stats">${stat('TESTS TAKEN',db.tests.length,'Total recorded')}${stat('AVERAGE SCORE',avg,'Out of 720')}${stat('BEST SCORE',db.tests.length?Math.max(...db.tests.map(t=>t.score)):0,'Out of 720')}${stat('LATEST ACCURACY',db.tests.length?db.tests[db.tests.length-1].accuracy+'%':'—','As entered')}</div>${upcomingTestsHTML()}<div class="two-col"><div class="card"><h3>Record mock test</h3><form id="testForm" oninput="autoCalcMockScore()"><div class="field"><label>TEST NAME</label><input name="name" placeholder="e.g. Full syllabus test 04" required></div><div class="form-grid"><div class="field"><label>SCORE ( / 720 )</label><input name="score" type="number" min="0" max="720" required></div><div class="field"><label>ACCURACY (%)</label><input name="accuracy" type="number" min="0" max="100" placeholder="e.g. 72"></div></div><div class="form-grid"><div class="field"><label>PHYSICS</label><input name="physics" type="number" min="0" max="180" placeholder="Score"></div><div class="field"><label>CHEMISTRY</label><input name="chemistry" type="number" min="0" max="180" placeholder="Score"></div></div><div class="field"><label>BIOLOGY</label><input name="biology" type="number" min="0" max="360" placeholder="Score"></div><div class="field"><label>ATTACH TEST PAPER / ANALYSIS (IMAGE OR PDF, OPTIONAL)</label><input type="file" name="file" accept="image/*,application/pdf"></div><label style="display:flex;align-items:center;gap:8px;font-size:12px;margin:2px 0 14px"><input type="checkbox" name="analysisDone" style="width:auto"> Analysis done ✅</label><button class="btn">Save test</button></form></div><div class="card"><h3>Test history</h3>${db.tests.length?`<div class="table-wrap"><table><thead><tr><th>TEST</th><th>SCORE</th><th>P / C / B</th><th>ACCURACY</th><th>DATE</th><th>ANALYSIS</th><th></th></tr></thead><tbody>${db.tests.slice().reverse().map(t=>{const pcb=(t.physics||t.chemistry||t.biology)?`${t.physics||0} / ${t.chemistry||0} / ${t.biology||0}`:'—';return `<tr><td>${esc(t.name)}</td><td><b>${t.score}</b></td><td><span class="muted">${pcb}</span></td><td>${t.accuracy||'—'}%</td><td>${t.date}</td><td style="white-space:nowrap"><button type="button" class="tag ${t.analysisDone?'green':''}" style="cursor:pointer;${t.analysisDone?'':'background:#4a1019;color:#fca5a5;border-color:#ef4444'}" onclick="toggleTestAnalysis('${t.id}')">${t.analysisDone?'✅ Done':'● Pending'}</button>${t.fileId?` <button type="button" class="icon-btn" style="padding:3px 7px" title="Open file" onclick="previewFile('${t.fileId}')">📎</button>`:''}</td><td><button class="icon-btn" onclick="removeTest('${t.id}')">×</button></td></tr>`}).join('')}</tbody></table></div>${db.tests.slice().reverse().slice(0,3).map(t=>`<div style="margin-top:12px;padding-top:10px;border-top:1px solid var(--line)"><b style="font-size:11px">${esc(t.name)}</b><p class="muted" style="font-size:10px">${t.analysis?esc(t.analysis):(t.analysisDone?'Analysis done.':'Analysis pending.')}</p></div>`).join('')}`:'<div class="empty">No mock tests recorded yet.</div>'}</div></div>`;
}
function dayKeys(n){return Array.from({length:n},(_,i)=>{const d=new Date();d.setDate(d.getDate()-(n-1-i));return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0')})}
function dayLabel(k){return new Date(k+'T12:00:00').toLocaleDateString('en-IN',{weekday:'short'})}
function barChart(labels,vals,id){const W=420,H=170,pt=20,pb=26,n=vals.length,max=Math.max(...vals,1),bw=W/n;let o=`<svg viewBox="0 0 ${W} ${H}" width="100%" role="img"><defs><linearGradient id="${id}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#f04a5b"/><stop offset="1" stop-color="#7c1d2a"/></linearGradient></defs><line x1="0" x2="${W}" y1="${H-pb}" y2="${H-pb}" stroke="#272731"/>`;vals.forEach((v,i)=>{const h=(H-pt-pb)*v/max,x=i*bw+bw*.18,y=H-pb-h;o+=`<rect x="${x}" y="${y}" width="${bw*.64}" height="${v?Math.max(h,3):0}" rx="4" fill="url(#${id})"/>`;if(v)o+=`<text x="${x+bw*.32}" y="${y-5}" text-anchor="middle" class="cv">${v}</text>`;o+=`<text x="${x+bw*.32}" y="${H-9}" text-anchor="middle" class="cl">${esc(labels[i])}</text>`});return o+'</svg>'}
function lineChart(labels,vals,max){const W=420,H=190,pl=30,pr=12,pt=20,pb=26,n=vals.length,step=n>1?(W-pl-pr)/(n-1):0,X=i=>n>1?pl+i*step:W/2,Y=v=>pt+(H-pt-pb)*(1-v/max);let o=`<svg viewBox="0 0 ${W} ${H}" width="100%" role="img">`;[0,.25,.5,.75,1].forEach(f=>{const y=Y(max*f);o+=`<line x1="${pl}" x2="${W-pr}" y1="${y}" y2="${y}" stroke="#272731"/><text x="${pl-5}" y="${y+3}" text-anchor="end" class="cl">${Math.round(max*f)}</text>`});if(n>1)o+=`<polyline fill="none" stroke="#e33b4b" stroke-width="2.2" stroke-linejoin="round" points="${vals.map((v,i)=>X(i)+','+Y(v)).join(' ')}"/>`;vals.forEach((v,i)=>{o+=`<circle cx="${X(i)}" cy="${Y(v)}" r="4" fill="#09090d" stroke="#f04a5b" stroke-width="2"/><text x="${X(i)}" y="${Y(v)-9}" text-anchor="middle" class="cv">${v}</text><text x="${X(i)}" y="${H-9}" text-anchor="middle" class="cl">${esc(labels[i])}</text>`});return o+'</svg>'}
function hbars(items,unit=''){const max=Math.max(...items.map(x=>x.value),1);return items.map(x=>`<div class="progress-row"><div class="progress-meta"><span>${esc(x.label)}</span><span>${x.value}${unit}</span></div><div class="bar"><span style="width:${x.value/max*100}%"></span></div></div>`).join('')}
function dayFocusMins(dateKey){
  return db.sessions.filter(x=>x.date===dateKey).reduce((a,x)=>a+(x.minutes||0),0);
}
function dayTasksProgress(dateKey){
  const dayTasks=db.tasks.filter(t=>t.date===dateKey);
  if(!dayTasks.length)return{total:0,done:0,pct:0};
  const done=dayTasks.filter(t=>t.done).length;
  return{total:dayTasks.length,done,pct:done/dayTasks.length};
}
/** A day counts toward streak if focus ≥ 6h (360 min) OR ≥60% of that day's missions are done. */
function dayQualifies(dateKey){
  if(dayFocusMins(dateKey)>=360)return true;
  const p=dayTasksProgress(dateKey);
  return p.total>0 && p.pct>=0.6;
}
function streak(){
  let d=new Date(),c=0;
  const key=()=>d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');
  if(!dayQualifies(key()))d.setDate(d.getDate()-1);
  while(dayQualifies(key())){c++;d.setDate(d.getDate()-1)}
  return c;
}
function streakLabel(){
  return `D-${String(streak()).padStart(2,'0')}🔥`;
}
function updateStreakBadge(){
  const el=document.getElementById('streakBadge');
  if(el)el.textContent=streakLabel();
}

function analytics(){
  title('Analytics');
  const k7=dayKeys(7),lab=k7.map(dayLabel),focus7=k7.map(k=>db.sessions.filter(x=>x.date===k).reduce((a,x)=>a+x.minutes,0)),closed7=k7.map(k=>db.tasks.filter(t=>t.done&&(t.doneAt||t.date)===k).length);
  const totMin=db.sessions.reduce((a,x)=>a+x.minutes,0),totQ=db.sessions.reduce((a,x)=>a+(x.questions||0),0),closedAll=db.tasks.filter(t=>t.done).length,best=db.tests.length?Math.max(...db.tests.map(t=>t.score)):0;
  const last=db.tests.slice(-10),subs=['Physics','Chemistry','Biology'],subMax={Physics:180,Chemistry:180,Biology:360};
  const subAvg=subs.map(s=>{const arr=db.tests.filter(t=>t[s.toLowerCase()]>0);return{label:s,value:arr.length?Math.round(arr.reduce((a,t)=>a+t[s.toLowerCase()]/subMax[s]*100,0)/arr.length):0}});
  const mSub=subs.map(s=>({label:s,value:db.mistakes.filter(m=>m.subject===s).length}));
  const types=[...new Set(db.mistakes.map(m=>m.type))].map(t=>({label:t,value:db.mistakes.filter(m=>m.type===t).length})).sort((a,b)=>b.value-a.value);
  const fSub=['Physics','Chemistry','Biology'].map(s=>({label:s,value:db.sessions.filter(x=>x.subject===s).reduce((a,x)=>a+x.minutes,0)}));
  const sk=streak();
  return `<div class="stats">${stat('TOTAL FOCUS',`${Math.floor(totMin/60)}h ${totMin%60}m`,`${totQ} total MCQs logged`)}${stat('MISSIONS CLOSED',closedAll,'All time')}${stat('STREAK',`${sk} day${sk===1?'':'s'} 🔥`,'6h focus or 60% missions')}${stat('BEST SCORE',best,'Out of 720')}</div>
  <div class="grid2"><div class="card"><h3>Focus minutes — last 7 days</h3>${barChart(lab,focus7,'bar-f2')}</div><div class="card"><h3>Missions closed — last 7 days</h3>${barChart(lab,closed7,'bar-m')}</div></div>
  <div class="grid2" style="margin-top:14px"><div class="card"><h3>Mock test score trend</h3>${last.length?lineChart(last.map((t,i)=>'T'+(db.tests.length-last.length+i+1)),last.map(t=>t.score),720):'<div class="empty">Record a mock test to see your trend.</div>'}</div><div class="card"><h3>Subject-wise mock average (%)</h3>${db.tests.length?hbars(subAvg,'%'):'<div class="empty">Add subject scores in a mock test to see this.</div>'}<h3 style="margin-top:22px">Focus time by subject (min)</h3>${totMin?hbars(fSub):'<div class="empty">Log focus sessions to see this.</div>'}</div></div>
  <div class="grid2" style="margin-top:14px"><div class="card"><h3>Mistakes by subject</h3>${db.mistakes.length?hbars(mSub):'<div class="empty">No mistakes recorded yet.</div>'}</div><div class="card"><h3>Mistakes by type</h3>${types.length?hbars(types):'<div class="empty">No mistakes recorded yet.</div>'}<p class="muted" style="font-size:10px;margin-top:14px">Open: ${db.mistakes.filter(m=>!m.closed).length} • Fixed: ${db.mistakes.filter(m=>m.closed).length}</p></div></div>
  <div class="grid2" style="margin-top:14px"><div class="card"><h3>Chapters done (%)</h3>${hbars(TRACK_ORDER.map(k=>({label:TRACK[k].label,value:covg(k,'done')})),'%')}</div><div class="card"><h3>Revision 1 completed (%)</h3>${hbars(TRACK_ORDER.map(k=>({label:TRACK[k].label,value:covg(k,'r1')})),'%')}</div></div>`;
}

const NEET_SYLLABUS={
  Physics:['Physics and Measurement','Kinematics','Laws of Motion','Work, Energy and Power','Rotational Motion','Gravitation','Properties of Solids and Liquids','Thermodynamics','Kinetic Theory of Gases','Oscillations and Waves','Electrostatics','Current Electricity','Magnetic Effects of Current and Magnetism','Electromagnetic Induction and Alternating Currents','Electromagnetic Waves','Optics','Dual Nature of Matter and Radiation','Atoms and Nuclei','Electronic Devices','Experimental Skills'],
  Chemistry:['Some Basic Concepts in Chemistry','Atomic Structure','Chemical Bonding and Molecular Structure','Chemical Thermodynamics','Solutions','Equilibrium','Redox Reactions and Electrochemistry','Chemical Kinetics','Classification of Elements and Periodicity in Properties','p-Block Elements','d- and f-Block Elements','Coordination Compounds','Purification and Characterisation of Organic Compounds','Some Basic Principles of Organic Chemistry','Hydrocarbons','Organic Compounds Containing Halogens','Organic Compounds Containing Oxygen','Organic Compounds Containing Nitrogen','Biomolecules','Principles Related to Practical Chemistry'],
  Biology:['The Living World','Biological Classification','Plant Kingdom','Animal Kingdom','Morphology of Flowering Plants','Anatomy of Flowering Plants','Floral Families (Malvaceae, Cruciferae, Leguminosae, Compositae, Gramineae)','Animal Tissues','Frog (Morphology and Anatomy)','Cell: The Unit of Life','Biomolecules and Enzymes','Cell Cycle and Cell Division','Photosynthesis in Higher Plants','Respiration in Plants','Plant Growth and Development','Breathing and Exchange of Gases','Body Fluids and Circulation','Excretory Products and Their Elimination','Locomotion and Movement','Neural Control and Coordination','Chemical Coordination and Integration','Sexual Reproduction in Flowering Plants','Human Reproduction','Reproductive Health','Principles of Inheritance and Variation','Molecular Basis of Inheritance','Evolution','Human Health and Disease','Microbes in Human Welfare','Biotechnology: Principles and Processes','Biotechnology and Its Applications','Organisms and Populations','Ecosystem','Biodiversity and Conservation']
};
function loadSyllabus(silent){let added=0;for(const sub of Object.keys(NEET_SYLLABUS)){const have=new Set((db.chapters[sub]||[]).map(c=>c.name.trim().toLowerCase()));db.chapters[sub]=db.chapters[sub]||[];for(const [i,name] of NEET_SYLLABUS[sub].entries())if(!have.has(name.toLowerCase())){db.chapters[sub].push({id:crypto.randomUUID(),name,done:false,strength:'Medium',weakTopics:'',...(sub==='Chemistry'?{branch:i<8?'Physical':i<12?'Inorganic':'Organic'}:{})});added++}}db.syllabusLoaded=true;save();if(!silent){render();toast(added?`${added} chapters added.`:'All syllabus chapters already added.')}return added}
window.loadSyllabus=loadSyllabus;

const TRACK_ORDER=['Physics','Physical','Inorganic','Organic','Biology'];
const CHEM_COLS=[['module','Module'],['dpp','DPP'],['pyqj','JEE Mains PYQ'],['pyqn','NEET PYQ'],['air','AIR Assignment']];
const TRACK={
  Physics:{label:'Physics',sub:'Physics',cols:[['module','Module'],['dpp','DPP'],['pyqj','PYQ JEE'],['pyqn','PYQ NEET']],rev:4},
  Physical:{label:'Physical Chem',sub:'Chemistry',branch:'Physical',cols:CHEM_COLS,rev:4},
  Inorganic:{label:'Inorganic Chem',sub:'Chemistry',branch:'Inorganic',cols:CHEM_COLS,rev:4},
  Organic:{label:'Organic Chem',sub:'Chemistry',branch:'Organic',cols:CHEM_COLS,rev:4},
  Biology:{label:'Biology',sub:'Biology',cols:[['punch','NCERT Punch 👊'],['module','Module'],['ncert','NCERT Line-by-Line']],rev:5}
};

function keepScroll(fn){const w=document.querySelector('.track-wrap'),x=w?w.scrollLeft:0,y=window.scrollY;fn();const n=document.querySelector('.track-wrap');if(n)n.scrollLeft=x;window.scrollTo(0,y)}
window.setTrackTab=k=>{trackTab=k;render()};
window.toggleCell=(id,key)=>{const cfg=TRACK[trackTab],c=(db.chapters[cfg.sub]||[]).find(x=>x.id===id);if(!c)return;if(key==='done')c.done=!c.done;else{c.t=c.t||{};if(c.t[key])delete c.t[key];else c.t[key]=today()}save();keepScroll(render)};
window.addTrackChapter=e=>{e.preventDefault();const cfg=TRACK[trackTab],name=e.target.chapter.value.trim();if(!name)return;const ch={id:crypto.randomUUID(),name,done:false,strength:'Medium',weakTopics:''};if(cfg.branch)ch.branch=cfg.branch;db.chapters[cfg.sub].push(ch);save();keepScroll(render);toast('Chapter added.')};
function allRevs(){const out=db.revisions.map(r=>({date:r.date,chapter:r.chapter,subject:r.subject,kind:r.kind,r3:r.r3,r7:r.r7,r21:r.r21}));for(const sub of Object.keys(db.chapters))for(const c of db.chapters[sub])for(const [k,d] of Object.entries(c.t||{}))if(/^r\d$/.test(k))out.push({date:d,chapter:c.name,subject:sub,kind:'Revision '+k.slice(1)});return out.sort((a,b)=>b.date.localeCompare(a.date))}
function covg(k,key){const l=tabChapters(TRACK[k]);return l.length?Math.round(l.filter(c=>key==='done'?c.done:(c.t||{})[key]).length/l.length*100):0}

const LAB_KEYS=[['physics','Physics',45,180],['chemistry','Chemistry',45,180],['botany','Botany',45,180],['zoology','Zoology',45,180]];
function labList(){return (db.mockLabs||[]).slice().sort((a,b)=>a.date.localeCompare(b.date)||a.name.localeCompare(b.name))}
function labPart(a){return (a?.c||0)*4-(a?.w||0)}
function labTotals(m){
  let correct=0,wrong=0,score=0;
  for(const [k] of LAB_KEYS){correct+=m[k]?.c||0;wrong+=m[k]?.w||0;score+=labPart(m[k])}
  const attempted=correct+wrong, skipped=180-attempted;
  return {correct,wrong,attempted,skipped,score,accuracy:attempted?correct/attempted:0,burned:wrong,left:skipped*4};
}
function labMean(xs){return xs.length?xs.reduce((a,b)=>a+b,0)/xs.length:0}
function labProject(scores){
  if(!scores.length)return 0;
  if(scores.length===1)return scores[0];
  const n=scores.length; let sx=0,sy=0,sxy=0,sx2=0;
  scores.forEach((y,i)=>{sx+=i;sy+=y;sxy+=i*y;sx2+=i*i});
  const den=n*sx2-sx*sx; if(!den)return scores[n-1];
  const slope=(n*sxy-sx*sy)/den, intercept=(sy-slope*sx)/n;
  return Math.round(Math.max(0,Math.min(720,intercept+slope*n)));
}
function labBand(score){
  if(score>=700)return ['Top band','Historically the first few hundred to low thousands. Not an AIR.'];
  if(score>=670)return ['High competitive','Often a serious government-seat conversation. Cutoffs move.'];
  if(score>=640)return ['Competitive','State merit and stronger private options usually open here.'];
  if(score>=600)return ['Mid competitive','Counselling range. Government seats get tight for general.'];
  if(score>=550)return ['Building','A 40–70 mark jump changes the map more than another full pass.'];
  if(score>=500)return ['Foundation','The paper is readable. Selection still needs a subject jump.'];
  return ['Early','Accuracy before volume. Negatives are eating the paper.'];
}
function labGapPlan(lt,weak,target){
  if(!lt)return 'Log a paper to unlock a week plan.';
  const gap=Math.max(0,target-lt.score);
  if(gap===0)return `Target ${target} already cleared on the latest paper. Hold accuracy; protect ${weak?weak[1]:'Biology'} from silly wrongs.`;
  const lines=[`Gap ${gap} marks to ${target}.`];
  if(weak)lines.push(`Priority subject: ${weak[1]} — raise % before adding new chapters.`);
  if(lt.burned>5)lines.push(`Cut about ${Math.min(lt.burned,Math.ceil(gap/5))} avoidable wrongs first (+5 each if they flip).`);
  if(lt.accuracy<0.75)lines.push(`Accuracy ${Math.round(lt.accuracy*100)}% — skip 50/50 guesses this week; use Skip drill in Practice.`);
  else if(lt.skipped>8)lines.push(`Accuracy is usable — selectively attempt ${Math.min(lt.skipped,Math.ceil(gap/4))} high-confidence blanks only.`);
  lines.push('This week: auto weak-chapter missions + one full mock analysis in Mock Lab.');
  return lines.join(' ');
}
function labWeak(m){
  return LAB_KEYS.slice().sort((a,b)=>labPart(m[a[0]])/a[3]-labPart(m[b[0]])/b[3])[0];
}
window.setMockTarget=v=>{db.settings.mockTarget=Math.max(0,Math.min(720,parseInt(v,10)||0));save()};
window.setMockLabFocus=id=>{mockLabFocusId=id;render()};
window.setMockLabFix=v=>{mockLabFix=parseInt(v,10)||0;render()};
window.deleteMockLab=id=>{db.mockLabs=(db.mockLabs||[]).filter(x=>x.id!==id);if(mockLabFocusId===id)mockLabFocusId=null;save();render();toast('Paper removed.')};

function mockLabView(){
  title('Mock Lab');
  const list=labList();
  const scores=list.map(m=>labTotals(m).score);
  const latest=list.at(-1)||null;
  const prev=list.at(-2)||null;
  const lt=latest?labTotals(latest):null;
  const delta=latest&&prev?lt.score-labTotals(prev).score:0;
  const avg=Math.round(labMean(scores));
  const best=scores.length?Math.max(...scores):0;
  const projected=labProject(scores);
  const target=db.settings.mockTarget??650;
  const focused=list.find(m=>m.id===mockLabFocusId)||latest;
  const ft=focused?labTotals(focused):null;
  const weak=latest?labWeak(latest):null;
  const band=lt?labBand(lt.score):null;
  const flip=ft?Math.max(0,Math.min(ft.wrong,mockLabFix)):0;
  const flipped=ft?Math.min(720,ft.score+flip*5):0;
  const labels=list.map(m=>new Date(m.date+'T12:00:00').toLocaleDateString('en-IN',{day:'numeric',month:'short'}));
  return `<div class="stats">
    ${stat('LATEST',lt?lt.score:'—',latest?`${esc(latest.name)} · ${delta>=0?'+':''}${delta} vs previous`:'Log right and wrong')}
    ${stat('BEST',best||'—',`${list.length} papers`)}
    ${stat('MEAN',list.length?avg:'—','All Mock Lab papers')}
    ${stat('IF SLOPE HOLDS',list.length?projected:'—',lt?`${projected-lt.score>=0?'+':''}${projected-lt.score} from latest`:'Need a paper')}
  </div>
  <div class="card" style="margin-bottom:14px">
    <div class="section-head" style="margin:0 0 8px"><h2>Target</h2><small>OUT OF 720</small></div>
    <div class="actions" style="align-items:center">
      <input type="number" min="0" max="720" value="${target}" style="max-width:120px" onchange="setMockTarget(this.value)">
      <span class="muted">${lt?`Gap ${Math.max(0,target-lt.score)} marks`:'Set the score you are actually writing for.'}</span>
    </div>
  </div>
  ${list.length?`<div class="grid2">
    <div class="card"><h3>Score trend</h3>${lineChart(labels,scores,720)}<p class="muted" style="font-size:10px;margin-top:8px">Solid line is the paper. Not a 3-test average — that lives in the mean card.</p></div>
    <div class="card"><h3>Unofficial band</h3><p style="font-size:22px;font-weight:800;margin:8px 0 6px">${band[0]}</p><p class="muted" style="font-size:12px;line-height:1.6">${band[1]}</p>
      <div class="list-item"><span>Accuracy</span><b>${Math.round(lt.accuracy*100)}%</b></div>
      <div class="list-item"><span>Attempted</span><b>${lt.attempted} / 180</b></div>
      <div class="list-item"><span>Burned on wrongs</span><b style="color:var(--red)">−${lt.burned}</b></div>
      <div class="list-item"><span>Left blank</span><b>${lt.skipped} Q · up to ${lt.left}</b></div>
      <div class="list-item"><span>Softest subject</span><b>${weak[1]}</b></div>
    </div>
  </div>
  <div class="card" style="margin-top:14px;border-color:#51212b">
    <h3>Rank gap plan (this week)</h3>
    <p style="font-size:13px;line-height:1.7;margin:8px 0 0">${esc(labGapPlan(lt,weak,target))}</p>
  </div>
  <div class="grid2" style="margin-top:14px">
    <div class="card">
      <div class="section-head" style="margin-top:0"><h2>Subject split</h2>
        <select onchange="setMockLabFocus(this.value)" style="max-width:220px">${list.map(m=>`<option value="${m.id}" ${m.id===(focused&&focused.id)?'selected':''}>${esc(m.date)} · ${esc(m.name)}</option>`).join('')}</select>
      </div>
      ${LAB_KEYS.map(([k,label,cap,max])=>{const a=focused[k]||{c:0,w:0};const sc=labPart(a);const w=Math.max(0,Math.min(100,sc/max*100));return `<div class="progress-row"><div class="progress-meta"><span>${label}</span><span>${a.c} right · ${a.w} wrong · ${sc}/${max}</span></div><div class="bar"><span style="width:${w}%"></span></div></div>`}).join('')}
    </div>
    <div class="card">
      <h3>If wrongs become rights</h3>
      <p class="muted" style="font-size:12px;line-height:1.6">One converted mistake is 5 marks: you stop losing 1 and start gaining 4.</p>
      <p style="font-size:32px;font-weight:800;margin:10px 0 0">${flipped}</p>
      <p class="muted" style="font-size:12px">+${flipped-ft.score} if ${flip} wrongs flip · now ${ft.score} · ${ft.wrong} wrongs on this paper</p>
      <input class="lab-range" type="range" min="0" max="${Math.max(1,ft.wrong)}" value="${flip}" oninput="setMockLabFix(this.value)">
      <p class="muted" style="font-size:12px;margin-top:8px">${target-flipped>0?`${target-flipped} still short of ${target}.`:`That fix clears ${target} on this paper.`}</p>
      <p class="muted" style="font-size:12px;line-height:1.6;margin-top:10px">${lt.accuracy<0.75?`${weak[1]} is the softest percent, and accuracy is ${Math.round(lt.accuracy*100)}%. Cutting wrongs there moves the total faster than attempting the ${lt.skipped} left blank.`:`${Math.round(lt.accuracy*100)}% accuracy is usable. ${weak[1]} is still the drag.`}</p>
    </div>
  </div>
  <div class="card" style="margin-top:14px"><h3>Papers</h3><div class="table-wrap"><table><thead><tr><th>PAPER</th><th>P</th><th>C</th><th>BOT</th><th>ZOO</th><th>SCORE</th><th>ACC</th><th></th></tr></thead><tbody>
    ${list.slice().reverse().map(m=>{const t=labTotals(m);return `<tr><td><b>${esc(m.name)}</b><div class="muted">${esc(m.date)}</div></td>${LAB_KEYS.map(([k])=>`<td>${labPart(m[k])}</td>`).join('')}<td><b>${t.score}</b></td><td>${Math.round(t.accuracy*100)}%</td><td><button class="icon-btn" onclick="deleteMockLab('${m.id}')">×</button></td></tr>`}).join('')}
  </tbody></table></div></div>`:'<div class="card"><div class="empty">No Mock Lab papers yet. Neeche right / wrong daalo — Physics aur Chemistry 45, Botany aur Zoology 45. +4 / −1.</div></div>'}
  <div class="card" style="margin-top:14px"><h3>Log a paper</h3>
    <form id="mockLabForm">
      <div class="form-grid">
        <div class="field"><label>NAME</label><input name="name" required placeholder="Full syllabus 10"></div>
        <div class="field"><label>DATE</label><input name="date" type="date" value="${today()}" required></div>
      </div>
      <div class="lab-grid">
        ${LAB_KEYS.map(([k,label,cap])=>`<fieldset class="sub-group-box" style="margin:0"><div class="sub-group-title">${label} · ${cap} Q</div><div class="form-grid"><div class="field"><label>RIGHT</label><input name="${k}C" inputmode="numeric" value="0"></div><div class="field"><label>WRONG</label><input name="${k}W" inputmode="numeric" value="0"></div></div></fieldset>`).join('')}
      </div>
      <button class="btn" style="margin-top:12px">Save paper</button>
    </form>
  </div>`;
}

function render(){
  let html={dashboard,missions,subjects,focus,practice:practiceView,mistakes,revision,tests,mocklab:mockLabView,vault,aitutor:aiTutorView,analytics}[current]();
  document.getElementById('view').innerHTML=html;
  bindForms();
  updateTopFocusUI();
  updateStreakBadge();
  if(current==='dashboard')startLiveCountdown();
  if(current==='practice' && practiceTab==='quiz' && activeQuiz && quizIdx<activeQuiz.length)startQuizStopwatch();
  if(current==='focus'){
    const stBtn=document.getElementById('startTimer');
    const rsBtn=document.getElementById('resetTimer');
    if(stBtn)stBtn.onclick=startTimer;
    if(rsBtn)rsBtn.onclick=resetCurrentTimer;
    updateTimerDOM();
  }
}

function bindForms(){
  const bind=(id,fn)=>{const el=document.getElementById(id);if(el)el.onsubmit=async e=>{e.preventDefault();await fn(el)}};
  bind('taskForm',f=>{
    const remVal=(f.reminderAt?.value||'').trim();
    if(remVal&&('Notification' in window)&&Notification.permission==='default')requestBrowserNotifyPermission(false);
    const fMins=parseInt(f.focusMins?.value,10);
    const mType=f.missionType?.value||suggestMissionType(f.text.value)||'Practice';
    const taskDate=remVal&&/^\d{4}-\d{2}-\d{2}/.test(remVal)?remVal.slice(0,10):today();
    db.tasks.push({
      id:crypto.randomUUID(),
      date:taskDate,
      text:f.text.value.trim(),
      subject:f.subject.value,
      missionType:mType,
      done:false,
      ...(fMins>0?{focusMins:fMins}:{}),
      ...(remVal?{reminderAt:remVal,reminded:false}:{})
    });
    if(fMins>0 && !timerRunning){
      clearInterval(timerInterval);
      timerInitial=fMins*60;
      timerSeconds=fMins*60;
    }
    save();
    checkDueReminders();
    render();
    toast(fMins>0?`Mission (${mType}) & ${fMins}m focus saved.`:`Mission (${mType}) added.`);
  });
  bind('sessionForm',f=>{
    const mins=parseInt(f.minutes.value,10);
    const qs=parseInt(f.questions?.value,10)||0;
    if(!mins||mins<1){toast('Enter valid minutes.');return}
    db.sessions.push({id:crypto.randomUUID(),date:today(),subject:f.subject.value,minutes:mins,questions:qs,topic:f.topic.value.trim()});
    save();render();toast('Study session saved.');
  });
  bind('mistakeForm',async f=>{
    let fileMeta={};
    const fileInput=f.attachment.files[0];
    if(fileInput){
      const saved=await storeFile(fileInput);
      fileMeta={fileId:saved.id,fileName:saved.name,fileType:saved.type};
    }
    db.mistakes.push({id:crypto.randomUUID(),date:today(),subject:f.subject.value,type:f.type.value,chapter:f.chapter.value.trim(),error:f.error.value.trim(),fix:f.fix.value.trim(),...fileMeta});
    save();render();toast('Mistake recorded.');
  });
  bind('vaultForm',async f=>{
    const fileInput=f.file.files[0];
    const link=(f.link?.value||'').trim();
    if(link&&!/^https?:\/\//i.test(link)){toast('Link http:// ya https:// se shuru hona chahiye.');return}
    if(!fileInput&&!link){toast('File ya link me se kuch to daalo.');return}
    const saved=fileInput?await storeFile(fileInput):null;
    db.vault=db.vault||[];
    db.vault.push({id:crypto.randomUUID(),date:today(),title:f.title.value.trim(),subject:f.subject.value,chapter:(f.chapter&&f.chapter.value)||'',category:f.category.value,...(saved?{fileId:saved.id,fileName:saved.name,fileType:saved.type}:{}),...(link?{link}:{})});
    save();render();toast('Saved to Study Vault.');
  });
  bind('revisionForm',f=>{
    const d=today();
    db.revisions.push({id:crypto.randomUUID(),date:d,r3:addDays(d,3),r7:addDays(d,7),r21:addDays(d,21),chapter:f.chapter.value.trim(),subject:f.subject.value,kind:f.kind.value});
    save();render();toast('Revision logged (+3d / +7d / +21d scheduled).');
  });
  bind('upcomingForm',async f=>{
    let meta=null;
    const file=f.file.files[0];
    if(file){
      try{meta=await storeFile(file)}catch(_){toast('File save nahi hui, bina file ke save kar raha hoon.')}
    }
    db.upcomingTests=db.upcomingTests||[];
    db.upcomingTests.push({id:crypto.randomUUID(),name:f.elements.name.value.trim(),date:f.date.value,time:f.time.value||'',syllabus:f.syllabus.value.trim(),...(meta?{fileId:meta.id,fileName:meta.name,fileType:meta.type}:{})});
    save();render();toast('Upcoming test added.');
  });
  bind('testForm',async f=>{
    const p=+f.physics.value||0,c=+f.chemistry.value||0,b=+f.biology.value||0;
    const manualScore=+f.score.value;
    const finalScore=(p||c||b)?(manualScore||p+c+b):manualScore;
    let meta=null;
    const file=f.file&&f.file.files[0];
    if(file){try{meta=await storeFile(file)}catch(_){toast('File save nahi hui, bina file ke save kar raha hoon.')}}
    db.tests.push({id:crypto.randomUUID(),date:today(),name:f.name.value.trim(),score:finalScore,accuracy:+f.accuracy.value||0,physics:p,chemistry:c,biology:b,analysis:'',analysisDone:!!(f.analysisDone&&f.analysisDone.checked),...(meta?{fileId:meta.id,fileName:meta.name,fileType:meta.type}:{})});
    save();render();toast('Mock test saved.');
  });
  bind('mockLabForm',f=>{
    const paper={id:crypto.randomUUID(),name:f.name.value.trim(),date:f.date.value||today()};
    for(const [k,label,cap] of LAB_KEYS){
      const c=Math.floor(Number(f[k+'C'].value)||0);
      const w=Math.floor(Number(f[k+'W'].value)||0);
      if(c<0||w<0||c+w>cap){toast(`${label}: right + wrong cannot pass ${cap}.`);return}
      paper[k]={c,w};
    }
    db.mockLabs=db.mockLabs||[];
    db.mockLabs.push(paper);
    mockLabFocusId=paper.id;
    save();render();toast('Mock Lab paper saved.');
  });
}

window.toggleTask=(id,v)=>{let t=db.tasks.find(x=>x.id===id);if(t){t.done=v;t.doneAt=v?today():null}save();render()};
window.setMissionTab=t=>{missionTab=t;render()};
window.setMistakeTab=t=>{mistakeTab=t;render()};
window.toggleMistake=id=>{let m=db.mistakes.find(x=>x.id===id);if(m){m.closed=!m.closed;m.closedAt=m.closed?today():null}save();render();toast(m&&m.closed?'Mistake closed.':'Mistake reopened.')};
window.deleteTask=id=>{db.tasks=db.tasks.filter(t=>t.id!==id);save();render();toast('Mission removed.')};
window.deleteChapter=(s,id)=>{if(!confirm('Delete this chapter and all its ticks and dates?'))return;db.chapters[s]=db.chapters[s].filter(x=>x.id!==id);save();keepScroll(render)};
window.removeItem=(list,id)=>{db[list]=db[list].filter(x=>x.id!==id);save();render();toast('Record removed.')};

const exportBtnEl=document.getElementById('exportBtn');
if(exportBtnEl)exportBtnEl.onclick=()=>{let blob=new Blob([JSON.stringify(db,null,2)],{type:'application/json'}),a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download=`igris-backup-${today()}.json`;a.click();URL.revokeObjectURL(a.href);toast('Backup downloaded.')};
if(!db.syllabusLoaded&&Object.values(db.chapters).every(a=>!a.length))loadSyllabus(true);
render();
checkDueReminders();
clearInterval(reminderInterval);
reminderInterval=setInterval(checkDueReminders,15000);
const importBtnEl=document.getElementById('importBtn');
const importFileEl=document.getElementById('importFile');
if(importBtnEl&&importFileEl)importBtnEl.onclick=()=>importFileEl.click();
if(importFileEl)importFileEl.onchange=e=>{const file=e.target.files[0];if(!file)return;const r=new FileReader();r.onload=()=>{try{const d=JSON.parse(r.result);if(!d||!Array.isArray(d.tasks))throw new Error('bad');if(!confirm('Replace current data with this backup?')){e.target.value='';return}db={...seed(),...d};save();render();toast('Backup imported.')}catch(_){toast('Invalid backup file.')}e.target.value=''};r.readAsText(file)};

document.addEventListener('visibilitychange',async()=>{
  if(document.hidden){try{flushMissionSession();saveQuizProgress()}catch(_){}return}
  timerTick();
  checkDueReminders();
  const el=document.getElementById('timerFull');
  if(el&&!el.hidden&&'wakeLock' in navigator){try{wakeLock=await navigator.wakeLock.request('screen')}catch(_){}}
});
