package com.onest.app.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class PortalViewController {

    /**
     * Modulos del dashboard analitico (rol MEDICO_ANALISTA) - clave de ruta -> titulo.
     * Estructura tomada del prototipo original (cambios/Dashboard Salud/public/index.html,
     * const NAV), sin "Administracion" (ya existe en /admin/**, no se duplica aqui).
     * Fase 1 (esqueleto navegable, ver fragments/menu.html): cada modulo es su propia URL
     * bookmarkeable, todos comparten el mismo template placeholder hasta que se construya
     * el contenido real de cada uno (siguiente fase, empezando por Ejecutivo+Morbilidad).
     */
    private static final Map<String, String> MODULOS_ANALISIS = new LinkedHashMap<>();
    static {
        MODULOS_ANALISIS.put("ejecutivo", "Dashboard Ejecutivo");
        MODULOS_ANALISIS.put("predio", "Vista por Predio");
        MODULOS_ANALISIS.put("atenciones", "Atenciones");
        MODULOS_ANALISIS.put("causas", "Causas");
        MODULOS_ANALISIS.put("musculoesqueleticas", "Musculoesqueléticas");
        MODULOS_ANALISIS.put("examenes", "Exámenes Médicos");
        MODULOS_ANALISIS.put("incapacidades", "Incapacidades");
        MODULOS_ANALISIS.put("accidentabilidad", "Accidentabilidad");
        MODULOS_ANALISIS.put("antidoping", "Antidoping");
        MODULOS_ANALISIS.put("inventario", "Inventario");
        MODULOS_ANALISIS.put("maternidad", "Maternidad");
        MODULOS_ANALISIS.put("empleados", "Empleados");
        MODULOS_ANALISIS.put("importar", "Importar Excel");
        MODULOS_ANALISIS.put("reportes", "Reportes");
        MODULOS_ANALISIS.put("auditoria", "Auditoría");
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    /** Home = el dashboard de KPIs (ApexCharts), decision del 18 de agosto. La ruta /dashboard-demo-apex
     * se conserva aparte (mismo template) para no perderla como URL independiente. */
    @GetMapping("/home")
    public String home() {
        return "pages/dashboard-demo-apex";
    }

    /** Demo del Dashboard ligero de KPIs (Resumen General) usando Recharts (igual que salud-ocupacional-v2). */
    @GetMapping("/dashboard-demo")
    public String dashboardDemo() {
        return "pages/dashboard-demo";
    }

    /** Misma demo, usando ApexCharts (ya bundleado en el theme) en vez de Recharts, para comparar. */
    @GetMapping("/dashboard-demo-apex")
    public String dashboardDemoApex() {
        return "pages/dashboard-demo-apex";
    }

    /**
     * Pagina dedicada de busqueda por NSS. Acepta un NSS opcional por query param
     * (usado como shortcut desde el buscador global del header) para pre-cargar y buscar.
     */
    @GetMapping("/nss")
    public String nssSearch(@RequestParam(name = "nss", required = false) String nss, Model model) {
        model.addAttribute("nssQuery", nss == null ? "" : nss.trim());
        return "pages/nss-search";
    }

    /**
     * Consumibles de antidoping (inventario por PREDIO/mes). NO forma parte del flujo de
     * busqueda por NSS - pagina standalone, accesible por URL directa (sin id_menu registrado
     * todavia, mismo patron que Antidoping/Accidentes al lanzarse).
     */
    @GetMapping("/consumibles")
    public String consumibles() {
        return "pages/consumibles";
    }

    /**
     * Antidoping - seleccion aleatoria de personal (docs/entregable-liberacion-stoppers-salud.html
     * paragrafo 2). Standalone, 100% cliente - no existe ningun WS que liste "todo el personal
     * activo" (confirmado contra docs/contextoWS.txt), asi que el pool se pega manualmente cada
     * vez (decision confirmada con el usuario 2026-08-21), no se inventa una fuente de datos.
     */
    @GetMapping("/antidoping-seleccion")
    public String antidopingSeleccion() {
        return "pages/antidoping-seleccion";
    }

    /**
     * Catálogo administrable de causas de consulta - pagina standalone, accesible por URL
     * directa (sin id_menu registrado todavia, mismo patron que Consumibles/Antidoping al
     * lanzarse). docs/ords-causa-consulta.sql.
     */
    @GetMapping("/causas-consulta")
    public String causasConsulta() {
        return "pages/causas-consulta";
    }

    /**
     * /admin/** protegido con hasRole("ADMIN") en SecurityConfiguration. Roles locales
     * (APP_SEC_ROLE) - primera de las 3 pantallas admin del plan RBAC local
     * (docs/plan-rbac-local.md): roles, roles/{id}/menus, usuarios.
     */
    @GetMapping("/admin/roles")
    public String adminRoles() {
        return "pages/admin-roles";
    }

    /** Segunda pantalla admin del plan RBAC local: checklist de menus por rol. */
    @GetMapping("/admin/roles/{id}/menus")
    public String adminRoleMenus(@PathVariable Long id, Model model) {
        model.addAttribute("roleId", id);
        return "pages/admin-role-menus";
    }

    /** Tercera y ultima pantalla admin del plan RBAC local: rol(es) por NSS. */
    @GetMapping("/admin/usuarios")
    public String adminUsuarios() {
        return "pages/admin-usuarios";
    }

    /**
     * Mapeo cuenta-&gt;predio para el dashboard analitico (docs/ords-predio-cuenta.sql).
     * Bajo /admin/** (hasRole("ADMIN")) porque asignar predio es una tarea de configuracion,
     * no del uso diario del rol MEDICO_ANALISTA que consume el resultado en /analisis.
     */
    @GetMapping("/admin/predios")
    public String adminPredios() {
        return "pages/admin-predios";
    }

    /**
     * Dashboard analitico (metricas/graficas por predio - migracion de
     * cambios/Dashboard Salud, ver docs/onest-skin-guide.md y docs/ords-predio-cuenta.sql).
     * Protegido con hasRole("MEDICO_ANALISTA") en SecurityConfiguration - rol nuevo, se crea
     * desde /admin/roles (RoleAdminService.crear ya existente, sin seed SQL necesario).
     * Un modulo = una URL (no tabs, ver fragments/menu.html) - bookmarkeable/compartible.
     */
    @GetMapping("/analisis/{modulo}")
    public String analisis(@PathVariable String modulo, Model model) {
        String titulo = MODULOS_ANALISIS.get(modulo);
        if (titulo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modulo de analisis desconocido: " + modulo);
        }
        // "ejecutivo" ya tiene contenido real (fase 2, 09-sep-2026) - reusa los mismos
        // endpoints /api/dashboard/** que /home, sin WS/controller nuevo. El resto de los
        // 14 modulos sigue en el placeholder generico hasta que se construyan.
        if ("ejecutivo".equals(modulo)) {
            return "pages/analisis-ejecutivo";
        }
        // "predio" (fase 3, 10-sep-2026): ficha por predio. Reusa los mismos 4 endpoints
        // /api/dashboard/** con el parametro predio, que ya aceptan desde que los WS _cta
        // devuelven CUENTA por registro. Sin WS ni controller nuevo.
        if ("predio".equals(modulo)) {
            return "pages/analisis-predio";
        }
        if ("atenciones".equals(modulo)) {
            return "pages/analisis-atenciones";
        }
        if ("causas".equals(modulo)) {
            return "pages/analisis-causas";
        }
        if ("examenes".equals(modulo)) {
            return "pages/analisis-examenes";
        }
        if ("incapacidades".equals(modulo)) {
            return "pages/analisis-incapacidades";
        }
        if ("accidentabilidad".equals(modulo)) {
            return "pages/analisis-accidentabilidad";
        }
        if ("antidoping".equals(modulo)) {
            return "pages/analisis-antidoping";
        }
        if ("inventario".equals(modulo)) {
            return "pages/analisis-inventario";
        }
        if ("maternidad".equals(modulo)) {
            return "pages/analisis-maternidad";
        }
        if ("musculoesqueleticas".equals(modulo)) {
            return "pages/analisis-musculoesqueleticas";
        }
        if ("auditoria".equals(modulo)) {
            return "pages/analisis-auditoria";
        }
        if ("importar".equals(modulo)) {
            return "pages/analisis-importar";
        }
        if ("empleados".equals(modulo)) {
            return "pages/analisis-empleados";
        }
        if ("reportes".equals(modulo)) {
            return "pages/analisis-reportes";
        }
        model.addAttribute("moduloTitulo", titulo);
        return "pages/analisis-modulo";
    }
}
