package pe.edu.lys.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.lys.entity.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
  List<Pedido> findAllByOrderByCreadoEnDescIdDesc();
  List<Pedido> findByClienteIdOrderByCreadoEnDescIdDesc(Long clienteId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Pedido p where p.id = :id")
  Optional<Pedido> bloquear(@Param("id") Long id);
}
