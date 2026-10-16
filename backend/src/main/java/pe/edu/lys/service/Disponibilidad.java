package pe.edu.lys.service;

import java.time.Instant;
import pe.edu.lys.entity.*;

final class Disponibilidad {
  private Disponibilidad() {}

  static boolean producto(Producto producto) {
    return producto.isActivo() && producto.getCategoria().isActivo();
  }

  static boolean combo(Combo combo) {
    return combo.isActivo() && !combo.getItems().isEmpty()
        && combo.getItems().stream().allMatch(i -> producto(i.getProducto()));
  }

}
