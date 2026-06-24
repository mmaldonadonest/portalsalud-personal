function showUser() {
  var nss = $("#nssInput").val();
  $.ajax({
    type: "POST",
    url: "./controller/element/usercard.php",
    data: { data: nss },
    dataType: "html",
    success: function (response) {
      $("#respuesta").html(response);
      $(".welcome").hide();
    },
  });
}
function showModules() {
  var nss = $("#nssInput").val();
  var app = $("#appSelect").val();
  $.ajax({
    type: "POST",
    url: "./controller/element/module.php",
    data: { data: app, nss: nss },
    dataType: "html",
    success: function (response) {
      $("#resModulo").html(response);
      loadSelectRol();
    },
  });
}
function showAltaMenus() {
  $.ajax({
    type: "POST",
    url: "./controller/element/altaMenu.php",
    data: "data",
    dataType: "html",
    success: function (response) {
      $(".welcome").hide();
      $("#respuesta").html(response);
      $(".progress").hide();
    },
  });
}
function saverolUser() {
  var nssUserRol = $("#nssUserRol").val();
  var idAppUserRol = $("#idAppUserRol").val();
  var passInpRols1 = $("#passInpRols1").val();
  var passInpRols2 = $("#passInpRols2").val();
  var SelectRol = $("#SelectRol").val();
  if (passInpRols1 == passInpRols2) {
    ajaxSend();
  } else {
    console.log("");
    Swal.fire({
      title: "¡Error!",
      text: "Contraseñas no coinciden",
      icon: "error",
      confirmButtonText: "Continuar",
    });
  }
  function ajaxSend() {
    $.ajax({
      type: "POST",
      url: "./controller/element/saveRol.php",
      data: {
        nssUserRol: nssUserRol,
        idAppUserRol: idAppUserRol,
        passInpRols1: passInpRols1,
        passInpRols2: passInpRols2,
        SelectRol: SelectRol,
      },
      dataType: "html",
      success: function (response) {
        console.log(response);
        Swal.fire({
          title: "¡Información!",
          text: response,
          icon: "info",
          confirmButtonText: "Continuar",
        });
      },
    });
  }
}
function loadSelectRol() {
  var nssUserRol = $("#nssUserRol").val();
  console.log(nssUserRol+" ");
  var idAppUserRol = $("#idAppUserRol").val();
  console.log(idAppUserRol);
  $.ajax({
    type: "POST",
    url: "./controller/element/roles.php",
    data: { nssUserRol: nssUserRol, idAppUserRol: idAppUserRol },
    dataType: "json",
    success: function (response) {
      console.log(response);
      var idRol = response.Resultado.Datos[0].id_rol;
      $("#SelectRol").empty();
      loadRolesApps(idRol)
    },
  });
}
function loadRolesApps(texto) {
  var idAppUserRol = $("#idAppUserRol").val();
  $.ajax({
    type: "POST",
    url: "./controller/element/buscarApp.php",
    data: { data: idAppUserRol },
    dataType: "json",
    success: function (response) {
      var roles = response.Resultado.Datos[0].Roles;
      roles.forEach(function (rol) {
        if (rol.id_rol == texto) {
          $("#SelectRol").append(
            "<option selected value='" + rol.id_rol + "'>" + rol.nombre_rol + "</option>"
          );
        } else {
          $("#SelectRol").append(
            "<option value='" + rol.id_rol + "'>" + rol.nombre_rol + "</option>"
          );
        }
      });
    },
  });
}
function saveDats() {
  var appInput = $("#appInput").val();
  $(".btn").prop("disabled", true);
  $("#progre1").show();
  $.ajax({
    type: "POST",
    url: "./controller/element/altApp.php",
    data: { data: appInput },
    dataType: "json",
    success: function (response) {
      $("#progre1").hide();
      Swal.fire(response.Resultado.Mensaje);
      showAltaMenus();
    },
  });
}
function searchIdApp() {
  $("#progre2").show();
  var appId = $("#appId").val();
  $.ajax({
    type: "POST",
    url: "./controller/element/buscarApp.php",
    data: { data: appId },
    dataType: "json",
    success: function (response) {
      var rolesTable = $("#rolesTable").DataTable();
      // Destruir la instancia DataTable

      // Limpiar los datos de la tabla
      rolesTable.clear().draw();
      // Destruir la instancia DataTable
      rolesTable.destroy();

      console.log(response);
      $(".progress").hide();
      console.log(response.Resultado.Datos[0].id_app);
      $("#idAppInput").val(response.Resultado.Datos[0].id_app);
      $("#nameAppInput").val(response.Resultado.Datos[0].nombre_app);
      $("#idAppModal").val(response.Resultado.Datos[0].id_app);
      $("#idAppModalRol").val(response.Resultado.Datos[0].id_app);

      var roles = response.Resultado.Datos[0].Roles;
      var idApp = response.Resultado.Datos[0].id_app;
      var button = `
      <div class="d-grid gap-2">
                <button type="button" onclick="borrarApp(${response.Resultado.Datos[0].id_app})" name="btnBorra${response.Resultado.Datos[0].id_app}" id="btnBorra${response.Resultado.Datos[0].id_app}" class="btn btn-danger">
                    Borrar
                </button>
            </div>
      `;
      $("#btnBorrar").html(button);
      var buttonModal = `<button type="button" data-bs-toggle="modal" data-bs-target="#modalId" class="btn btn-primary mb-4 mt-3">
      Agregar menu
  </button>`;
      var buttonModalRol = `<button type="button" data-bs-toggle="modal" data-bs-target="#modalROL" class="btn btn-outline-info mb-4 mt-3">
      Agregar rol
  </button>`;

      $("#btnModalRol").html(buttonModalRol);
      // Obtener la tabla y el cuerpo de la tabla
      var rolesTable = $("#rolesTable").DataTable({
        language: {
          url: "http://salud.onestcloud.mx:70/scontent/Spanish.json",
        },
      });
      var rolesTableBody = $("#rolesTable tbody");

      // Iterar sobre los roles y agregar filas a la tabla
      var idRolMasAlto = 0;
      roles.forEach(function (rol) {
        var row =
          "<tr>" +
          "<td>" +
          rol.id_rol +
          "</td>" +
          '<td contenteditable="true" id="nombrerolTable' +
          rol.id_rol +
          '" onkeydown="cambioRol(' +
          idApp +
          ", " +
          rol.id_rol +
          ',this,event)">' +
          rol.nombre_rol +
          "</td>" +
          "<td><div class='d-grid gap-2'>" +
          '<button class="btn btn-outline-primary" onclick="traerMenus(' +
          idApp +
          "," +
          rol.id_rol +
          ')" data-bs-toggle="modal" data-bs-target="#modalModules">Ver modulos</button>' +
          '<button class="btn btn-outline-danger" onclick="bajaRol(' +
          idApp +
          "," +
          rol.id_rol +
          ')">Borrar</button>' +
          "</div></td>" +
          "</tr>";
        rolesTable.row.add($(row)).draw();

        if (rol.id_rol > idRolMasAlto) {
          idRolMasAlto = rol.id_rol;
        }
      });
      $("#IdROLM").val(idRolMasAlto + 1);

      // Agregar eventos a los botones
    },
  });
}
function borrarApp(params) {
  Swal.fire({
    title: "¿Realmente quieres hacer esta acción?",
    showDenyButton: true,
    showCancelButton: true,
    confirmButtonText: "Borrar",
    denyButtonText: `No borrar`,
  }).then((result) => {
    /* Read more about isConfirmed, isDenied below */
    if (result.isConfirmed) {
      bajaApp();
    } else if (result.isDenied) {
      Swal.fire("Cambios cancelados", "", "info");
    }
  });
}
function guardarMenu() {
  var idApp = $("#idAppModal").val();
  var idMenu = $("#IdMenuM").val();
  var nombreMenu = $("#idNombreM").val();
  var idRol = $("#idRolM").val();
  $.ajax({
    type: "POST",
    url: "./controller/element/altaMenuController.php",
    data: {
      idApp: idApp,
      idMenu: idMenu,
      nombreMenu: nombreMenu,
      idRol: idRol,
    },
    dataType: "json",
    success: function (response) {
      Swal.fire(response.Resultado.Mensaje);
    },
  });
}
function guardarROL() {
  var idAppModalRol = $("#idAppModalRol").val();
  var IdROLM = $("#IdROLM").val();
  var idNombreMROL = $("#idNombreMROL").val();
  $.ajax({
    type: "POST",
    url: "./controller/element/altaRol.php",
    data: {
      idAppModalRol: idAppModalRol,
      IdROLM: IdROLM,
      idNombreMROL: idNombreMROL,
    },
    dataType: "json",
    success: function (response) {
      searchIdApp();
      Swal.fire(response.Resultado.Mensaje);
    },
  });
}
function bajaApp() {
  $(".btn").prop("disabled", true);
  var idApp = $("#idAppModalRol").val();
  $.ajax({
    type: "POST",
    url: "./controller/element/bajaApp.php",
    data: {
      idApp: idApp,
    },
    dataType: "json",
    success: function (response) {
      Swal.fire(response.Resultado.Mensaje);
      setTimeout(showAltaMenus, 2000);
    },
  });
}
function bajaRol(idApp, idRol) {
  Swal.fire({
    title: "¿Realmente quieres hacer esta acción?",
    showDenyButton: true,
    showCancelButton: true,
    confirmButtonText: "Borrar",
    denyButtonText: `No borrar`,
  }).then((result) => {
    /* Read more about isConfirmed, isDenied below */
    if (result.isConfirmed) {
      ajaxBajaRol(idApp, idRol);
    } else if (result.isDenied) {
      Swal.fire("Cambios cancelados", "", "info");
    }
  });
  function ajaxBajaRol(idApp, idRol) {
    $.ajax({
      type: "POST",
      url: "./controller/element/bajaRol.php",
      data: { idApp: idApp, idRol: idRol },
      dataType: "json",
      success: function (response) {
        searchIdApp();
        Swal.fire(response.Resultado.Mensaje);
      },
    });
  }
}
function cambioApp() {
  var name = $("#nameAppInput").val();
  var idApp = $("#idAppInput").val();
  if (idApp != null) {
    Swal.fire({
      title: "¿Realmente quieres hacer esta acción?",
      showDenyButton: true,
      showCancelButton: true,
      confirmButtonText: "Cambiar nombre",
      denyButtonText: `No cambiar`,
    }).then((result) => {
      /* Read more about isConfirmed, isDenied below */
      if (result.isConfirmed) {
        cambioAppAjax(name, idApp);
      } else if (result.isDenied) {
        Swal.fire("Cambios cancelados", "", "info");
      }
    });
  } else {
    Swal.fire("El campo nombre está vacio", "", "info");
  }

  function cambioAppAjax(name, idApp) {
    $.ajax({
      type: "POST",
      url: "./controller/element/cambioApp.php",
      data: { name: name, idApp, idApp },
      dataType: "json",
      success: function (response) {
        searchIdApp();
        Swal.fire(response.Resultado.Mensaje);
      },
    });
  }
}
function traerMenus(idApp, idRol) {
  $("#idrolModulo").val(idRol);
  $.ajax({
    type: "POST",
    url: "./controller/element/traerMenus.php",
    data: { idApp: idApp, idRol: idRol },
    dataType: "json",
    success: function (response) {
      var rolesTable = $("#tablaRol").DataTable();
      // Destruir la instancia DataTable

      // Limpiar los datos de la tabla
      rolesTable.clear().draw();
      // Destruir la instancia DataTable
      rolesTable.destroy();
      console.log(response);
      if (response) {
        // Variable para almacenar el id_menu más alto
        var idMenuMasAlto = 0;

        // Recorrer el array y comparar los id_menu
        $.each(response.Datos, function (index, item) {
          if (item.id_menu > idMenuMasAlto) {
            idMenuMasAlto = item.id_menu;
          }
        });
        $("#idModuloInput").val(idMenuMasAlto + 1);
        var tablaBody = $("#tabla-body");
        $.each(response.Datos, function (index, item) {
          var fila = $("<tr>").appendTo(tablaBody);
          $("<td>").text(item.id_menu).appendTo(fila);
          $(
            "<td contenteditable='true' onkeydown='cambioMenu(" +
              item.id_menu +
              ",this,event)'>"
          )
            .text(item.nombre_menu)
            .appendTo(fila);

          $("<td>")
            .html(
              '<button class="btn btn-danger" onclick="bajaMenu(' +
                item.id_menu +
                ')">Borrar</button>'
            )
            .appendTo(fila);
        });
        $("#tablaRol").DataTable({
          language: {
            url: "//cdn.datatables.net/plug-ins/1.13.7/i18n/es-ES.json",
          },
        });
      }
    },
  });
}
function guardarMenuDos() {
  var idApp = $("#idAppInput").val();
  var idMenu = $("#idModuloInput").val();
  var nombreMenu = $("#nombreModuloIn").val();
  var idrol = $("#idrolModulo").val();
  Swal.fire({
    title: "¿Realmente quieres hacer esta acción?",
    showDenyButton: true,
    showCancelButton: true,
    confirmButtonText: "Guardar",
    denyButtonText: `No guardar`,
  }).then((result) => {
    /* Read more about isConfirmed, isDenied below */
    if (result.isConfirmed) {
      altaRolAjax2(idApp, idMenu, nombreMenu, idrol);
    } else if (result.isDenied) {
      Swal.fire("Cambios cancelados", "", "info");
    }
  });
  function altaRolAjax2(idApp, idMenu, nombreMenu, idRol) {
    $.ajax({
      type: "POST",
      url: "./controller/element/altaMenuController.php",
      data: {
        idApp: idApp,
        idMenu: idMenu,
        nombreMenu: nombreMenu,
        idRol: idRol,
      },
      dataType: "html",
      success: function (response) {
        console.log(response);
        traerMenus(idApp, idRol);
      },
    });
  }
}
function cambioRol(idApp, idRol, name, event) {
  if (event.keyCode === 13) {
    var celdas = document.querySelectorAll("#rolesTable td");

    // Iterar sobre las celdas y quitar el atributo contenteditable
    celdas.forEach(function (celda) {
      celda.removeAttribute("contenteditable");
    });
    ajaxCambioRol(idApp, idRol, name);
    function ajaxCambioRol(idApp, idRol, name) {
      const nameRol = name.innerText;
      $.ajax({
        type: "POST",
        url: "./controller/element/cabioRol.php",
        data: { idApp: idApp, idRol: idRol, nameRol: nameRol },
        dataType: "json",
        success: function (response) {
          searchIdApp();
          Swal.fire(response.Resultado.Mensaje);
        },
      });
    }
  }
}
function cambioMenu(idMenu, nombreMenu, event) {
  const nombreMenus = nombreMenu.innerText;
  var idApp = $("#idAppInput").val();
  var idrol = $("#idrolModulo").val();
  if (event.keyCode === 13) {
    var celdas = document.querySelectorAll("#tablaRol td");

    // Iterar sobre las celdas y quitar el atributo contenteditable
    celdas.forEach(function (celda) {
      celda.removeAttribute("contenteditable");
    });

    $.ajax({
      type: "POST",
      url: "./controller/element/cambioMenu.php",
      data: {
        idApp: idApp,
        idMenu: idMenu,
        nombreMenu: nombreMenus,
        idRol: idrol,
      },
      dataType: "json",
      success: function (response) {
        Swal.fire(response.Resultado.Mensaje);
        traerMenus(idApp, idrol);
      },
    });
  }
}
function bajaMenu(idMenu) {
  var idApp = $("#idAppInput").val();
  var idrol = $("#idrolModulo").val();

  Swal.fire({
    title: "¿Realmente quieres hacer esta acción?",
    showDenyButton: true,
    showCancelButton: true,
    confirmButtonText: "Borrar",
    denyButtonText: `No borrar`,
  }).then((result) => {
    /* Read more about isConfirmed, isDenied below */
    if (result.isConfirmed) {
      ajaxBaja(idApp, idMenu, idrol);
    } else if (result.isDenied) {
      Swal.fire("Cambios cancelados", "", "info");
    }
  });

  function ajaxBaja(idApp, idMenu, idrol) {
    $.ajax({
      type: "POST",
      url: "./controller/element/bajaMenu.php",
      data: {
        idApp: idApp,
        idMenu: idMenu,
        idRol: idrol,
      },
      dataType: "json",
      success: function (response) {
        Swal.fire(response.Resultado.Mensaje);
        traerMenus(idApp, idrol);
      },
    });
  }
}
