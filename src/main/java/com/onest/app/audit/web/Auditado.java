package com.onest.app.audit.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un handler que ESCRIBE (alta, cambio, borrado, exportacion) para que
 * {@link AuditoriaInterceptor} lo registre en APP_AUD_EVENT al terminar con exito.
 *
 * <p>Se pone solo en escrituras reales: muchos POST del portal son lecturas
 * (form-urlencoded fiel al PHP) y no deben ensuciar la bitacora. Sin AOP a proposito:
 * spring-boot-starter-aop esta excluido en el pom, y un HandlerInterceptor basta.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditado {

    /** Modulo funcional: Consultas, Incapacidades, Accidentes, Examenes, Antidoping, Maternidad, Administracion... */
    String modulo();

    /** create | update | delete | export | login */
    String accion();

    /** Tipo de registro (Consulta, Incapacidad, Rol, Archivo...). */
    String entidad();

    /** Nombre del request param o path variable que identifica el registro (nss, id, roleId...). */
    String registro() default "";

    /** Request params / path variables que se copian al detalle, en ese orden. */
    String[] detalle() default {};
}
