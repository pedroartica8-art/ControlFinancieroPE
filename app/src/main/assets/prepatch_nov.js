(function(){
  var KEY="control_financiero_pe_v3";
  var db=null;
  try{ db=JSON.parse(localStorage.getItem(KEY)||"null"); }catch(e){}

  var now=new Date();
  var currentMonth=now.getFullYear()+"-"+String(now.getMonth()+1).padStart(2,"0");

  if(!db){
    db={
      version:3,
      config:{
        expectedIncome:2000,
        openingBalance:600,
        minBalance:200,
        saveRate:0.10,
        emergencyGoal:1000,
        movingGoal:660,
        movingDate:"2027-02-28",
        evaluatedMonth:"2026-11"
      },
      movements:[],
      payments:[],
      budgets:[]
    };
  }

  db.config=db.config||{};
  db.movements=Array.isArray(db.movements)?db.movements:[];
  db.payments=Array.isArray(db.payments)?db.payments:[];
  db.budgets=Array.isArray(db.budgets)?db.budgets:[];

  db.config.expectedIncome=2000;
  db.config.minBalance=200;
  db.config.saveRate=0.10;
  db.config.emergencyGoal=1000;
  db.config.movingGoal=660;
  db.config.movingDate="2027-02-28";
  db.config.evaluatedMonth="2026-11";

  var novMoves=db.movements.filter(function(m){
    return String(m.date||"").slice(0,7)==="2026-11";
  });
  if(novMoves.length===0 && Number(db.config.openingBalance||0)===0){
    db.config.openingBalance=600;
  }

  var limits={
    "Vivienda":280,
    "Alimentación":450,
    "Moto/Transporte":130,
    "Servicios":161,
    "Hogar":50,
    "Salud":50,
    "Educación/Profesión":81,
    "Personal/Ocio":100,
    "Viajes":0,
    "Imprevistos/Otros":100,
    "Deudas":0,
    "Ahorro/Metas":0
  };
  db.budgets=Object.keys(limits).map(function(category){
    return {category:category,limit:limits[category]};
  });

  function upsertPay(concept,amount,day,type,category,medium){
    var key=concept.toLowerCase();
    var p=db.payments.find(function(x){
      return String(x.concept||"").trim().toLowerCase()===key;
    });
    if(!p){
      p={id:Date.now()+Math.floor(Math.random()*100000),concept:concept};
      db.payments.push(p);
    }
    p.concept=concept;
    p.amount=amount;
    p.day=day;
    p.type=type;
    p.category=category;
    p.medium=medium;
    p.active=true;
  }

  upsertPay("internet",30,9,"Gasto","Servicios","Yape");
  upsertPay("chatgpt",81,18,"Gasto","Educación/Profesión","Tarjeta");
  upsertPay("celular",131,28,"Gasto","Servicios","Yape");
  upsertPay("cuarto",280,29,"Gasto","Vivienda","Yape");

  if(currentMonth==="2026-11"){
    upsertPay("deuda actual noviembre",330,15,"Pago deuda","Deudas","Yape");
  }else{
    db.payments=db.payments.filter(function(p){
      return String(p.concept||"").trim().toLowerCase()!=="deuda actual noviembre";
    });
  }

  db.novemberPlan={
    startingCash:600,
    firstPayday:15,
    secondPayday:30,
    foodBudget:450,
    weeklyFoodCap:104,
    dailyFoodGuide:15,
    motorcycleFund:30,
    emergencyContribution:200,
    equipmentContribution:120,
    fanCap:120,
    minimumReserve:200,
    projectedClose:548,
    note:"Noviembre 2026: pagar deuda, proteger pagos del 18/28/29, comprar ventilador solo con tope de S/120 y cerrar con reserva."
  };

  localStorage.setItem(KEY,JSON.stringify(db));
})();