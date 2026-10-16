package pe.edu.lys.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.lys.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
  List<Categoria> findByActivoTrueOrderByNombreAsc();

  boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
