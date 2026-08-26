// "Onest" es un 3er modo dentro del mismo grupo "Skin Mode" (Light/Dark/Onest),
// mutuamente excluyente - no existe una variante oscura del skin ONEST todavia
// (ver static/css/onest-skin.css), asi que elegir "Onest" sale de Dark si estaba
// activo. Este archivo corre DESPUES de theme/assets/js/script.js (mismo orden en
// scripts.html): su logica de Light/Dark original sigue intacta (no se toca ese
// archivo, es del theme comprado), esto solo se encarga del atributo data-brand
// y de dejar el resaltado "active" correcto entre los 3 botones al cargar la
// pagina - el :last-child que usa script.js para restaurar "Dark" asumia solo 2
// botones y quedaria mal con el 3ro, por eso se re-resuelve aqui con data-mode.

// Cargar preferencia guardada
var brandSkin = localStorage.getItem('brand-skin');
var skinMode = localStorage.getItem('skin-mode');

if (brandSkin === 'onest') {
  $('html').attr('data-brand', 'onest');
  // data-skin="dark" puede haber quedado de una sesion anterior con Dark activo
  // (script.js del theme no lo limpia al elegir un boton que no es "dark" via
  // texto, y este 3er boton no coincide con esa logica) - varias reglas del
  // theme como "[data-skin=dark] .sidebar ... .nav-label" tienen mas
  // especificidad que las de este archivo y ganarian, dejando texto casi
  // invisible (rgba(255,255,255,.25)) en vez del blanco que pide el skin.
  $('html').attr('data-skin', '');
  $('#skinMode .nav-link').removeClass('active');
  $('#skinMode .nav-link[data-mode="onest"]').addClass('active');
} else {
  $('#skinMode .nav-link').removeClass('active');
  $('#skinMode .nav-link[data-mode="' + (skinMode === 'dark' ? 'dark' : 'light') + '"]').addClass('active');
}

// Fijar preferencia (se suma al click handler que ya trae script.js para light/dark)
$('#skinMode .nav-link').on('click', function (e) {
  e.preventDefault();

  var mode = $(this).attr('data-mode');
  if (mode === 'onest') {
    $('html').attr('data-brand', 'onest');
    localStorage.setItem('brand-skin', 'onest');
    // Limpiar cualquier data-skin="dark" que haya quedado activo (ver comentario
    // arriba en la carga de preferencia) - Onest no tiene variante oscura propia.
    $('html').attr('data-skin', '');
    localStorage.removeItem('skin-mode');
  } else {
    $('html').attr('data-brand', '');
    localStorage.removeItem('brand-skin');
  }
});
