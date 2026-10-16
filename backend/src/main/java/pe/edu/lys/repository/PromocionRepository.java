package pe.edu.lys.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.lys.entity.Promocion;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {
  List<Promocion> findByActivoTrueAndInicioLessThanEqualAndFinGreaterThan(Instant inicio, Instant fin);
}
