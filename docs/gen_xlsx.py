import sys, os
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.worksheet.datavalidation import DataValidation
from openpyxl.formatting.rule import FormulaRule
from openpyxl.utils import get_column_letter

AZUL   = "1F4E79"
AZULC  = "DEEAF6"
GRIS   = "595959"
AMAR   = "FFF7D6"   # celda por contestar
BORDE  = Side(style="thin", color="BFBFBF")
BOX    = Border(left=BORDE, right=BORDE, top=BORDE, bottom=BORDE)

# ---------------------------------------------------------------- listas
LISTAS = {
    "SINO":      ["SÍ", "NO"],
    "ANIO":      ["2017", "2018", "2019", "2020", "2021", "2022", "2023", "Otro"],
    "EXTERNOS":  ["Sí cuentan", "Se excluyen", "Aparte / en su propia gráfica"],
    "INCLUSION": ["Separado en todas las gráficas", "Sumado al total"],
    "PERIODO":   ["De 2017 a 2026 de corrido", "Solo de 2020 en adelante"],
    "POLI":      ["Causa de atención", "Lesión músculo-esquelética"],
    "NUEVAS":    ["No se hacían", "Se contaban dentro de otra causa"],
    "AGRUPA":    ["Predio", "Cuenta", "Ambos"],
    "AGENCIA":   ["Comparar los 10 años", "Arrancar en 2022", "No hace falta esa gráfica"],
    "INTERNAS":  ["Cuentan en el ausentismo", "Aparte", "No se cargan"],
    "CRUCE":     ["Todos en el año en que inicia", "Repartidos entre los dos años"],
    "COSTO":     ["Entra al portal", "Fuera de alcance"],
    "EXAMEN":    ["Tres tipos del mismo examen", "Tres procesos distintos"],
    "DICTCAP":   ["Se sigue capturando hoy", "Ya no se captura"],
    "ANTIDOP":   ["Se grafica el desenlace", "Solo positivo / negativo"],
}

