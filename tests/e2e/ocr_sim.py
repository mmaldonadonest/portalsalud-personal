"""
Simulador de OCR para las pruebas E2E:
  - OcrCliente: lo que OCR manda al portal (POST solicitud, GET consulta, cancelar) con X-Api-Key.
  - Receptor: lo que OCR recibe del portal (webhook del dictamen); valida X-Firma = HMAC-SHA256(cuerpo)
    y guarda cada POST para que la prueba lo revise.
Contrato: docs/integracion-ocr-examen-inicial.md y docs/respuesta-ocr-webhook-dictamen.md.
"""
import hashlib
import hmac
import json
import threading
import time
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

RUTA_API = "/api/integracion/ocr/examen-inicial/solicitudes"


class OcrCliente:
    def __init__(self, base: str, api_key: str):
        self.base = base.rstrip("/")
        self.api_key = api_key

    def _req(self, metodo: str, ruta: str, cuerpo=None, api_key=None):
        datos = None if cuerpo is None else json.dumps(cuerpo, ensure_ascii=False).encode("utf-8")
        req = urllib.request.Request(self.base + ruta, data=datos, method=metodo)
        req.add_header("Accept", "application/json")
        if datos is not None:
            req.add_header("Content-Type", "application/json; charset=UTF-8")
        llave = self.api_key if api_key is None else api_key
        if llave:
            req.add_header("X-Api-Key", llave)
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return r.status, _json(r.read())
        except urllib.error.HTTPError as e:
            return e.code, _json(e.read())

    def crear(self, solicitud: dict, api_key=None):
        return self._req("POST", RUTA_API, solicitud, api_key)

    def por_id(self, sid):
        return self._req("GET", f"{RUTA_API}/{sid}")

    def por_referencia(self, ref):
        return self._req("GET", f"{RUTA_API}?referenciaOcr={ref}")

    def cancelar(self, sid, motivo="prueba E2E"):
        return self._req("POST", f"{RUTA_API}/{sid}/cancelar", {"motivo": motivo})


def _json(b: bytes):
    if not b:
        return None
    try:
        return json.loads(b.decode("utf-8"))
    except ValueError:
        return b.decode("utf-8", "replace")


class Receptor:
    """Servidor HTTP local que hace de OCR recibiendo el webhook."""

    def __init__(self, puerto: int, secreto: str):
        self.puerto = puerto
        self.secreto = secreto.encode("utf-8")
        self.recibidos = []  # dicts: {cuerpo, json, firma_ok, headers, hora}
        self._srv = None
        self._hilo = None

    @property
    def url(self):
        return f"http://127.0.0.1:{self.puerto}/dictamen"

    def iniciar(self):
        receptor = self

        class H(BaseHTTPRequestHandler):
            def log_message(self, *a):  # silencio
                pass

            def do_POST(self):
                n = int(self.headers.get("Content-Length", "0"))
                cuerpo = self.rfile.read(n)
                firma = (self.headers.get("X-Firma") or "").strip().lower()
                esperada = hmac.new(receptor.secreto, cuerpo, hashlib.sha256).hexdigest()
                ok = hmac.compare_digest(firma, esperada)
                try:
                    j = json.loads(cuerpo.decode("utf-8"))
                except ValueError:
                    j = None
                receptor.recibidos.append({
                    "cuerpo": cuerpo.decode("utf-8", "replace"), "json": j, "firma_ok": ok,
                    "headers": {k: v for k, v in self.headers.items()}, "hora": time.time(),
                })
                # Como OCR real: 401 si la firma no cuadra, 200 {"recibido": true} si sí
                if not ok:
                    self.send_response(401)
                    self.send_header("Content-Type", "application/json")
                    self.end_headers()
                    self.wfile.write(b'{"error":"FIRMA_INVALIDA"}')
                    return
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(b'{"recibido":true}')

        self._srv = ThreadingHTTPServer(("127.0.0.1", self.puerto), H)
        self._hilo = threading.Thread(target=self._srv.serve_forever, daemon=True)
        self._hilo.start()
        return self

    def detener(self):
        if self._srv:
            self._srv.shutdown()
            self._srv.server_close()

    def esperar_solicitud(self, solicitud_id, timeout=150):
        """El portal manda el webhook con el scheduler de 60 s: se espera hasta 2.5 min."""
        fin = time.time() + timeout
        while time.time() < fin:
            for r in self.recibidos:
                if r["json"] and r["json"].get("solicitudId") == solicitud_id:
                    return r
            time.sleep(2)
        return None
