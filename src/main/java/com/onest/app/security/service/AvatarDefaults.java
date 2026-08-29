package com.onest.app.security.service;

/**
 * Avatar de respaldo cuando no hay foto real (IntranetUserClient sin match por
 * email, o login legacy/local antes de resolver foto). Silueta neutra (sin
 * genero) en vez de la foto de stock del theme (/theme/assets/img/img1.jpg,
 * que es la foto de un hombre) - esa foto no tiene sentido como generico para
 * cualquier usuario, y el theme no trae una segunda foto "generica mujer" para
 * alternar. Mismo SVG que ya se usaba como fallback de ultimo nivel (onerror)
 * en fragments/menu.html.
 */
public final class AvatarDefaults {

    public static final String DEFAULT_AVATAR = "data:image/svg+xml;base64,"
            + "PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj4"
            + "8cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgcng9IjUwIiBmaWxsPSIjQ0JENUUxIi8+PGNpcmNsZSBjeD0iNTAiIGN5PSIzOCIgcj0iMTgiIGZpbGw9IiM5NEEzQjgiLz4"
            + "8cGF0aCBkPSJNNTAgNThjLTIyIDAtMzggMTQtMzggMzJ2MTBoNzZWOTBjMC0xOC0xNi0zMi0zOC0zMnoiIGZpbGw9IiM5NEEzQjgiLz48L3N2Zz4K";

    private AvatarDefaults() {
    }
}