# ------------------------------------------------- contenido: (tipo, ...)
# ("SEC", letra, titulo)
# ("P", num, pregunta, lista|None, pista)
FILAS = [
    ("SEC", "A", "Alcance"),
    ("P", "1",    "¿Cuáles reportes deben alimentar las gráficas?", None,
     "Conteste SÍ o NO en cada uno de los nueve renglones de abajo"),
    ("P", "1.1",  "Atenciones diarias", "SINO", ""),
    ("P", "1.2",  "Examen de ingreso", "SINO", ""),
    ("P", "1.3",  "Examen periódico", "SINO", ""),
    ("P", "1.4",  "Examen pos-incapacidad", "SINO", ""),
    ("P", "1.5",  "Incapacidades", "SINO", ""),
    ("P", "1.6",  "Accidentabilidad", "SINO", ""),
    ("P", "1.7",  "Antidoping", "SINO", ""),
    ("P", "1.8",  "Maternidad", "SINO", ""),
    ("P", "1.9",  "Consumibles", "SINO", ""),
    ("P", "2",    "¿Desde qué año se carga la historia?", "ANIO",
     "Si elige Otro, escriba el año en Observaciones"),
    ("P", "3",    "Atenciones a externos o clientes que no son empleados", "EXTERNOS", ""),
    ("P", "4",    "El personal de INCLUSIÓN", "INCLUSION", ""),

    ("SEC", "B", "Causas de atención"),
    ("P", "5",    "La gráfica de causas debe comparar", "PERIODO",
     "Si responde «solo desde 2020», ya no hace falta contestar la 6"),
    ("P", "6",    "¿Son la misma cosa con otro nombre?", None,
     "SÍ o NO en cada renglón. Si es NO, escriba en Observaciones a dónde se fue"),
    ("P", "6.1",  "CEFALEA  →  NEUROLÓGICO", "SINO", ""),
    ("P", "6.2",  "DIABETES MELLITUS  →  ENDOCRINO", "SINO", ""),
    ("P", "6.3",  "DIABETES MELLITUS  →  TOMA DE GLUCOSA", "SINO", ""),
    ("P", "6.4",  "DISMENORREA  →  GINECOLÓGICO", "SINO", ""),
    ("P", "6.5",  "CONTROL PESO  →  (no sabemos a dónde se fue)", None,
     "Escriba en Observaciones con qué causa se cuenta hoy"),
    ("P", "6.6",  "NAZAL  →  RESPIRATORIO", "SINO", ""),
    ("P", "6.7",  "SEGUIMIENTO SALUD + PROMOCIÓN SALUD  →  VACUNA / MÉTODO PF / SEG SALUD", "SINO", ""),
    ("P", "7",    "POLICONTUNDIDO está en dos listas. Para no contarlo doble, contar desde", "POLI", ""),
    ("P", "8",    "NOM 035, FISIOTERAPIA y TOMA DE GLUCOSA, antes de aparecer", "NUEVAS",
     "Si se contaban en otra, diga cuál en Observaciones"),

    ("SEC", "C", "Predio y cuenta"),
    ("P", "9",    "Las gráficas se agrupan por", "AGRUPA",
     "En CUENTA hay clientes (CLARINS, PUIG) y predios (MIKELS, SIGLO XXI, SMO) mezclados: diga en Observaciones cómo se distinguen"),
    ("P", "10",   "Gráfica por agencia", "AGENCIA",
     "Hasta 2021 había muchas (GIN, MAYORAL, HQC); desde 2022 solo GLI u OTRO"),

    ("SEC", "D", "Incapacidades y accidentes"),
    ("P", "11",   "Incapacidades INTERNAS (las que no son del IMSS)", "INTERNAS", ""),
    ("P", "12",   "Incapacidad que cruza el fin de año: los días se cuentan", "CRUCE", ""),
    ("P", "13",   "Costo en pesos de la incapacidad", "COSTO", ""),
    ("P", "14",   "¿Qué diferencia a RT1 de RT2?", None,
     "Escriba la respuesta en Observaciones"),
    ("P", "14.1", "Entra al indicador de accidentabilidad: RT1", "SINO", ""),
    ("P", "14.2", "Entra al indicador de accidentabilidad: RT2", "SINO", ""),
    ("P", "14.3", "Entra al indicador de accidentabilidad: Enfermedad profesional", "SINO", ""),
    ("P", "14.4", "Entra al indicador de accidentabilidad: Incidente (sin lesión)", "SINO", ""),
    ("P", "14.5", "Entra al indicador de accidentabilidad: Accidente de trayecto", "SINO", ""),

    ("SEC", "E", "Exámenes y antidoping"),
    ("P", "15",   "Ingreso, periódico y pos-incapacidad son", "EXAMEN", ""),
    ("P", "15.1", "El dictamen APTO / APTO CONDICIONADO / NO APTO", "DICTCAP",
     "Aparece en los archivos desde 2017"),
    ("P", "15.2", "Ese dictamen debe graficarse en el portal", "SINO", ""),
    ("P", "16",   "Antidoping positivo: el desenlace (MONITOREO / BAJA / NO CONTRATADO)", "ANTIDOP", ""),
]

wb = openpyxl.Workbook()

# ---------------------------------------------------------- hoja Listas
hl = wb.create_sheet("Listas")
rangos = {}
for i, (nom, vals) in enumerate(LISTAS.items(), start=1):
    col = get_column_letter(i)
    hl.cell(row=1, column=i, value=nom).font = Font(bold=True)
    for j, v in enumerate(vals, start=2):
        hl.cell(row=j, column=i, value=v)
    rangos[nom] = "Listas!${c}$2:${c}${n}".format(c=col, n=len(vals) + 1)
    hl.column_dimensions[col].width = 32
hl.sheet_state = "hidden"

# ----------------------------------------------------- hoja Cuestionario
ws = wb.active
ws.title = "Cuestionario"
ws.sheet_view.showGridLines = False

ws.column_dimensions["A"].width = 7
ws.column_dimensions["B"].width = 72
ws.column_dimensions["C"].width = 34
ws.column_dimensions["D"].width = 52

# encabezado
ws["A1"] = "Carga historica al Portal de Salud"
ws["A1"].font = Font(bold=True, size=18, color=AZUL)
ws["A2"] = "Cuestionario para Servicio Médico · 16 preguntas. Elija la respuesta en la lista desplegable de la columna Respuesta."
ws["A2"].font = Font(size=10, italic=True, color=GRIS)
ws.merge_cells("A1:D1"); ws.merge_cells("A2:D2")

