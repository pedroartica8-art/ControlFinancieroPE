package pe.controlfinanciero.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    static final String[] TYPES = {
            "Ingreso", "Gasto", "Ahorro", "Retiro ahorro", "Pago deuda",
            "Préstamo recibido", "Dinero prestado", "Cobro"
    };
    static final String[] CATS = {
            "Vivienda", "Alimentación", "Moto/Transporte", "Servicios", "Hogar",
            "Salud", "Educación/Profesión", "Personal/Ocio", "Viajes",
            "Imprevistos/Otros", "Deudas", "Ahorro/Metas"
    };
    static final String[] CLS = {"Necesidad", "Deseo", "Deuda", "Ahorro", "Ingreso"};
    static final String[] NAT = {"Fijo", "Variable", "Eventual"};
    static final String[] MED = {"Yape", "Efectivo", "Transferencia", "Tarjeta", "Plin", "Otro"};

    final int BG = Color.rgb(9, 12, 16);
    final int CARD = Color.rgb(22, 27, 34);
    final int TEXT = Color.rgb(241, 245, 249);
    final int MUTED = Color.rgb(160, 174, 192);
    final int BLUE = Color.rgb(100, 181, 246);
    final int GREEN = Color.rgb(102, 187, 106);
    final int RED = Color.rgb(239, 83, 80);
    final int AMBER = Color.rgb(255, 183, 77);

    DB db;
    SharedPreferences sp;
    FrameLayout body;
    String pendingExport = "";

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new DB(this);
        sp = getSharedPreferences("cfg", MODE_PRIVATE);
        seed();
        setContentView(shell());
        dashboard();
    }

    void seed() {
        if (!sp.contains("income")) {
            sp.edit()
                    .putString("income", "2000")
                    .putString("opening", "0")
                    .putString("minimum", "0")
                    .putString("saveRate", "0.20")
                    .apply();
        }
    }

    View shell() {
        LinearLayout root = vbox(0);
        root.setBackgroundColor(BG);

        LinearLayout header = vbox(14);
        header.addView(txt("Control Financiero", 24, TEXT, true));
        header.addView(txt("Registro diario • límites • exportación a Excel", 12, MUTED, false));
        root.addView(header);

        body = new FrameLayout(this);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"Inicio", "Registrar", "Movs", "Pagos", "Ajustes"};
        for (String label : labels) {
            Button x = btn(label);
            nav.addView(x, new LinearLayout.LayoutParams(0, dp(52), 1f));
        }
        ((Button) nav.getChildAt(0)).setOnClickListener(v -> dashboard());
        ((Button) nav.getChildAt(1)).setOnClickListener(v -> movementForm(null));
        ((Button) nav.getChildAt(2)).setOnClickListener(v -> movements());
        ((Button) nav.getChildAt(3)).setOnClickListener(v -> payments());
        ((Button) nav.getChildAt(4)).setOnClickListener(v -> settings());
        root.addView(nav);
        return root;
    }

    void dashboard() {
        body.removeAllViews();
        ScrollView sv = new ScrollView(this);
        LinearLayout box = vbox(14);
        Snap s = snapshot();

        box.addView(title("Resumen de " + YearMonth.now()));
        box.addView(card(s.verdict, 14, s.endProjection < 0 ? RED : GREEN, true));

        LinearLayout r1 = row();
        r1.addView(kpi("Saldo visible", s.visible, BLUE), wt());
        r1.addView(kpi("Gastable", s.spendable, GREEN), wt());
        box.addView(r1);

        LinearLayout r2 = row();
        r2.addView(kpi("Límite diario", s.daily, AMBER), wt());
        r2.addView(kpi("Límite semanal", s.weekly, AMBER), wt());
        box.addView(r2);

        box.addView(block("¿Me alcanza?", s.advice));
        box.addView(block(
                "Compromisos",
                "Pagos mensuales pendientes: S/ " + m(s.pending) +
                        "\nAhorro objetivo pendiente: S/ " + m(s.savePending) +
                        "\nSaldo mínimo intocable: S/ " + m(s.minimum)
        ));
        box.addView(block(
                "Mes actual",
                "Ingresos: S/ " + m(s.income) +
                        "\nGastos: S/ " + m(s.expenses) +
                        "\nAhorro: S/ " + m(s.savings) +
                        "\nPago de deuda: S/ " + m(s.debt) +
                        "\nPréstamos recibidos: S/ " + m(s.loans)
        ));

        Button evaluate = primary("EVALUARME");
        evaluate.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(s.verdict)
                .setMessage(s.advice +
                        "\n\nProyección de cierre: S/ " + m(s.endProjection) +
                        "\nDías restantes: " + s.days)
                .setPositiveButton("Entendido", null)
                .show());
        box.addView(evaluate);

        Button copy = btn("COPIAR ESTA SEMANA PARA EXCEL");
        copy.setOnClickListener(v -> copyWeek());
        box.addView(copy);

        Button csv = btn("EXPORTAR ESTA SEMANA A CSV");
        csv.setOnClickListener(v -> exportWeek());
        box.addView(csv);

        sv.addView(box);
        body.addView(sv);
    }

    void movementForm(Long editId) {
        body.removeAllViews();
        ScrollView sv = new ScrollView(this);
        LinearLayout box = vbox(14);
        box.addView(title(editId == null ? "Registrar movimiento" : "Editar movimiento"));

        Rec old = editId == null ? null : db.one(editId);
        EditText date = input("Fecha AAAA-MM-DD", old == null ? LocalDate.now().toString() : old.date);
        Spinner type = spin(TYPES, old == null ? "Gasto" : old.type);
        Spinner cat = spin(CATS, old == null ? "Alimentación" : old.cat);
        Spinner cls = spin(CLS, old == null ? "Necesidad" : old.cls);
        Spinner nat = spin(NAT, old == null ? "Variable" : old.nat);
        Spinner med = spin(MED, old == null ? "Yape" : old.med);
        EditText concept = input("Concepto", old == null ? "" : old.concept);
        EditText amount = num("Monto (S/)", old == null ? "" : m(old.amount));
        EditText dest = input("Destino / persona / meta", old == null ? "" : old.dest);
        EditText note = input("Nota", old == null ? "" : old.note);

        View[] fields = {
                label("Fecha"), date, label("Tipo"), type, label("Categoría"), cat,
                concept, amount, label("Clasificación"), cls, label("Naturaleza"), nat,
                label("Medio"), med, dest, note
        };
        for (View field : fields) box.addView(field);

        Button save = primary("GUARDAR MOVIMIENTO");
        save.setOnClickListener(v -> {
            try {
                double value = Double.parseDouble(amount.getText().toString().replace(",", "."));
                if (value <= 0) throw new IllegalArgumentException();
                LocalDate.parse(date.getText().toString());

                Rec r = new Rec();
                r.id = editId == null ? 0 : editId;
                r.date = date.getText().toString();
                r.type = type.getSelectedItem().toString();
                r.cat = cat.getSelectedItem().toString();
                r.concept = concept.getText().toString().trim();
                r.amount = value;
                r.cls = cls.getSelectedItem().toString();
                r.nat = nat.getSelectedItem().toString();
                r.med = med.getSelectedItem().toString();
                r.dest = dest.getText().toString().trim();
                r.month = r.date.substring(0, 7);
                r.note = note.getText().toString().trim();

                if (r.concept.isEmpty()) {
                    toast("Escribe un concepto");
                    return;
                }

                if (editId == null) db.add(r);
                else db.update(r);

                toast("Guardado");
                dashboard();
            } catch (Exception e) {
                toast("Revisa fecha y monto");
            }
        });
        box.addView(save);

        sv.addView(box);
        body.addView(sv);
    }

    void movements() {
        body.removeAllViews();
        ScrollView sv = new ScrollView(this);
        LinearLayout box = vbox(12);
        box.addView(title("Movimientos"));

        List<Rec> list = db.range(
                LocalDate.now().minusDays(60).toString(),
                LocalDate.now().toString()
        );

        if (list.isEmpty()) {
            box.addView(block("Sin datos", "Aún no has registrado movimientos."));
        }

        for (Rec r : list) {
            TextView item = card(
                    r.date + " • " + r.type +
                            "\n" + r.concept + " — S/ " + m(r.amount) +
                            "\n" + r.cat + " • " + r.med,
                    14, TEXT, false
            );
            item.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle(r.concept)
                    .setItems(new String[]{"Editar", "Eliminar", "Cancelar"}, (dialog, which) -> {
                        if (which == 0) {
                            movementForm(r.id);
                        } else if (which == 1) {
                            new AlertDialog.Builder(this)
                                    .setMessage("¿Eliminar este movimiento?")
                                    .setPositiveButton("Eliminar", (dd, ww) -> {
                                        db.del(r.id);
                                        movements();
                                    })
                                    .setNegativeButton("Cancelar", null)
                                    .show();
                        }
                    })
                    .show());
            box.addView(item);
        }

        sv.addView(box);
        body.addView(sv);
    }

    void payments() {
        body.removeAllViews();
        ScrollView sv = new ScrollView(this);
        LinearLayout box = vbox(12);
        box.addView(title("Pagos mensuales"));
        box.addView(txt(
                "Registra alquiler, celular, internet, cuotas u otros compromisos que se repiten cada mes.",
                13, MUTED, false
        ));

        Button add = primary("+ AGREGAR PAGO MENSUAL");
        add.setOnClickListener(v -> paymentDialog());
        box.addView(add);

        for (Pay p : db.pays()) {
            YearMonth ym = YearMonth.now();
            double paid = db.sumExact(
                    p.type, p.concept,
                    ym.atDay(1).toString(),
                    ym.atEndOfMonth().toString()
            );
            double pending = Math.max(0, p.amount - paid);
            String status = pending <= 0
                    ? "PAGADO"
                    : (p.day < LocalDate.now().getDayOfMonth() ? "VENCIDO" : "PENDIENTE");

            TextView item = card(
                    p.concept + " — S/ " + m(p.amount) +
                            "\nDía " + p.day + " • " + p.type + " • " + status +
                            (pending > 0 ? "\nPendiente: S/ " + m(pending) : ""),
                    14,
                    pending <= 0 ? GREEN : ("VENCIDO".equals(status) ? RED : TEXT),
                    false
            );
            item.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setMessage("¿Eliminar " + p.concept + "?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            db.delPay(p.id);
                            payments();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                return true;
            });
            box.addView(item);
        }

        sv.addView(box);
        body.addView(sv);
    }

    void paymentDialog() {
        LinearLayout box = vbox(12);
        EditText concept = input("Concepto exacto", "");
        EditText amount = num("Monto mensual", "");
        EditText day = num("Día de pago (1-31)", "1");
        Spinner type = spin(new String[]{"Gasto", "Pago deuda"}, "Gasto");
        Spinner cat = spin(CATS, "Servicios");

        box.addView(concept);
        box.addView(amount);
        box.addView(day);
        box.addView(label("Tipo"));
        box.addView(type);
        box.addView(label("Categoría"));
        box.addView(cat);

        new AlertDialog.Builder(this)
                .setTitle("Nuevo pago mensual")
                .setView(box)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    try {
                        Pay p = new Pay();
                        p.concept = concept.getText().toString().trim();
                        p.amount = Double.parseDouble(amount.getText().toString().replace(",", "."));
                        p.day = Math.max(1, Math.min(31, Integer.parseInt(day.getText().toString())));
                        p.type = type.getSelectedItem().toString();
                        p.cat = cat.getSelectedItem().toString();
                        if (p.concept.isEmpty() || p.amount <= 0) throw new IllegalArgumentException();
                        db.addPay(p);
                        payments();
                    } catch (Exception e) {
                        toast("Datos inválidos");
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    void settings() {
        body.removeAllViews();
        ScrollView sv = new ScrollView(this);
        LinearLayout box = vbox(14);
        box.addView(title("Ajustes financieros"));

        EditText income = num("Ingreso mensual esperado", sp.getString("income", "2000"));
        EditText opening = num("Saldo libre al inicio del mes", sp.getString("opening", "0"));
        EditText minimum = num("Saldo mínimo intocable", sp.getString("minimum", "0"));
        EditText rate = num("Ahorro objetivo (ej. 0.20 = 20%)", sp.getString("saveRate", "0.20"));

        box.addView(income);
        box.addView(opening);
        box.addView(minimum);
        box.addView(rate);

        Button save = primary("GUARDAR AJUSTES");
        save.setOnClickListener(v -> {
            try {
                double rr = Double.parseDouble(rate.getText().toString().replace(",", "."));
                if (rr < 0 || rr > 0.8) throw new IllegalArgumentException();

                sp.edit()
                        .putString("income", income.getText().toString())
                        .putString("opening", opening.getText().toString())
                        .putString("minimum", minimum.getText().toString())
                        .putString("saveRate", rate.getText().toString())
                        .apply();

                toast("Ajustes guardados");
                dashboard();
            } catch (Exception e) {
                toast("Revisa los valores");
            }
        });
        box.addView(save);

        box.addView(block(
                "Cómo interpreta tu dinero",
                "Saldo visible = saldo inicial + entradas - salidas." +
                        "\nGastable = lo que queda después de reservar pagos mensuales, ahorro objetivo y saldo intocable." +
                        "\nEl límite diario y semanal se recalcula según los días que faltan del mes."
        ));

        sv.addView(box);
        body.addView(sv);
    }

    Snap snapshot() {
        LocalDate now = LocalDate.now();
        YearMonth ym = YearMonth.from(now);
        String start = ym.atDay(1).toString();
        String end = ym.atEndOfMonth().toString();

        Snap s = new Snap();
        s.expected = d(sp.getString("income", "2000"));
        s.opening = d(sp.getString("opening", "0"));
        s.minimum = d(sp.getString("minimum", "0"));
        double rate = d(sp.getString("saveRate", "0.20"));

        s.income = db.sum("Ingreso", start, end);
        s.expenses = db.sum("Gasto", start, end);
        s.savings = db.sum("Ahorro", start, end);
        s.debt = db.sum("Pago deuda", start, end);
        s.loans = db.sum("Préstamo recibido", start, end);
        s.collections = db.sum("Cobro", start, end);
        s.withdraw = db.sum("Retiro ahorro", start, end);
        s.lent = db.sum("Dinero prestado", start, end);

        s.visible = s.opening + s.income + s.collections + s.loans + s.withdraw
                - s.expenses - s.savings - s.debt - s.lent;

        for (Pay p : db.pays()) {
            s.pending += Math.max(
                    0,
                    p.amount - db.sumExact(p.type, p.concept, start, end)
            );
        }

        double incomeBase = Math.max(s.expected, s.income);
        s.savePending = Math.max(0, incomeBase * rate - s.savings);

        s.endProjection = s.opening + incomeBase + s.collections + s.withdraw
                - s.expenses - s.debt - s.lent
                - s.pending - s.savePending - s.minimum;

        s.spendable = Math.max(0, s.endProjection);
        s.days = Math.max(1, ym.lengthOfMonth() - now.getDayOfMonth() + 1);
        s.daily = s.spendable / s.days;
        s.weekly = Math.min(s.spendable, s.daily * 7);

        if (s.endProjection < 0) {
            s.verdict = "NO ALCANZA CON EL PLAN ACTUAL";
            s.advice = "Te faltan aproximadamente S/ " + m(-s.endProjection) +
                    " para cerrar el mes respetando tus pagos, ahorro y saldo mínimo. " +
                    "Prioriza vivienda, alimentación, transporte de trabajo, salud y deudas; " +
                    "reduce primero ocio, compras postergables y viajes.";
        } else if (s.spendable < 100) {
            s.verdict = "ALCANZA, PERO ESTÁS MUY AJUSTADO";
            s.advice = "Tu margen libre es pequeño. Usa como techo S/ " + m(s.daily) +
                    " por día y evita gastos no esenciales hasta asegurar los pagos del mes.";
        } else {
            s.verdict = "VAS EN UN RANGO SOSTENIBLE";
            s.advice = "Puedes usar como techo aproximado S/ " + m(s.daily) +
                    " por día o S/ " + m(s.weekly) +
                    " por semana. Este límite ya reserva tus pagos, tu ahorro objetivo y tu saldo intocable.";
        }

        if (s.loans > 0) {
            s.advice += "\n\nImportante: los préstamos recibidos no se consideran ingreso sostenible.";
        }

        return s;
    }

    void copyWeek() {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        String tsv = exportText(db.range(monday.toString(), sunday.toString()), "\t");

        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Excel", tsv));
        toast("Semana copiada. Pégala en Excel desde la primera celda.");
    }

    void exportWeek() {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        pendingExport = exportText(db.range(monday.toString(), sunday.toString()), ",");

        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType("text/csv");
        i.putExtra(Intent.EXTRA_TITLE, "movimientos_" + monday + "_a_" + sunday + ".csv");
        startActivityForResult(i, 10);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 10 && res == RESULT_OK && data != null) {
            try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                if (out == null) throw new IllegalStateException("Sin destino");
                out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                out.write(pendingExport.getBytes(StandardCharsets.UTF_8));
                toast("CSV exportado");
            } catch (Exception e) {
                toast("No se pudo exportar");
            }
        }
    }

    String exportText(List<Rec> list, String separator) {
        StringBuilder out = new StringBuilder();
        out.append(join(
                separator,
                "Fecha", "Tipo", "Categoría", "Concepto", "Monto (S/)",
                "Clasificación", "Naturaleza", "Medio",
                "Destino / persona / meta", "Mes", "Nota"
        )).append('\n');

        for (Rec r : list) {
            out.append(join(
                    separator,
                    r.date, r.type, r.cat, r.concept, m(r.amount),
                    r.cls, r.nat, r.med, r.dest, r.month, r.note
            )).append('\n');
        }
        return out.toString();
    }

    String join(String separator, String... values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(separator);
            String x = values[i] == null ? "" : values[i];

            if (",".equals(separator)) {
                x = x.replace("\"", "\"\"");
                if (x.contains(",") || x.contains("\n") || x.contains("\"")) {
                    x = "\"" + x + "\"";
                }
            } else {
                x = x.replace("\t", " ").replace("\n", " ");
            }
            out.append(x);
        }
        return out.toString();
    }

    static class Rec {
        long id;
        String date, type, cat, concept, cls, nat, med, dest, month, note;
        double amount;
    }

    static class Pay {
        long id;
        String type, concept, cat;
        double amount;
        int day;
    }

    static class Snap {
        double expected, opening, minimum, income, expenses, savings, debt, loans;
        double collections, withdraw, lent, visible, pending, savePending;
        double endProjection, spendable, daily, weekly;
        int days;
        String verdict, advice;
    }

    static class DB extends SQLiteOpenHelper {
        DB(Context c) {
            super(c, "finance.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase d) {
            d.execSQL("CREATE TABLE mov(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "date TEXT,type TEXT,cat TEXT,concept TEXT,amount REAL," +
                    "cls TEXT,nat TEXT,med TEXT,dest TEXT,month TEXT,note TEXT)");
            d.execSQL("CREATE TABLE pay(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "type TEXT,concept TEXT,cat TEXT,amount REAL,day INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase d, int oldVersion, int newVersion) {
        }

        void add(Rec r) {
            getWritableDatabase().insert("mov", null, cv(r));
        }

        void update(Rec r) {
            getWritableDatabase().update("mov", cv(r), "id=?", new String[]{String.valueOf(r.id)});
        }

        void del(long id) {
            getWritableDatabase().delete("mov", "id=?", new String[]{String.valueOf(id)});
        }

        ContentValues cv(Rec r) {
            ContentValues c = new ContentValues();
            c.put("date", r.date);
            c.put("type", r.type);
            c.put("cat", r.cat);
            c.put("concept", r.concept);
            c.put("amount", r.amount);
            c.put("cls", r.cls);
            c.put("nat", r.nat);
            c.put("med", r.med);
            c.put("dest", r.dest);
            c.put("month", r.month);
            c.put("note", r.note);
            return c;
        }

        Rec one(long id) {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT * FROM mov WHERE id=?",
                    new String[]{String.valueOf(id)}
            );
            Rec r = c.moveToFirst() ? rec(c) : null;
            c.close();
            return r;
        }

        List<Rec> range(String start, String end) {
            ArrayList<Rec> list = new ArrayList<>();
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT * FROM mov WHERE date BETWEEN ? AND ? ORDER BY date DESC,id DESC",
                    new String[]{start, end}
            );
            while (c.moveToNext()) list.add(rec(c));
            c.close();
            return list;
        }

        Rec rec(Cursor c) {
            Rec r = new Rec();
            r.id = c.getLong(c.getColumnIndexOrThrow("id"));
            r.date = c.getString(c.getColumnIndexOrThrow("date"));
            r.type = c.getString(c.getColumnIndexOrThrow("type"));
            r.cat = c.getString(c.getColumnIndexOrThrow("cat"));
            r.concept = c.getString(c.getColumnIndexOrThrow("concept"));
            r.amount = c.getDouble(c.getColumnIndexOrThrow("amount"));
            r.cls = c.getString(c.getColumnIndexOrThrow("cls"));
            r.nat = c.getString(c.getColumnIndexOrThrow("nat"));
            r.med = c.getString(c.getColumnIndexOrThrow("med"));
            r.dest = c.getString(c.getColumnIndexOrThrow("dest"));
            r.month = c.getString(c.getColumnIndexOrThrow("month"));
            r.note = c.getString(c.getColumnIndexOrThrow("note"));
            return r;
        }

        double sum(String type, String start, String end) {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT COALESCE(SUM(amount),0) FROM mov WHERE type=? AND date BETWEEN ? AND ?",
                    new String[]{type, start, end}
            );
            c.moveToFirst();
            double x = c.getDouble(0);
            c.close();
            return x;
        }

        double sumExact(String type, String concept, String start, String end) {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT COALESCE(SUM(amount),0) FROM mov " +
                            "WHERE type=? AND concept=? AND date BETWEEN ? AND ?",
                    new String[]{type, concept, start, end}
            );
            c.moveToFirst();
            double x = c.getDouble(0);
            c.close();
            return x;
        }

        void addPay(Pay p) {
            ContentValues c = new ContentValues();
            c.put("type", p.type);
            c.put("concept", p.concept);
            c.put("cat", p.cat);
            c.put("amount", p.amount);
            c.put("day", p.day);
            getWritableDatabase().insert("pay", null, c);
        }

        void delPay(long id) {
            getWritableDatabase().delete("pay", "id=?", new String[]{String.valueOf(id)});
        }

        List<Pay> pays() {
            ArrayList<Pay> list = new ArrayList<>();
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT * FROM pay ORDER BY day,concept",
                    null
            );
            while (c.moveToNext()) {
                Pay p = new Pay();
                p.id = c.getLong(0);
                p.type = c.getString(1);
                p.concept = c.getString(2);
                p.cat = c.getString(3);
                p.amount = c.getDouble(4);
                p.day = c.getInt(5);
                list.add(p);
            }
            c.close();
            return list;
        }
    }

    LinearLayout vbox(int padding) {
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);
        x.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
        return x;
    }

    LinearLayout row() {
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(LinearLayout.HORIZONTAL);
        return x;
    }

    LinearLayout.LayoutParams wt() {
        return new LinearLayout.LayoutParams(0, -2, 1f);
    }

    TextView txt(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, 1);
        t.setPadding(dp(10), dp(8), dp(10), dp(8));
        return t;
    }

    TextView title(String s) {
        return txt(s, 22, TEXT, true);
    }

    TextView label(String s) {
        return txt(s, 12, MUTED, false);
    }

    TextView card(String s, int size, int color, boolean bold) {
        TextView t = txt(s, size, color, bold);
        t.setBackgroundColor(CARD);
        t.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(6), 0, dp(6));
        t.setLayoutParams(p);
        return t;
    }

    TextView block(String heading, String text) {
        return card(heading + "\n" + text, 14, TEXT, false);
    }

    TextView kpi(String heading, double value, int color) {
        return card(heading + "\nS/ " + m(value), 14, color, true);
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        return b;
    }

    Button primary(String s) {
        Button b = btn(s);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.rgb(25, 118, 210));
        return b;
    }

    EditText input(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setSingleLine(false);
        return e;
    }

    EditText num(String hint, String value) {
        EditText e = input(hint, value);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    Spinner spin(String[] values, String selected) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                values
        );
        s.setAdapter(ad);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(selected)) {
                s.setSelection(i);
                break;
            }
        }
        return s;
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    double d(String x) {
        try {
            return Double.parseDouble(x.replace(",", "."));
        } catch (Exception e) {
            return 0;
        }
    }

    static String m(double x) {
        return String.format(Locale.US, "%.2f", x);
    }
}
