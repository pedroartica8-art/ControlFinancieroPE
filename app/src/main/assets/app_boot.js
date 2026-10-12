(function(){
  try{
    var nativeIso=Date.prototype.toISOString;
    Date.prototype.toISOString=function(){
      var local=new Date(this.getTime()-this.getTimezoneOffset()*60000);
      return nativeIso.call(local);
    };
    var raw=atob(window.__APP64||"");
    var bytes=new Uint8Array(raw.length);
    for(var i=0;i<raw.length;i++)bytes[i]=raw.charCodeAt(i);
    var code=new TextDecoder("utf-8").decode(bytes);
    (0,eval)(code);
  }catch(e){
    document.getElementById("app").innerHTML="<div class='card'><b>No se pudo iniciar la app.</b><br><small>"+String(e)+"</small></div>";
  }
})();