etiquetas = ["Predio(s):", "Quién responde:", "Fecha:"]
for i, e in enumerate(etiquetas):
    r = 4 + i
    ws.cell(row=r, column=1, value=e).font = Font(bold=True, size=10)
    ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=2)
    c = ws.cell(row=r, column=3)
    c.fill = PatternFill("solid", fgColor=AMAR); c.border = BOX

HDR = 8
for j, titulo in enumerate(["#", "Pregunta", "Respuesta", "Observaciones"], start=1):
    c = ws.cell(row=HDR, column=j, value=titulo)
    c.font = Font(bold=True, size=11, color="FFFFFF")
    c.fill = PatternFill("solid", fgColor=AZUL)
    c.alignment = Alignment(horizontal="center", vertical="center")
    c.border = BOX
ws.freeze_panes = "A9"

dvs = {}
for nom, ref in rangos.items():
    dv = DataValidation(type="list", formula1=ref, allow_blank=True, showDropDown=False)
    dv.error = "Elija una opción de la lista."
    dv.errorTitle = "Respuesta no válida"
    dv.prompt = "Haga clic en la flecha y elija una opción."
    dv.promptTitle = "Seleccione"
    ws.add_data_validation(dv)
    dvs[nom] = dv

r = HDR + 1
resp_celdas = []
for item in FILAS:
    if item[0] == "SEC":
        _, letra, titulo = item
        ws.cell(row=r, column=1, value=letra)
        ws.cell(row=r, column=2, value=titulo)
        for col in range(1, 5):
            c = ws.cell(row=r, column=col)
            c.fill = PatternFill("solid", fgColor=AZULC)
            c.font = Font(bold=True, size=12, color=AZUL)
            c.border = BOX
        ws.row_dimensions[r].height = 24
        r += 1
        continue

    _, num, texto, lista, pista = item
    ws.cell(row=r, column=1, value=num).alignment = Alignment(horizontal="center", vertical="top")
    ws.cell(row=r, column=1).font = Font(bold=True, size=10, color=AZUL)
    cb = ws.cell(row=r, column=2, value=texto)
    cb.alignment = Alignment(wrap_text=True, vertical="top")
    cb.font = Font(size=11, bold=("." not in num))

    cr = ws.cell(row=r, column=3)
    cr.alignment = Alignment(vertical="top")
    if lista:
        dvs[lista].add(cr)
        cr.fill = PatternFill("solid", fgColor=AMAR)
        resp_celdas.append(cr.coordinate)
    else:
        cr.value = "-"
        cr.font = Font(color="BFBFBF")
        cr.alignment = Alignment(horizontal="center", vertical="top")

    cd = ws.cell(row=r, column=4, value=pista or None)
    cd.alignment = Alignment(wrap_text=True, vertical="top")
    if pista:
        cd.font = Font(size=9, italic=True, color=GRIS)
    if not lista and pista:
        cd.fill = PatternFill("solid", fgColor=AMAR)

    for col in range(1, 5):
        ws.cell(row=r, column=col).border = BOX
    ws.row_dimensions[r].height = 30 if len(texto) > 60 or len(pista) > 60 else 20
    r += 1

# resalta en rojo suave lo que siga sin contestar
if resp_celdas:
    rojo = PatternFill("solid", fgColor="FCE4E4")
    ws.conditional_formatting.add(
        "C%d:C%d" % (HDR + 1, r - 1),
        FormulaRule(formula=['AND(C%d<>"-",C%d="")' % (HDR + 1, HDR + 1)], fill=rojo, stopIfTrue=False))

r += 1
ws.cell(row=r, column=2,
        value="Si solo pueden contestar tres, que sean la 1, la 5 y la 9.").font = Font(bold=True, size=11, color=AZUL)
r += 1
ws.cell(row=r, column=2,
        value=("Elaborado a partir de una muestra de 30 archivos (3 por cada uno de los 10 reportes) "
               "en años y predios distintos, de 2017 a 2026. Universo revisado: 1,072 archivos, 34 predios.")
        ).font = Font(size=9, italic=True, color=GRIS)

destino = sys.argv[1]
wb.save(destino)
print("escrito:", destino)
print("preguntas con lista desplegable:", len(resp_celdas))
