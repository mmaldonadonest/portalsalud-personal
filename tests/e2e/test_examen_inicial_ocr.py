"""
Flujo completo del examen médico inicial con OCR simulado (docs/guia-procesos-examen-inicial-ocr.html §2-§5):

  OCR crea solicitud (201) -> campanita -> ficha del candidato -> médico abre el examen (EN_CAPTURA,
  asignado) -> dictamen + ICD + firma -> Finalizar -> bandeja FINALIZADO -> webhook firmado llega al
  receptor -> consulta de respaldo dice FINALIZADO/notificado.

Las pruebas van en orden y comparten estado en `estado`. TODAS reciben `pagina` aunque no la usen:
pytest reordena por fixtures de sesión y separaría las de API de las de navegador.
Deja datos en QA para el NSS de pruebas: solicitud + examen tipo Admisión en el WS.
"""
import time

import pytest

from conftest import csrf_de, esperar
from ocr_sim import OcrCliente, Receptor

estado = {}


@pytest.fixture(scope="module")
def receptor(cfg):
    r = Receptor(cfg.receptor_puerto, cfg.hmac_secret).iniciar()
    yield r
    r.detener()


@pytest.fixture(scope="module")
def ocr(cfg):
    return OcrCliente(cfg.base, cfg.api_key)


def _referencia():
    return "E2E-" + time.strftime("%Y%m%d-%H%M%S")


def _solicitud(cfg, receptor, ref):
    return {
        "referenciaOcr": ref,
        "nss": cfg.nss,
        "curp": "MACM830923HMCLLG01",
        "nombre": "Prueba",
        "apellidoPaterno": "Automatizada",
        "apellidoMaterno": "Playwright",
        "puestoSolicitado": "Auxiliar de almacén",
        "cuenta": "E2E",
        "empresa": "Onest",
        "telefono": "5512345678",
        "fechaCita": time.strftime("%Y-%m-%d"),
        "callbackUrl": receptor.url,
    }


# ---------------------------------------------------------------------------
# 0. Limpieza: cancelar solicitudes vivas del NSS de corridas anteriores, para que la de
#    esta corrida sea la que el portal asocie al NSS (activaPorNss toma la primera viva).
# ---------------------------------------------------------------------------
def test_00_cancelar_solicitudes_vivas_previas(pagina, cfg, ocr):
    pagina.goto(f"{cfg.base}/examen-inicial/bandeja?nss={cfg.nss}")
    assert pagina.locator("h4.main-title").inner_text().startswith("Bandeja"), "La bandeja no abrió: ¿el rol tiene EXAMEN_INICIAL_BANDEJA y corriste examen-inicial-bandeja.sql en QA?"
    vivas = pagina.locator("tr[data-id]").evaluate_all(
        "trs => trs.filter(t => ['PENDIENTE','EN_CAPTURA'].includes(t.dataset.estatus)).map(t => t.dataset.id)")
    for sid in vivas:
        code, _ = ocr.cancelar(int(sid), "limpieza E2E")
        assert code in (200, 409), f"cancelar {sid} -> {code}"
    estado["canceladas_previas"] = len(vivas)


# ---------------------------------------------------------------------------
# 1. API: 401 / 400 / 201 / 200
# ---------------------------------------------------------------------------
def test_01_api_llave_mala_401(pagina, cfg, ocr, receptor):
    code, body = ocr.crear(_solicitud(cfg, receptor, _referencia()), api_key="llave-mala")
    assert code == 401 and body["error"] == "NO_AUTORIZADO"


def test_02_api_obligatorios_400(pagina, cfg, ocr, receptor):
    s = _solicitud(cfg, receptor, _referencia())
    del s["curp"]
    s["nss"] = "123"
    code, body = ocr.crear(s)
    assert code == 400 and body["error"] == "SOLICITUD_INVALIDA"
    detalles = " ".join(body["detalles"])
    assert "curp" in detalles and "nss" in detalles


def test_03_api_alta_201(pagina, cfg, ocr, receptor):
    ref = _referencia()
    code, body = ocr.crear(_solicitud(cfg, receptor, ref))
    if code == 409:
        pytest.fail(f"El NSS {cfg.nss} tiene examen vigente como empleado (candado): {body}. "
                    "Corre tests/e2e/preparar-nss-qa.local.sql en la base del portal de QA y vuelve a intentar.")
    assert code == 201, body
    assert body["estatus"] == "PENDIENTE" and body["nss"] == cfg.nss and body["referenciaOcr"] == ref
    assert body["caduca"], "caduca vacío"
    estado["id"] = body["solicitudId"]
    estado["ref"] = ref


def test_04_api_idempotente_200(pagina, cfg, ocr, receptor):
    code, body = ocr.crear(_solicitud(cfg, receptor, estado["ref"]))
    assert code == 200 and body["solicitudId"] == estado["id"]


