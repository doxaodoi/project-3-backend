package com.reclaim.service;

import com.reclaim.dto.request.ItemRequest;
import com.reclaim.dto.response.ItemResponse;
import com.reclaim.entity.*;
import com.reclaim.exception.ApiException;
import com.reclaim.repository.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ItemService {

    private final ItemRepository itemRepo;
    private final CategoryRepository categoryRepo;
    private final LocationRepository locationRepo;
    private final TagRepository tagRepo;
    private final MatchRepository matchRepo;
    private final MatchService matchService;

    public ItemService(ItemRepository itemRepo, CategoryRepository categoryRepo,
                       LocationRepository locationRepo, TagRepository tagRepo,
                       MatchRepository matchRepo, @Lazy MatchService matchService) {
        this.itemRepo = itemRepo;
        this.categoryRepo = categoryRepo;
        this.locationRepo = locationRepo;
        this.tagRepo = tagRepo;
        this.matchRepo = matchRepo;
        this.matchService = matchService;
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> search(String type, String status, Long categoryId,
                                     Long locationId, String q, Pageable pageable) {
        return itemRepo.search(type, status, categoryId, locationId, q, pageable)
            .map(item -> {
                int mc = matchRepo.findByItemId(item.getId()).size();
                return ItemResponse.from(item, mc);
            });
    }

    @Transactional(readOnly = true)
    public ItemResponse getById(Long id) {
        Item item = itemRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Item"));
        int mc = matchRepo.findByItemId(id).size();
        return ItemResponse.from(item, mc);
    }

    @Transactional
    public ItemResponse create(ItemRequest req, User reporter) {
        Item item = Item.builder()
            .reporter(reporter)
            .type(req.getType().toUpperCase())
            .title(req.getTitle())
            .description(req.getDescription())
            .heldAt(req.getHeldAt())
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .eventDate(req.getEventDate())
            .color(req.getColor())
            .brand(req.getBrand())
            .isAiDescribed(req.getIsAiDescribed() != null && req.getIsAiDescribed())
            .build();

        if (req.getCategoryId() != null) {
            item.setCategory(categoryRepo.findById(req.getCategoryId()).orElse(null));
        }
        if (req.getLocationId() != null) {
            item.setLocation(locationRepo.findById(req.getLocationId()).orElse(null));
        }

        // Tags
        if (req.getTags() != null) {
            Set<Tag> tags = new HashSet<>();
            for (String name : req.getTags()) {
                Tag tag = tagRepo.findByName(name.trim().toLowerCase())
                    .orElseGet(() -> tagRepo.save(Tag.builder().name(name.trim().toLowerCase()).build()));
                tags.add(tag);
            }
            item.setTags(tags);
        }

        // Photos
        if (req.getPhotoUrls() != null) {
            for (int i = 0; i < req.getPhotoUrls().size(); i++) {
                ItemPhoto photo = ItemPhoto.builder()
                    .item(item)
                    .url(req.getPhotoUrls().get(i))
                    .isPrimary(i == 0)
                    .build();
                item.getPhotos().add(photo);
            }
        }

        item = itemRepo.save(item);

        // Trigger matching asynchronously
        matchService.computeMatches(item);

        return ItemResponse.from(item);
    }

    @Transactional
    public ItemResponse update(Long id, ItemRequest req, User currentUser) {
        Item item = itemRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Item"));

        if (!item.getReporter().getId().equals(currentUser.getId())
                && !"ADMIN".equals(currentUser.getRole())) {
            throw ApiException.forbidden("You can only edit your own reports");
        }

        item.setTitle(req.getTitle());
        item.setDescription(req.getDescription());
        item.setHeldAt(req.getHeldAt());
        item.setLatitude(req.getLatitude());
        item.setLongitude(req.getLongitude());
        item.setEventDate(req.getEventDate());
        item.setColor(req.getColor());
        item.setBrand(req.getBrand());

        if (req.getCategoryId() != null) {
            item.setCategory(categoryRepo.findById(req.getCategoryId()).orElse(null));
        }
        if (req.getLocationId() != null) {
            item.setLocation(locationRepo.findById(req.getLocationId()).orElse(null));
        }

        return ItemResponse.from(itemRepo.save(item));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Item item = itemRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Item"));

        if (!item.getReporter().getId().equals(currentUser.getId())
                && !"ADMIN".equals(currentUser.getRole())) {
            throw ApiException.forbidden("You can only delete your own reports");
        }
        itemRepo.delete(item);
    }

    @Transactional
    public ItemResponse updateStatus(Long id, String status, User currentUser) {
        Item item = itemRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Item"));

        if (!item.getReporter().getId().equals(currentUser.getId())
                && !"ADMIN".equals(currentUser.getRole())) {
            throw ApiException.forbidden("Not authorized");
        }

        item.setStatus(status.toUpperCase());
        return ItemResponse.from(itemRepo.save(item));
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getMyItems(User user) {
        return itemRepo.findByReporterId(user.getId()).stream()
            .map(item -> {
                int mc = matchRepo.findByItemId(item.getId()).size();
                return ItemResponse.from(item, mc);
            })
            .toList();
    }
}
