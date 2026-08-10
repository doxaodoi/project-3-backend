package com.reclaim.repository;

import com.reclaim.entity.ItemPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemPhotoRepository extends JpaRepository<ItemPhoto, Long> {
    List<ItemPhoto> findByItemId(Long itemId);
}