# ---------------------------------------------------------------------------
# 2. Aviso y ficha
# ---------------------------------------------------------------------------
def test_05_campanita_tiene_la_notificacion(pagina, cfg):
    pagina.goto(cfg.base + "/home")
    r = pagina.request.get(cfg.base + "/api/notificaciones", headers={"Accept": "application/json"})
    assert r.ok, r.status
    items = r.json()
    lista = items if isinstance(items, list) else items.get("items") or items.get("pendientes") or []
    textos = " | ".join(str(i) for i in lista)
    assert "Prueba Automatizada" in textos or cfg.nss in textos, f"La campanita no trae la solicitud: {textos[:300]}"


def test_06_ficha_candidato_y_banner(pagina, cfg):
    pagina.goto(f"{cfg.base}/nss?nss={cfg.nss}")
    # El mismo texto vive tambien en la campanita (oculta): se busca el banner del resultado
    pagina.wait_for_selector("#moduleContent .alert-warning:has-text('Examen médico inicial pendiente'), .alert-warning:has-text('Examen médico inicial pendiente') >> visible=true", timeout=60000)
    cuerpo = pagina.locator("body").inner_text()
    # Candidato que no está en el biométrico: la ficha sale de la solicitud; si sí está, sale del WS
    assert "Prueba" in cuerpo or cfg.nss in cuerpo


# ---------------------------------------------------------------------------
# 3. Captura y finalización
# ---------------------------------------------------------------------------
def _abrir_examen_inicial(pagina, cfg):
    pagina.goto(f"{cfg.base}/nss?nss={cfg.nss}&abrir=examen-inicial")
    pagina.wait_for_selector("#exIni", timeout=90000)
    bloqueado = pagina.locator("#exIni .alert-danger:has-text('bloqueado')")
    if bloqueado.count():
        pytest.fail("Candado: " + bloqueado.first.inner_text())


def test_07_abrir_pasa_a_en_captura_y_asigna(pagina, cfg):
    _abrir_examen_inicial(pagina, cfg)
    assert pagina.locator("#exIniAtendido").count() == 0, "El examen aparece atendido por otro usuario"
    pagina.goto(f"{cfg.base}/examen-inicial/bandeja?nss={cfg.nss}&estatus=EN_CAPTURA")
    fila = pagina.locator(f"tr[data-id='{estado['id']}']")
    assert fila.count() == 1, "La solicitud no está EN_CAPTURA en la bandeja"
    assert cfg.usuario.lower() in fila.inner_text().lower(), "La columna Atiende no trae al usuario que abrió"


def test_08_autoguardado_persiste(pagina, cfg):
    _abrir_examen_inicial(pagina, cfg)
    pagina.evaluate("exIniIr(7)")  # Exploración física
    peso = pagina.locator("#exIniForm [name$='.PESO']").first
    talla = pagina.locator("#exIniForm [name$='.TALLA']").first
    valor = "8" + time.strftime("%M.%S").lstrip("0").replace(".", "") [:1] + "." + time.strftime("%S")  # p. ej. 83.07: distinto en cada corrida
    peso.fill(valor)
    talla.fill("1.75")
    talla.dispatch_event("change")
    # Cambiar de hoja dispara el autoguardado: esperar la respuesta real del POST /borrador
    # (el sello #exIniGuardado ya trae la hora del borrador anterior, no sirve como señal)
    pagina.evaluate("exIniIr(8)")
    # Cambiar de hoja dispara el autoguardado; exIniAutosave devuelve la cola de envíos y
    # Playwright espera la promesa, así que al regresar ya está todo en el servidor.
    pagina.evaluate("() => exIniAutosave()")
    # Volver a abrir: debe seguir ahí
    _abrir_examen_inicial(pagina, cfg)
    pagina.evaluate("exIniIr(7)")
    assert pagina.locator("#exIniForm [name$='.PESO']").first.input_value() == valor


