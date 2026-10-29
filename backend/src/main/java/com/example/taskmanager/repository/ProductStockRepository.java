package com.example.taskmanager.repository;

import com.example.taskmanager.entity.ProductStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {
    List<ProductStock> findAllByWarehouseIdOrderByProduct_Name(Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ps from ProductStock ps where ps.product.id = :productId and ps.warehouse.id = :warehouseId")
    Optional<ProductStock> findByProductIdAndWarehouseIdForUpdate(
        @Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    Optional<ProductStock> findByProductIdAndWarehouseId(Long productId, Long warehouseId);
}
