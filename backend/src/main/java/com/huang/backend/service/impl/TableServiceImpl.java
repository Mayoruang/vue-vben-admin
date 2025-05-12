package com.huang.backend.service.impl;

import com.huang.backend.payload.response.PageResponse;
import com.huang.backend.service.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TableServiceImpl implements TableService {

    private static final List<Map<String, Object>> MOCK_DATA = new ArrayList<>();
    private static final String[] STATUSES = {"success", "error", "warning"};
    private static final String[] PRODUCT_ADJECTIVES = {"Premium", "Deluxe", "Luxury", "Basic", "Essential", "Advanced"};
    private static final String[] PRODUCT_NAMES = {"Widget", "Gadget", "Device", "Tool", "Component", "System", "Product"};
    private static final String[] CATEGORIES = {"Electronics", "Office", "Kitchen", "Outdoor", "Clothing", "Health", "Sports"};
    private static final String[] COLORS = {"Red", "Blue", "Green", "Yellow", "Black", "White", "Purple", "Orange"};

    static {
        // 初始化模拟数据
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> item = new HashMap<>();

            item.put("id", UUID.randomUUID().toString());
            item.put("imageUrl", "https://via.placeholder.com/150");
            item.put("imageUrl2", "https://via.placeholder.com/150");
            item.put("open", random.nextBoolean());
            item.put("status", STATUSES[random.nextInt(STATUSES.length)]);
            
            String productAdj = PRODUCT_ADJECTIVES[random.nextInt(PRODUCT_ADJECTIVES.length)];
            String productName = PRODUCT_NAMES[random.nextInt(PRODUCT_NAMES.length)];
            item.put("productName", productAdj + " " + productName);
            
            item.put("price", String.format("%.2f", 10 + 990 * random.nextDouble()));
            item.put("currency", "USD");
            item.put("quantity", random.nextInt(100) + 1);
            item.put("available", random.nextBoolean());
            item.put("category", CATEGORIES[random.nextInt(CATEGORIES.length)]);
            
            // 过去5年内的随机日期
            LocalDateTime releaseDate = LocalDateTime.now().minusDays(random.nextInt(5 * 365));
            item.put("releaseDate", releaseDate);
            
            item.put("rating", 1 + 4 * random.nextDouble());
            item.put("description", "This is a " + productAdj.toLowerCase() + " " + productName.toLowerCase() + " for all your needs.");
            item.put("weight", 0.1 + 9.9 * random.nextDouble());
            item.put("color", COLORS[random.nextInt(COLORS.length)]);
            item.put("inProduction", random.nextBoolean());
            
            List<String> tags = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                tags.add(PRODUCT_ADJECTIVES[random.nextInt(PRODUCT_ADJECTIVES.length)]);
            }
            item.put("tags", tags);
            
            MOCK_DATA.add(item);
        }
    }

    @Override
    public PageResponse<Map<String, Object>> getTableData(int page, int pageSize, String sortBy, String sortOrder) {
        List<Map<String, Object>> sortedData = new ArrayList<>(MOCK_DATA);
        
        // 对数据进行排序（如果提供了排序字段）
        if (sortBy != null && !sortBy.isEmpty()) {
            sortedData.sort((a, b) -> {
                Object valueA = a.get(sortBy);
                Object valueB = b.get(sortBy);
                
                // 处理特殊类型
                if (valueA instanceof String && valueB instanceof String) {
                    int comparison = ((String) valueA).compareToIgnoreCase((String) valueB);
                    return "asc".equalsIgnoreCase(sortOrder) ? comparison : -comparison;
                } else if (valueA instanceof Number && valueB instanceof Number) {
                    double numA = ((Number) valueA).doubleValue();
                    double numB = ((Number) valueB).doubleValue();
                    int comparison = Double.compare(numA, numB);
                    return "asc".equalsIgnoreCase(sortOrder) ? comparison : -comparison;
                } else if (valueA instanceof Boolean && valueB instanceof Boolean) {
                    int comparison = Boolean.compare((Boolean) valueA, (Boolean) valueB);
                    return "asc".equalsIgnoreCase(sortOrder) ? comparison : -comparison;
                } else if (valueA instanceof LocalDateTime && valueB instanceof LocalDateTime) {
                    int comparison = ((LocalDateTime) valueA).compareTo((LocalDateTime) valueB);
                    return "asc".equalsIgnoreCase(sortOrder) ? comparison : -comparison;
                }
                
                // 默认情况
                return 0;
            });
        }
        
        // 分页处理
        int totalItems = sortedData.size();
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);
        
        List<Map<String, Object>> pagedData;
        if (startIndex < totalItems) {
            pagedData = sortedData.subList(startIndex, endIndex);
        } else {
            pagedData = Collections.emptyList();
        }
        
        return PageResponse.<Map<String, Object>>builder()
                .items(pagedData)
                .total(totalItems)
                .page(page)
                .pageSize(pageSize)
                .build();
    }
}