def test_09_finalizar_no_apto(pagina, cfg):
    # NO APTO a propósito: es el único dictamen que deja el candado abierto para un empleado
    # (ExamenInicialService.candado), así la suite se puede volver a correr sin tocar la base.
    _abrir_examen_inicial(pagina, cfg)
    pagina.evaluate("exIniIr(10)")  # Cierre
    # Dictamen
    # Los chips del dictamen son toggle: si el borrador ya trae NO APTO, otro clic lo quitaría
    chip = pagina.locator("#exIniForm button[data-val='no_apto']")
    if "activo" not in (chip.get_attribute("class") or ""):
        chip.click()
    assert pagina.locator("#exIniForm [name='SERV_MED_RESULTADO_EXAMEN.DICTAMEN']").input_value() == "no_apto"
    # Diagnóstico ICD: escribir, elegir el primero del catálogo
    pagina.fill("#examenIcdConsult", "fiebre")  # el catalogo ICD de QA esta incompleto: "fiebre" si responde
    pagina.wait_for_selector("#examenIcdResult button[data-icd]", timeout=60000)
    pagina.locator("#examenIcdResult button[data-icd]").first.click()
    assert pagina.locator("#examenDiagnostico").input_value().strip() != ""
    # Firma: trazo en el canvas
    canvas = pagina.locator("#firmaCanvas")
    box = canvas.bounding_box()
    pagina.mouse.move(box["x"] + 30, box["y"] + 80)
    pagina.mouse.down()
    pagina.mouse.move(box["x"] + 120, box["y"] + 40, steps=8)
    pagina.mouse.move(box["x"] + 220, box["y"] + 110, steps=8)
    pagina.mouse.up()
    # Finalizar: el confirm() se acepta solo (fixture); se abre el expediente en pestaña nueva
    try:
        with pagina.context.expect_page(timeout=120000) as nueva:
            pagina.click("#exIniFinalizarBtn")
        doc = nueva.value
    except Exception as ex:
        raise AssertionError("No se abrió el expediente al finalizar. Resultado en pantalla: '"
                             + pagina.locator("#exIniResult").inner_text() + "' · botón: '"
                             + pagina.locator("#exIniFinalizarBtn").inner_text() + "' · " + str(ex)[:120])
    esperar(lambda: pagina.locator("#exIniResult").inner_text().strip() != "", timeout=120, que="resultado de Finalizar")
    resultado = pagina.locator("#exIniResult")
    assert "text-danger" not in (resultado.get_attribute("class") or ""), "Finalizar falló: " + resultado.inner_text()
    # La pestaña abre con "Generando el expediente…" y se navega sola al documento al terminar
    doc.wait_for_url("**/examen/imprimir**", timeout=180000)
    doc.wait_for_load_state("load", timeout=120000)
    assert "DICTAMEN" in doc.locator("body").inner_text().upper()
    doc.close()


def test_10_bandeja_finalizado(pagina, cfg):
    pagina.goto(f"{cfg.base}/examen-inicial/bandeja?nss={cfg.nss}&estatus=FINALIZADO")
    fila = pagina.locator(f"tr[data-id='{estado['id']}']")
    assert fila.count() == 1, "La solicitud no aparece FINALIZADA en la bandeja"
    assert "NO_APTO" in fila.inner_text().upper()


# ---------------------------------------------------------------------------
# 4. Webhook a OCR (simulado) y consulta de respaldo
# ---------------------------------------------------------------------------
def test_11_webhook_llega_firmado(pagina, cfg, receptor):
    r = receptor.esperar_solicitud(estado["id"], timeout=150)
    assert r, "El webhook no llegó al receptor en 150 s (scheduler cada 60 s). Revisa logs/portal-salud-ocr.log [ocr-webhook]"
    assert r["firma_ok"], "X-Firma no coincide con HMAC-SHA256(secreto, cuerpo)"
    j = r["json"]
    assert j["evento"] == "EXAMEN_INICIAL_FINALIZADO"
    assert j["estatus"] == "DICTAMINADO" and j["dictamen"] == "NO_APTO"
    assert j["referenciaOcr"] == estado["ref"] and j["nss"] == cfg.nss
    assert j["fechaDictamen"]
    # documentoUrl solo viaja cuando public-base-url esta configurada (en QA va vacia)
    assert j.get("documentoUrl") is None or j["documentoUrl"].endswith(f"/solicitudes/{estado['id']}/documento")
    for h in ("X-Firma-Tiempo", "X-Firma-V2", "X-Evento"):
        assert h in r["headers"], f"falta cabecera {h}"


def test_12_consulta_respaldo_finalizada(pagina, cfg, ocr):
    code, body = ocr.por_id(estado["id"])
    assert code == 200 and body["estatus"] == "FINALIZADO" and body["dictamen"] == "NO_APTO"
    assert body.get("notificado") is True, "notificado debería ser true después del webhook OK"
    code, body2 = ocr.por_referencia(estado["ref"])
    assert code == 200 and body2["solicitudId"] == estado["id"]


def test_13_cancelar_finalizada_409(pagina, cfg, ocr):
    code, body = ocr.cancelar(estado["id"])
    assert code == 409 and body["error"] == "YA_FINALIZADO"


def test_14_documento_con_llave(pagina, cfg, ocr):
    import urllib.request
    url = f"{cfg.base}/api/integracion/ocr/examen-inicial/solicitudes/{estado['id']}/documento"
    req = urllib.request.Request(url, headers={"X-Api-Key": cfg.api_key})
    with urllib.request.urlopen(req, timeout=60) as r:
        html = r.read().decode("utf-8", "replace")
    assert r.status == 200 and "<html" in html.lower() and cfg.nss in html
