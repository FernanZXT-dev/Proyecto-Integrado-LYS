package pe.edu.lys.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.lys.dto.CategoriaDtos.*;
import pe.edu.lys.entity.Categoria;
import pe.edu.lys.exception.ApiException;
import pe.edu.lys.repository.CategoriaRepository;

@Service
public class CategoriaService {
  private final CategoriaRepository repository;

  public CategoriaService(CategoriaRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<CategoriaResponse> listar(boolean gestion) {
    return (gestion ? repository.findAll() : repository.findByActivoTrueOrderByNombreAsc())
        .stream().map(this::respuesta).toList();
  }

  @Transactional(readOnly = true)
  public CategoriaResponse consultar(Long id) {
    Categoria c = obtener(id);
    if (!c.isActivo()) throw noEncontrada();
    return respuesta(c);
  }

  @Transactional
  public CategoriaResponse crear(CategoriaRequest datos) {
    return guardar(new Categoria(), datos);
  }

  @Transactional
  public CategoriaResponse actualizar(Long id, CategoriaRequest datos) {
    return guardar(obtener(id), datos);
  }

  @Transactional
  public void desactivar(Long id) {
    obtener(id).setActivo(false);
  }

  private CategoriaResponse guardar(Categoria c, CategoriaRequest datos) {
    String nombre = datos.nombre().trim();
    if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, c.getId() == null ? 0L : c.getId()))
      throw new ApiException(HttpStatus.CONFLICT, "La categoría ya existe");
    c.setNombre(nombre);
    c.setActivo(datos.activo());
    return respuesta(repository.save(c));
  }

  private Categoria obtener(Long id) {
    return repository.findById(id).orElseThrow(this::noEncontrada);
  }

  private ApiException noEncontrada() {
    return new ApiException(HttpStatus.NOT_FOUND, "Categoría no encontrada");
  }

  private CategoriaResponse respuesta(Categoria c) {
    return new CategoriaResponse(c.getId(), c.getNombre(), c.isActivo());
  }
}
