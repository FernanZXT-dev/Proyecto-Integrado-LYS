package pe.edu.lys.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.lys.entity.Pedido;
public interface PedidoRepository extends JpaRepository<Pedido, Long> {}
