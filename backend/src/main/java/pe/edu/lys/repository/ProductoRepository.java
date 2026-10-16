package pe.edu.lys.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.lys.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
  List<Producto> findByActivoTrueAndCategoriaActivoTrueOrderByNombreAsc();
}
