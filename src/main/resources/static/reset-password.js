'use strict';
(() => {
  const text = {
    'pt-BR': { title:'Definir nova senha',intro:'O link vale por 15 minutos e pode ser usado uma única vez.',
      password:'Nova senha',confirm:'Confirme a nova senha',submit:'Salvar nova senha',show:'Mostrar senha',hide:'Ocultar senha',
      invalid:'Link inválido, expirado ou já utilizado. Solicite um novo link no aplicativo.',
      mismatch:'As senhas precisam ser iguais, com pelo menos 8 caracteres e até 72 bytes.',
      pending:'Salvando...',success:'Senha alterada. Volte ao aplicativo e entre com a nova senha.',
      network:'Não foi possível conectar. Confira sua internet e tente novamente.',
      rate:'Muitas tentativas. Aguarde e tente novamente.'},
    en: { title:'Set a new password',intro:'The link expires after 15 minutes and can only be used once.',
      password:'New password',confirm:'Confirm new password',submit:'Save new password',show:'Show password',hide:'Hide password',
      invalid:'Invalid, expired or already used link. Request another link in the app.',
      mismatch:'Passwords must match, with at least 8 characters and at most 72 bytes.',
      pending:'Saving...',success:'Password changed. Return to the app and sign in with the new password.',
      network:'Could not connect. Check your internet connection and try again.',
      rate:'Too many attempts. Please wait and try again.'}
  };
  const get=id=>document.getElementById(id);
  let token=new URLSearchParams(location.hash.slice(1)).get('token') || '';
  history.replaceState(null,'',location.pathname);
  let language='pt-BR',done=false;
  const setStatus=value=>{ get('status').textContent=value; };
  function labels(){
    document.documentElement.lang=language;
    const t=text[language];
    get('title').textContent=t.title;get('intro').textContent=t.intro;
    get('password-label').textContent=t.password;get('confirm-label').textContent=t.confirm;get('submit').textContent=t.submit;
    get('toggle').setAttribute('aria-label',get('password').type==='password'?t.show:t.hide);
    if(!/^[A-Za-z0-9_-]{43}$/.test(token) && !done){setStatus(t.invalid);get('submit').disabled=true;}
  }
  get('language').addEventListener('change',event=>{language=event.target.value;labels();});
  get('toggle').addEventListener('click',()=>{
    const show=get('password').type==='password';get('password').type=show?'text':'password';
    get('toggle').setAttribute('aria-pressed',String(show));
    get('toggle').setAttribute('aria-label',show?text[language].hide:text[language].show);
  });
  get('reset-form').addEventListener('submit',async event=>{
    event.preventDefault();const t=text[language],password=get('password').value;
    if(done || get('submit').disabled)return;
    if(password!==get('confirm').value || password.length<8 || new TextEncoder().encode(password).length>72){setStatus(t.mismatch);return;}
    get('submit').disabled=true;setStatus(t.pending);
    try{
      const response=await fetch('/auth/reset-password',{method:'POST',credentials:'omit',
        headers:{'Content-Type':'application/json'},body:JSON.stringify({token,password}),signal:AbortSignal.timeout(30000)});
      if(response.status===204){done=true;token='';get('reset-form').reset();get('reset-form').hidden=true;setStatus(t.success);}
      else{setStatus(response.status===429?t.rate:t.invalid);get('submit').disabled=false;}
    }catch{setStatus(t.network);get('submit').disabled=false;}
  });
  labels();
})();
