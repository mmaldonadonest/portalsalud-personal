package com.onest.app.ayuda;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** {@code ayudaVisible} en todas las vistas: header (boton "?"), menu (Ayuda) y layout (panel). */
@ControllerAdvice
public class AyudaViewAdvice {

    private final AyudaProperties props;

    public AyudaViewAdvice(AyudaProperties props) {
        this.props = props;
    }

    @ModelAttribute("ayudaVisible")
    public boolean ayudaVisible() {
        return props.esVisible();
    }
}
