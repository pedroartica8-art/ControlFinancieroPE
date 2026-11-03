(function(){
  function money(x){return "S/ "+Number(x||0).toLocaleString("es-PE",{minimumFractionDigits:2,maximumFractionDigits:2});}

  function renameText(){
    var root=document.body;
    var walker=document.createTreeWalker(root,NodeFilter.SHOW_TEXT);
    var nodes=[];
    while(walker.nextNode()) nodes.push(walker.currentNode);
    nodes.forEach(function(n){
      var s=n.nodeValue||"";
      s=s.replace(/Gastos · ahorro · mudanza · metas/g,"Vida independiente · pagos · ahorro · Excel");
      s=s.replace(/Mudanza \/ independencia/g,"Equipar mi cuarto");
      s=s.replace(/Meta para mudanza/g,"Meta para equipar cuarto");
      s=s.replace(/Fecha objetivo de mudanza/g,"Fecha objetivo de equipamiento");
      s=s.replace(/Mudanza y fondo/g,"Cuarto y fondo");
      s=s.replace(/Independizarte con orden/g,"Vivir solo con orden");
      s=s.replace(/avanzar hacia la mudanza/g,"avanzar con el equipamiento del cuarto");
      s=s.replace(/capital para mudanza/g,"capital para equipar el cuarto");
      n.nodeValue=s;
    });
  }

  function ensurePlanTab(){
    var tabs=document.getElementById("tabs");
    if(!tabs || document.getElementById("novPlanTab")) return;
    var b=document.createElement("button");
    b.id="novPlanTab";
    b.className="tab";
    b.textContent="Plan Nov.";
    b.onclick=showNovPlan;
    tabs.appendChild(b);
  }

  function showNovPlan(){
    document.querySelectorAll(".tab").forEach(function(x){x.classList.remove("active");});
    var b=document.getElementById("novPlanTab"); if(b)b.classList.add("active");
    var app=document.getElementById("app"); if(!app)return;
    app.innerHTML=
      '<div class="section-title"><h2>Plan de noviembre 2026</h2><small>Primer mes viviendo solo</small></div>'+
      '<div class="card highlight"><div class="kpi-label">Dinero con el que iniciaste</div><div class="kpi">S/ 600.00</div><div style="margin-top:7px;font-size:12px">Objetivo: llegar al primer cobro sin descapitalizarte y terminar noviembre sin deuda.</div></div>'+
      '<div class="grid">'+
        '<div class="card"><div class="kpi-label">Ingreso mensual</div><div class="kpi">S/ 2,000</div></div>'+
        '<div class="card"><div class="kpi-label">Reserva mínima</div><div class="kpi">S/ 200</div></div>'+
        '<div class="card"><div class="kpi-label">Comida máximo</div><div class="kpi">S/ 450</div></div>'+
        '<div class="card"><div class="kpi-label">Cierre proyectado</div><div class="kpi pos">S/ 548</div></div>'+
      '</div>'+
      '<div class="card"><h3>Calendario de pagos</h3>'+
        '<div class="summary-line"><span>09 · Internet</span><b>S/ 30</b></div>'+
        '<div class="summary-line"><span>15 · Deuda única</span><b class="neg">S/ 330</b></div>'+
        '<div class="summary-line"><span>18 · ChatGPT</span><b>S/ 81</b></div>'+
        '<div class="summary-line"><span>28 · Celular</span><b>S/ 131</b></div>'+
        '<div class="summary-line"><span>29 · Cuarto</span><b>S/ 280</b></div>'+
      '</div>'+
      '<div class="card"><h3>Comida mientras comes fuera</h3>'+
        '<div class="summary-line"><span>Tope mensual</span><b>S/ 450</b></div>'+
        '<div class="summary-line"><span>Tope semanal aproximado</span><b>S/ 104</b></div>'+
        '<div class="summary-line"><span>Guía diaria promedio</span><b>S/ 15</b></div>'+
        '<div class="note" style="margin-top:8px">El almuerzo de lunes a viernes ya lo cubre el trabajo. Registra desayuno, cena y las comidas del fin de semana para saber tu gasto real.</div>'+
      '</div>'+
      '<div class="card"><h3>Orden de prioridades</h3>'+
        '<div class="summary-line"><span>1. Pagar deuda</span><b>S/ 330</b></div>'+
        '<div class="summary-line"><span>2. Fondo de emergencia este mes</span><b>S/ 200</b></div>'+
        '<div class="summary-line"><span>3. Ventilador</span><b>máx. S/ 120</b></div>'+
        '<div class="summary-line"><span>4. Fondo mantenimiento moto</span><b>S/ 30</b></div>'+
        '<div class="summary-line"><span>5. Mesa / olla / silla</span><b class="muted">después</b></div>'+
      '</div>'+
      '<div class="recommend goodrec"><b>Regla del mes:</b> el dinero reservado para celular, cuarto, internet, deuda y ahorro ya no se considera dinero libre aunque siga en Yape o en tu cuenta.</div>';
  }

  function addHomeNote(){
    var app=document.getElementById("app");
    if(!app || document.getElementById("novHomeCard")) return;
    if(!app.textContent.includes("Accesos rápidos")) return;
    var c=document.createElement("div");
    c.id="novHomeCard";
    c.className="card";
    c.innerHTML='<div class="row"><div><div class="item-title">Noviembre: etapa de estabilización</div><div class="item-sub">Deuda S/330 · comida ≤ S/450 · ventilador ≤ S/120 · reserva mínima S/200</div></div><button class="secondary small right" id="openNovPlan">Ver plan</button></div>';
    app.insertBefore(c,app.firstChild);
    document.getElementById("openNovPlan").onclick=showNovPlan;
  }

  function patch(){
    renameText();
    ensurePlanTab();
    addHomeNote();
  }

  var observer=new MutationObserver(function(){setTimeout(patch,0);});
  observer.observe(document.body,{childList:true,subtree:true});
  patch();
})();