/* function validateModules() {
  $.ajax({
    type: "POST",
    url: "rest/validateModules.php",
    data: "data",
    dataType: "json",
    success: function (response) {
      for (i = 0; i < response.Datos.length; i++) {
        let apps = response.Datos[i].apps;
        for (i = 0; i < apps.length; i++) {
          let modules = apps[i].modulos;
          for (i = 0; i < modules.length; i++) {
            console.log(modules[i].modulo);
            $("." + modules[i].id_menu + "").show();
          }
        }
      }
    },
  });
}*/
$(document).ready(function () {
  /* traerDatsUser(); */
});
function traerDatsUser() {
  $(".module").hide();
  console.log("Hola");
  $.ajax({
    type: "POST",
    url: "rest/validateModules.php",
    data: "data",
    dataType: "json",
    success: function (response) {
      
      let datos = response.Resultado.Datos;
      for (let i = 0; i < datos.length; i++) {
        console.log(datos[i].id_menu);
        $("." + datos[i].id_menu + "").show();
      }
    },
  });
}
