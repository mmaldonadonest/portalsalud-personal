"""
Pruebas E2E (Playwright) del portal contra el Tomcat local apuntando a QA.

Lee TODO de archivos ignorados por git (config/produccion/*.local.properties):
  - e2e.local.properties           -> base, usuario/contrasena del portal (QA), NSS de pruebas
  - portal-salud-qa.local.properties -> ocr.apikey / ocr.hmac.secret (las mismas del WAR -Pqa)
Sin credenciales las pruebas se saltan con un mensaje claro; nunca hay valores por defecto.

Correr:  python -m pytest tests/e2e -v           (headless)
         python -m pytest tests/e2e -v --headed  (viendo el navegador)
"""
import os
import re
import time
from pathlib import Path

import pytest
from playwright.sync_api import Page, BrowserContext

RAIZ = Path(__file__).resolve().parents[2]
CONFIG = RAIZ / "config" / "produccion"


def _props(nombre: str) -> dict:
    ruta = CONFIG / nombre
    if not ruta.exists():
        return {}
    out = {}
    for linea in ruta.read_text(encoding="utf-8").splitlines():
        linea = linea.strip()
        if not linea or linea.startswith("#") or "=" not in linea:
            continue
        k, v = linea.split("=", 1)
        out[k.strip()] = v.strip()
    return out


class Cfg:
    def __init__(self):
        e2e = _props("e2e.local.properties")
        qa = _props("portal-salud-qa.local.properties")
        self.base = e2e.get("e2e.base", "http://localhost:9999/portal-salud").rstrip("/")
        self.usuario = e2e.get("e2e.usuario", "")
        self.password = e2e.get("e2e.password", "")
        self.nss = e2e.get("e2e.nss", "")
        self.receptor_puerto = int(e2e.get("e2e.receptor.puerto", "8099"))
        self.api_key = qa.get("ocr.apikey", "")
        self.hmac_secret = qa.get("ocr.hmac.secret", "")

    def faltantes(self):
        f = []
        if not self.usuario or not self.password:
            f.append("e2e.usuario / e2e.password en config/produccion/e2e.local.properties")
        if not self.nss:
            f.append("e2e.nss en config/produccion/e2e.local.properties")
        if not self.api_key or not self.hmac_secret:
            f.append("ocr.apikey / ocr.hmac.secret en config/produccion/portal-salud-qa.local.properties")
        return f


@pytest.fixture(scope="session")
def cfg() -> Cfg:
    c = Cfg()
    if c.faltantes():
        pytest.skip("Faltan datos para E2E: " + "; ".join(c.faltantes()))
    return c


@pytest.fixture(scope="session")
def browser_context_args(browser_context_args):
    # Pantalla ancha para que el sidebar y la bandeja se vean como en escritorio
    return {**browser_context_args, "viewport": {"width": 1400, "height": 900}, "locale": "es-MX"}


def login(page: Page, cfg: Cfg):
    page.goto(cfg.base + "/login")
    page.fill("#username", cfg.usuario)
    page.fill("#password", cfg.password)
    page.click("button.btn-sign")
    page.wait_for_load_state("networkidle")
    assert "/login" not in page.url, "El login no entró: revisa e2e.usuario / e2e.password (QA, LEGACY_PHP)"


@pytest.fixture(scope="session")
def sesion(browser, browser_context_args, cfg) -> BrowserContext:
    """Una sola sesión del portal para toda la corrida (el login contra ORDS es lento)."""
    ctx = browser.new_context(**browser_context_args)
    page = ctx.new_page()
    login(page, cfg)
    page.close()
    yield ctx
    ctx.close()


@pytest.fixture
def pagina(sesion: BrowserContext) -> Page:
    p = sesion.new_page()
    # Los confirm() del portal (Finalizar, Tomar) se aceptan solos
    p.on("dialog", lambda d: d.accept())
    yield p
    p.close()


def csrf_de(page: Page) -> dict:
    """Encabezado CSRF como lo manda el front (meta _csrf / _csrf_header)."""
    token = page.evaluate("() => { var m = document.querySelector('meta[name=\"_csrf\"]'); return m ? m.content : ''; }")
    header = page.evaluate("() => { var m = document.querySelector('meta[name=\"_csrf_header\"]'); return m ? m.content : 'X-CSRF-TOKEN'; }")
    return {header: token} if token else {}


def esperar(cond, timeout=60, cada=1.0, que="condición"):
    fin = time.time() + timeout
    while time.time() < fin:
        r = cond()
        if r:
            return r
        time.sleep(cada)
    raise AssertionError(f"Tiempo agotado ({timeout}s) esperando {que}")
