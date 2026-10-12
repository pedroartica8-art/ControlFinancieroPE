(function(){
  try{
    var raw=atob(window.__APP64||"");
    var bytes=new Uint8Array(raw.length);
    for(var i=0;i<raw.length;i++)bytes[i]=raw.charCodeAt(i);
    var code=new TextDecoder("utf-8").decode(bytes);
    (0,eval)(code);
  }catch(e){
    document.getElementById("app").innerHTML="<div class='card'><b>No se pudo iniciar la app.</b><br><small>"+String(e)+"</small></div>";
  }
})();