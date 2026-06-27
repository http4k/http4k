<#-- noparse: browser JavaScript, not a template -->
<#noparse>
<script>
function log(m){const e=document.getElementById('log');if(e)e.textContent=m}
function nextUrl(){return new URLSearchParams(location.search).get('next')||'/protected'}
function b64urlToBuf(s){s=s.replace(/-/g,'+').replace(/_/g,'/');while(s.length%4)s+='=';const b=atob(s),a=new Uint8Array(b.length);for(let i=0;i<b.length;i++)a[i]=b.charCodeAt(i);return a.buffer}
function bufToB64url(buf){const a=new Uint8Array(buf);let s='';for(let i=0;i<a.length;i++)s+=String.fromCharCode(a[i]);return btoa(s).replace(/\+/g,'-').replace(/\//g,'_').replace(/=/g,'')}
function descriptors(a){return (a||[]).map(d=>({type:'public-key',id:b64urlToBuf(d.id),transports:d.transports||[]}))}
function regBody(c){return JSON.stringify({
  credentialId:bufToB64url(c.rawId),clientDataJSON:bufToB64url(c.response.clientDataJSON),
  attestationObject:bufToB64url(c.response.attestationObject),
  transports:c.response.getTransports?c.response.getTransports():[],
  clientExtensionResults:c.getClientExtensionResults()})}
async function create(o){return navigator.credentials.create({publicKey:{
  challenge:b64urlToBuf(o.challenge),
  rp:{id:o.rp.id,name:o.rp.name},
  user:{id:b64urlToBuf(o.user.handle),name:o.user.name,displayName:o.user.displayName},
  pubKeyCredParams:o.pubKeyCredParams,
  excludeCredentials:descriptors(o.excludeCredentials),
  authenticatorSelection:o.authenticatorSelection,attestation:o.attestation,timeout:o.timeout,extensions:o.extensions}})}
async function register(){try{
  const o=await fetch('/passkeys/register/options',{method:'POST'}).then(r=>r.json());
  const c=await create(o);
  const r=await fetch('/passkeys/register',{method:'POST',headers:{'content-type':'application/json'},body:regBody(c)});
  log(r.ok?'passkey added - log out, then sign in with it':'add failed: '+r.status+' '+await r.text());
}catch(e){log('error: '+e)}}
async function signup(){try{
  const body=JSON.stringify({
    email:document.getElementById('email').value,
    firstName:document.getElementById('firstName').value,
    lastName:document.getElementById('lastName').value});
  const o=await fetch('/passkeys/register/options',{method:'POST',headers:{'content-type':'application/json'},body}).then(r=>r.json());
  const c=await create(o);
  const r=await fetch('/passkeys/register',{method:'POST',headers:{'content-type':'application/json'},body:regBody(c)});
  if(r.ok){location='/protected'}else{log('signup failed: '+r.status+' '+await r.text())}
}catch(e){log('error: '+e)}}
async function authenticate(){try{
  const o=await fetch('/passkeys/authenticate/options',{method:'POST'}).then(r=>r.json());
  const c=await navigator.credentials.get({publicKey:{
    challenge:b64urlToBuf(o.challenge),rpId:o.rp.id,
    allowCredentials:descriptors(o.allowCredentials),
    userVerification:o.userVerification,timeout:o.timeout}});
  const r=await fetch('/passkeys/authenticate',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({
    credentialId:bufToB64url(c.rawId),clientDataJSON:bufToB64url(c.response.clientDataJSON),
    authenticatorData:bufToB64url(c.response.authenticatorData),signature:bufToB64url(c.response.signature)})});
  if(r.ok){location=nextUrl()}else{log('auth failed: '+r.status+' '+await r.text())}
}catch(e){log('error: '+e)}}
</script>
</#noparse>
