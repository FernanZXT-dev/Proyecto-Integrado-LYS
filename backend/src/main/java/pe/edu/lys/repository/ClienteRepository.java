package pe.edu.lys.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.lys.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
  Optional<Cliente> findByCorreo(String correo);

  boolean existsByCorreo(String correo);
}
