package pe.edu.lys.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.lys.dto.ProductoDtos.*;
import pe.edu.lys.entity.Producto;
import pe.edu.lys.exception.ApiException;
import pe.edu.lys.repository.*;

@Service
public class ProductoService {
  private final ProductoRepository productos;
  private final CategoriaRepository categorias;

  public ProductoService(ProductoRepository productos, CategoriaRepository categorias) {
    this.productos = productos;
    this.categorias = categorias;
  }

  @Transactional(readOnly = true)
  public List<ProductoResponse> listar(boolean gestion) {
    return (gestion
            ? productos.findAll()
            : productos.findByActivoTrueAndCategoriaActivoTrueOrderByNombreAsc())
        .stream().map(this::respuesta).toList();
  }

  @Transactional(readOnly = true)
  public ProductoResponse consultar(Long id) {
    Producto p = obtener(id);
    if (!p.isActivo() || !p.getCategoria().isActivo()) throw noEncontrado();
    return respuesta(p);
  }

  @Transactional
  public ProductoResponse crear(ProductoRequest datos) {
    return guardar(new Producto(), datos);
  }

  @Transactional
  public ProductoResponse actualizar(Long id, ProductoRequest datos) {
    return guardar(obtener(id), datos);
  }

  @Transactional
  public void desactivar(Long id) {
    obtener(id).setActivo(false);
  }

  private ProductoResponse guardar(Producto p, ProductoRequest datos) {
    var categoria =
        categorias
            .findById(datos.categoriaId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
    if (!categoria.isActivo())
      throw new ApiException(HttpStatus.CONFLICT, "La categoría está inactiva");
    p.setNombre(datos.nombre().trim());
    p.setDescripcion(datos.descripcion());
    p.setPrecio(datos.precio());
    p.setCategoria(categoria);
    p.setActivo(datos.activo());
    return respuesta(productos.save(p));
  }

  private Producto obtener(Long id) {
    return productos.findById(id).orElseThrow(this::noEncontrado);
  }

  private ApiException noEncontrado() {
    return new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado");
  }

  private ProductoResponse respuesta(Producto p) {
    return new ProductoResponse(
        p.getId(),
        p.getNombre(),
        p.getDescripcion(),
        p.getPrecio(),
        p.getCategoria().getId(),
        p.getCategoria().getNombre(),
        p.isActivo());
  }
}
