"""
Examen médico (el migrado del PHP, docs/plan-pruebas-flujo-principal.html §5):
  5.1 carga del examen y de una sección · 5.2 guardar solo lo abierto · 5.3 ir y venir sin perder
  · 5.8 imprimir expediente.
Escribe en el WS Servcio/Medico de QA para el NSS de pruebas (sección Exploración física).
"""
import time

import pytest

from conftest import esperar


def _abrir_examen(pagina, cfg):
    pagina.goto(f"{cfg.base}/nss?nss={cfg.nss}")
    # El enlace vive en el submenu "Expediente General" (colapsado): se dispara por JS
    pagina.wait_for_selector("[data-action='examen']", state="attached", timeout=90000)
    pagina.evaluate("document.querySelector(\"[data-action='examen']\").click()")
    pagina.wait_for_selector("#examenAccordion", timeout=90000)


def _abrir_seccion(pagina, nombre):
    """Expande el grupo que contiene la sección y da clic en su pestaña (carga perezosa)."""
    tab = pagina.locator(f"#examenAccordion button[data-seccion='{nombre}']")
    assert tab.count() == 1, f"No existe la sección {nombre}"
    grupo = tab.locator("xpath=ancestor::div[contains(@class,'accordion-item')]")
    boton_grupo = grupo.locator(".accordion-button")
    if "collapsed" in (boton_grupo.get_attribute("class") or ""):
        boton_grupo.click()
        grupo.locator(".accordion-collapse.show").wait_for(timeout=10000)
    tab.click()
    pane = pagina.locator("#" + tab.get_attribute("aria-controls"))
    pane.locator("input, select, textarea").first.wait_for(timeout=60000)
    return pane


def test_01_carga_examen_y_seccion(pagina, cfg):
    _abrir_examen(pagina, cfg)
    grupos = pagina.locator("#examenAccordion .accordion-item").count()
    assert grupos >= 5, f"Se esperaban varios grupos del acordeón, hay {grupos}"
    pane = _abrir_seccion(pagina, "Exploración física")
    assert pane.locator("[name='SERV_MED_EXPLORACION_FISICA.PESO']").count() == 1


def test_02_guardar_solo_lo_abierto(pagina, cfg):
    _abrir_examen(pagina, cfg)
    pane = _abrir_seccion(pagina, "Exploración física")
    marca = "E2E " + time.strftime("%H:%M:%S")
    pane.locator("[name='SERV_MED_EXPLORACION_FISICA.PESO']").fill("81")
    pane.locator("[name='SERV_MED_EXPLORACION_FISICA.TALLA']").fill("1.76")
    pane.locator("[name='SERV_MED_EXPLORACION_FISICA.LENGUAJE']").fill(marca)
    pagina.click("#examenGuardarBtn")
    esperar(lambda: pagina.locator("#examenGuardarResult").inner_text().strip() != "", timeout=120, que="resultado de Guardar examen")
    res = pagina.locator("#examenGuardarResult")
    assert "text-danger" not in (res.get_attribute("class") or ""), "Guardar falló: " + res.inner_text()
    # Recargar y comprobar que persistió en el WS
    _abrir_examen(pagina, cfg)
    pane = _abrir_seccion(pagina, "Exploración física")
    assert pane.locator("[name='SERV_MED_EXPLORACION_FISICA.PESO']").input_value() == "81"
    assert pane.locator("[name='SERV_MED_EXPLORACION_FISICA.LENGUAJE']").input_value() == marca


def test_03_ir_y_venir_sin_perder(pagina, cfg):
    _abrir_examen(pagina, cfg)
    pane = _abrir_seccion(pagina, "Exploración física")
    pane.locator("[name='SERV_MED_EXPLORACION_FISICA.FC']").fill("72")
    # Otra sección del mismo grupo y volver
    _abrir_seccion(pagina, "Cráneo")
    _abrir_seccion(pagina, "Exploración física")
    assert pane.locator("[name='SERV_MED_EXPLORACION_FISICA.FC']").input_value() == "72"


def test_04_imprimir_expediente(pagina, cfg):
    _abrir_examen(pagina, cfg)
    with pagina.context.expect_page(timeout=60000) as nueva:
        pagina.locator("button[title^='Abre el expediente completo']").click()
    doc = nueva.value
    doc.wait_for_load_state("load", timeout=120000)
    texto = doc.locator("body").inner_text()
    assert cfg.nss in texto, "El expediente impreso no trae el NSS"
    assert "EXPLORACI" in texto.upper()
    doc.close()
